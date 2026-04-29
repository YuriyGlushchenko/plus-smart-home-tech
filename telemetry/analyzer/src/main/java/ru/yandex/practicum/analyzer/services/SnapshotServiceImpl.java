package ru.yandex.practicum.analyzer.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.analyzer.grpc.HubRouterClient;
import ru.yandex.practicum.analyzer.models.*;
import ru.yandex.practicum.analyzer.repositories.ScenarioActionRepository;
import ru.yandex.practicum.analyzer.repositories.ScenarioConditionRepository;
import ru.yandex.practicum.analyzer.repositories.ScenarioRepository;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequest;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SnapshotServiceImpl implements SnapshotService {

    private final ScenarioRepository scenarioRepository;
    private final HubRouterClient hubRouterClient;
    private final ScenarioConditionRepository scenarioConditionRepository;
    private final ScenarioActionRepository scenarioActionRepository;

    private final Map<String, List<Scenario>> hubScenariosCache = new ConcurrentHashMap<>();

    @Override
    @Transactional(readOnly = true)
    public void handleSnapshot(SensorsSnapshotAvro snapshot) {
        String hubId = snapshot.getHubId();
        log.debug("Обработка снапшота для хаба: {}, время: {}", hubId, snapshot.getTimestamp());

        if (snapshot.getSensorsState() == null || snapshot.getSensorsState().isEmpty()) {
            log.debug("Нет данных о датчиках в снапшоте для хаба {}", hubId);
            return;
        }

        // Загружаем сценарии для хаба (из базы или из кэша)
        List<Scenario> scenarios = getScenariosForHub(hubId);

        if (scenarios.isEmpty()) {
            log.debug("Нет сценариев для хаба {}", hubId);
            return;
        }

        // Извлекаем значения датчиков из снапшота
        Map<String, Map<String, Object>> sensorValues = extractSensorValues(snapshot);

        // Проверяем условия и собираем действия в список
        List<DeviceActionRequest> actionsToExecute = new ArrayList<>();
        for (Scenario scenario : scenarios) {
            if (checkScenarioConditions(scenario, sensorValues)) {
                log.debug("Сценарий '{}' активирован для хаба {}", scenario.getName(), hubId);
                for (ScenarioAction action : scenario.getScenarioActions()) {
                    actionsToExecute.add(hubRouterClient.createActionRequest(hubId, scenario.getName(), action));
                }
            }
        }

        // Выполняем действия
        for (DeviceActionRequest action : actionsToExecute) {
            hubRouterClient.executeAction(action);
        }
    }

    private List<Scenario> getScenariosForHub(String hubId) {
        if (hubScenariosCache.containsKey(hubId)) {
            return hubScenariosCache.get(hubId);
        }

        List<Scenario> scenarios = scenarioRepository.findByHubId(hubId);
        log.debug("Загружено сценариев: {}", scenarios.size());

        if (scenarios.isEmpty()) {
            hubScenariosCache.put(hubId, scenarios);
            return scenarios;
        }

        List<Long> scenarioIds = scenarios.stream()
                .map(Scenario::getId)
                .collect(Collectors.toList());
        log.debug("ID сценариев: {}", scenarioIds);

        // Загружаем все условия одним запросом
        List<ScenarioCondition> allConditions = scenarioConditionRepository.findByScenarioIdsWithFetch(scenarioIds);
        log.debug("Загружено условий: {}", allConditions.size());

        // Загружаем все действия одним запросом
        List<ScenarioAction> allActions = scenarioActionRepository.findByScenarioIdsWithFetch(scenarioIds);
        log.debug("Загружено действий: {}", allActions.size());

        // Группируем по scenarioId
        Map<Long, List<ScenarioCondition>> conditionsByScenario = allConditions.stream()
                .collect(Collectors.groupingBy(sc -> sc.getScenario().getId()));

        Map<Long, List<ScenarioAction>> actionsByScenario = allActions.stream()
                .collect(Collectors.groupingBy(sa -> sa.getScenario().getId()));

        // Собираем всё вместе внутри объекта Scenario
        for (Scenario scenario : scenarios) {
            scenario.getScenarioConditions().clear();
            scenario.getScenarioConditions().addAll(
                    conditionsByScenario.getOrDefault(scenario.getId(), Collections.emptyList())
            );

            scenario.getScenarioActions().clear();
            scenario.getScenarioActions().addAll(
                    actionsByScenario.getOrDefault(scenario.getId(), Collections.emptyList())
            );
        }

        // сохраняем в кэш
        hubScenariosCache.put(hubId, scenarios);
        return scenarios;
    }

    private Map<String, Map<String, Object>> extractSensorValues(SensorsSnapshotAvro snapshot) {

        // вложенный Map <id сенсора, Map <имя параметра, значение параметра>>
        Map<String, Map<String, Object>> values = new HashMap<>();

        Map<String, SensorStateAvro> sensorsState = snapshot.getSensorsState(); // ключ - id сенсора
        if (sensorsState == null) {
            return values;
        }

        for (Map.Entry<String, SensorStateAvro> entry : sensorsState.entrySet()) {
            String sensorId = entry.getKey();
            SensorStateAvro sensorState = entry.getValue();

            if (sensorState == null || sensorState.getData() == null) {
                continue;
            }

            Object data = sensorState.getData();
            Map<String, Object> sensorData = new HashMap<>();

            switch (data) {
                case MotionSensorAvro motionSensor ->
                        sensorData.put(SensorField.MOTION.getName(), motionSensor.getMotion() ? 1 : 0);

                case TemperatureSensorAvro temperatureSensor -> {
                    sensorData.put(SensorField.TEMPERATURE_C.getName(), temperatureSensor.getTemperatureC());
                    sensorData.put(SensorField.TEMPERATURE_F.getName(), temperatureSensor.getTemperatureF());
                    sensorData.put(SensorField.TEMPERATURE.getName(), temperatureSensor.getTemperatureC());
                }

                case LightSensorAvro lightSensor ->
                        sensorData.put(SensorField.LUMINOSITY.getName(), lightSensor.getLuminosity());

                case ClimateSensorAvro climateSensor -> {
                    sensorData.put(SensorField.TEMPERATURE_C.getName(), climateSensor.getTemperatureC());
                    sensorData.put(SensorField.TEMPERATURE.getName(), climateSensor.getTemperatureC());
                    sensorData.put(SensorField.HUMIDITY.getName(), climateSensor.getHumidity());
                    sensorData.put(SensorField.CO2.getName(), climateSensor.getCo2Level());
                }

                case SwitchSensorAvro switchSensor ->
                        sensorData.put(SensorField.STATE.getName(), switchSensor.getState() ? 1 : 0);

                case null, default -> log.warn("Неизвестный или null тип датчика: {}",
                        data == null ? "null" : data.getClass().getSimpleName());
            }

            values.put(sensorId, Map.copyOf(sensorData));
        }

        return values;
    }

    private boolean checkScenarioConditions(Scenario scenario, Map<String, Map<String, Object>> sensorValues) {
        for (ScenarioCondition scenarioCondition : scenario.getScenarioConditions()) {
            Condition condition = scenarioCondition.getCondition();
            String sensorId = scenarioCondition.getSensor().getId();

            if (!checkCondition(condition, sensorValues, sensorId, scenario.getName())) {
                return false;
            }
        }
        return true;
    }

    private boolean checkCondition(Condition condition,
                                   Map<String, Map<String, Object>> sensorValues,
                                   String sensorId,
                                   String scenarioName) {
        Map<String, Object> sensorData = sensorValues.get(sensorId);
        if (sensorData == null) {
            log.debug("Нет данных для датчика {} в сценарии {}", sensorId, scenarioName);
            return false;
        }

        String fieldName = getFieldName(condition.getType());
        Object sensorValue = sensorData.get(fieldName);

        if (sensorValue == null) {
            log.debug("Нет значения для поля '{}' датчика {} в сценарии {}", fieldName, sensorId, scenarioName);
            return false;
        }

        int sensorIntValue = convertToInt(sensorValue);
        int conditionValue = condition.getValue() != null ? condition.getValue() : 0;

        boolean result = switch (condition.getOperation()) {
            case EQUALS -> sensorIntValue == conditionValue;
            case GREATER_THAN -> sensorIntValue > conditionValue;
            case LOWER_THAN -> sensorIntValue < conditionValue;
        };

        log.debug("Проверка условия: сценарий={}, датчик={}, поле={}, значение={}, операция={}, условие={}, результат={}",
                scenarioName, sensorId, fieldName, sensorIntValue, condition.getOperation(), conditionValue, result);

        return result;
    }

    private String getFieldName(ConditionType type) {
        return switch (type) {
            case MOTION -> SensorField.MOTION.getName();
            case LUMINOSITY -> SensorField.LUMINOSITY.getName();
            case SWITCH -> SensorField.STATE.getName();
            case TEMPERATURE -> SensorField.TEMPERATURE.getName();
            case HUMIDITY -> SensorField.HUMIDITY.getName();
            case CO2LEVEL -> SensorField.CO2.getName();
        };
    }

    private int convertToInt(Object value) {
        return switch (value) {
            case Integer i -> i;
            case Boolean b -> b ? 1 : 0;
            case null -> 0;
            default -> {
                log.warn("Неизвестный тип значения: {}", value.getClass().getSimpleName());
                yield 0;
            }
        };
    }

    public void invalidateCache(String hubId) {
        hubScenariosCache.remove(hubId);
        log.debug("Инвалидирован кэш для хаба {}", hubId);
    }
}
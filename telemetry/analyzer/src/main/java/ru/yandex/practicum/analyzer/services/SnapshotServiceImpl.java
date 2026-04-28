package ru.yandex.practicum.analyzer.services;

import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.analyzer.models.*;
import ru.yandex.practicum.analyzer.repositories.ScenarioRepository;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequest;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class SnapshotServiceImpl implements SnapshotService {

    private final ScenarioRepository scenarioRepository;
    private final HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient;

    private final Map<String, List<Scenario>> hubScenariosCache = new ConcurrentHashMap<>();

    @Override
    @Transactional(readOnly = true)
    public void handleSnapshot(SensorsSnapshotAvro snapshot) {
        String hubId = snapshot.getHubId();
        log.debug("Обработка снапшота для хаба: {}, время: {}", hubId, snapshot.getTimestamp());

        // Проверяем, есть ли данные о датчиках
        if (snapshot.getSensorsState() == null || snapshot.getSensorsState().isEmpty()) {
            log.debug("Нет данных о датчиках в снапшоте для хаба {}", hubId);
            return;
        }

        // Получаем сценарии для хаба
        List<Scenario> scenarios = getScenariosForHub(hubId);

        if (scenarios.isEmpty()) {
            log.debug("Нет сценариев для хаба {}", hubId);
            return;
        }

        // Извлекаем актуальные значения датчиков из снапшота
        Map<String, Object> sensorValues = extractSensorValues(snapshot);

        // Проверяем условия и собираем действия
        List<DeviceActionRequest> actionsToExecute = new ArrayList<>();

        for (Scenario scenario : scenarios) {
            if (checkScenarioConditions(scenario, sensorValues)) {
                log.debug("Сценарий '{}' активирован для хаба {}", scenario.getName(), hubId);
                for (ScenarioAction action : scenario.getScenarioActions()) {
                    actionsToExecute.add(createActionRequest(hubId, scenario.getName(), action));
                }
            }
        }

        // Выполняем действия
        for (DeviceActionRequest action : actionsToExecute) {
            executeAction(action);
        }
    }

    private List<Scenario> getScenariosForHub(String hubId) {
        if (hubScenariosCache.containsKey(hubId)) {
            return hubScenariosCache.get(hubId);
        }

        List<Scenario> scenarios = scenarioRepository.findByHubIdWithAllRelations(hubId);
        hubScenariosCache.put(hubId, scenarios);
        return scenarios;
    }


    private Map<String, Object> extractSensorValues(SensorsSnapshotAvro snapshot) {
        Map<String, Object> values = new HashMap<>();

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

            switch (data) {
                case MotionSensorAvro motionSensor -> values.put(sensorId, motionSensor.getMotion() ? 1 : 0);

                case TemperatureSensorAvro temperatureSensor ->
                        values.put(sensorId, temperatureSensor.getTemperatureC());

                case LightSensorAvro lightSensor -> values.put(sensorId, lightSensor.getLuminosity());

                case ClimateSensorAvro climateSensor -> {
                    values.put(sensorId + ".temperature", climateSensor.getTemperatureC());
                    values.put(sensorId + ".humidity", climateSensor.getHumidity());
                    values.put(sensorId + ".co2", climateSensor.getCo2Level());
                    values.put(sensorId, climateSensor.getTemperatureC());
                }

                case SwitchSensorAvro switchSensor -> values.put(sensorId, switchSensor.getState() ? 1 : 0);

                case null, default -> log.warn("Неизвестный или null тип датчика: {}",
                        data == null ? "null" : data.getClass().getSimpleName());
            }
        }

        return values;
    }

    /**
     * Проверяет все условия сценария
     */
    private boolean checkScenarioConditions(Scenario scenario, Map<String, Object> sensorValues) {
        for (ScenarioCondition scenarioCondition : scenario.getScenarioConditions()) {
            Condition condition = scenarioCondition.getCondition();
            String sensorId = scenarioCondition.getSensor().getId();

            Object sensorValue = sensorValues.get(sensorId);

            // Для климат-датчика может понадобиться специфичное поле
            if (sensorValue == null) {
                sensorValue = findClimateSensorValue(sensorValues, sensorId, condition.getType());
            }

            if (sensorValue == null) {
                log.debug("Нет значения для датчика {} в сценарии {}", sensorId, scenario.getName());
                return false;
            }

            if (!checkCondition(condition, sensorValue)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Для климат-датчика ищет нужное значение (температура, влажность, CO2)
     */
    private Object findClimateSensorValue(Map<String, Object> sensorValues,
                                          String sensorId,
                                          ConditionType conditionType) {
        String key = switch (conditionType) {
            case TEMPERATURE -> sensorId + ".temperature";
            case HUMIDITY -> sensorId + ".humidity";
            case CO2LEVEL -> sensorId + ".co2";
            default -> sensorId;
        };
        return sensorValues.get(key);
    }

    /**
     * Проверяет одно условие
     */
    private boolean checkCondition(Condition condition, Object sensorValue) {
        int sensorIntValue = convertToInt(sensorValue);
        int conditionValue = condition.getValue() != null ? condition.getValue() : 0;

        return switch (condition.getOperation()) {
            case EQUALS -> sensorIntValue == conditionValue;
            case GREATER_THAN -> sensorIntValue > conditionValue;
            case LOWER_THAN -> sensorIntValue < conditionValue;
        };
    }

    /**
     * Преобразует значение датчика в int
     */
    private int convertToInt(Object value) {
        return switch (value) {
            case Integer i -> i;
            case Boolean b -> b ? 1 : 0;
            case Long l -> l.intValue();
            case Double d -> d.intValue();
            case Float f -> f.intValue();
            case null -> 0;
            default -> {
                log.warn("Неизвестный тип значения: {}", value.getClass().getSimpleName());
                yield 0;
            }
        };
    }

    /**
     * Создает gRPC запрос для выполнения действия
     */
    private DeviceActionRequest createActionRequest(String hubId, String scenarioName,
                                                    ScenarioAction scenarioAction) {
        Action action = scenarioAction.getAction();
        String sensorId = scenarioAction.getSensor().getId();

        DeviceActionProto.Builder actionBuilder = DeviceActionProto.newBuilder()
                .setSensorId(sensorId)
                .setType(convertActionType(action.getType()));

        if (action.getValue() != null) {
            actionBuilder.setValue(action.getValue());
        }

        return DeviceActionRequest.newBuilder()
                .setHubId(hubId)
                .setScenarioName(scenarioName)
                .setTimestamp(com.google.protobuf.Timestamp.newBuilder()
                        .setSeconds(Instant.now().getEpochSecond())
                        .setNanos(Instant.now().getNano())
                        .build())
                .setAction(actionBuilder.build())
                .build();
    }

    /**
     * Конвертирует ActionType в gRPC ActionTypeProto
     */
    private ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto convertActionType(ActionType type) {
        return switch (type) {
            case ACTIVATE -> ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto.ACTIVATE;
            case DEACTIVATE -> ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto.DEACTIVATE;
            case INVERSE -> ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto.INVERSE;
            case SET_VALUE -> ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto.SET_VALUE;
        };
    }

    /**
     * Выполняет действие через gRPC
     */
    private void executeAction(DeviceActionRequest action) {
        try {
            hubRouterClient.handleDeviceAction(action);
            log.debug("Выполнено действие: hub={}, scenario={}, sensor={}, type={}",
                    action.getHubId(),
                    action.getScenarioName(),
                    action.getAction().getSensorId(),
                    action.getAction().getType());
        } catch (StatusRuntimeException e) {
            log.error("gRPC ошибка при выполнении действия для хаба {}: status={}, message={}",
                    action.getHubId(), e.getStatus().getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Ошибка при выполнении действия для хаба {}: {}",
                    action.getHubId(), e.getMessage());
        }
    }

    /**
     * Обновляет кэш при изменении сценариев
     */
    public void invalidateCache(String hubId) {
        hubScenariosCache.remove(hubId);
        log.debug("Инвалидирован кэш для хаба {}", hubId);
    }
}
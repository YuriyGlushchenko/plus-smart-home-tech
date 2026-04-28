package ru.yandex.practicum.analyzer.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.analyzer.models.*;
import ru.yandex.practicum.analyzer.repositories.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class HubEventServiceImpl implements HubEventService {
    private final ActionRepository actionRepository;
    private final ConditionRepository conditionRepository;
    private final ScenarioRepository scenarioRepository;
    private final SensorRepository sensorRepository;
    private final ScenarioActionRepository scenarioActionRepository;
    private final ScenarioConditionRepository scenarioConditionRepository;
    private final SnapshotService snapshotService;

    public void handleHubEvent(HubEventAvro event) {
        switch (event.getPayload()) {
            case ScenarioAddedEventAvro payload -> handleScenarioAdded(event.getHubId(), payload);
            case ScenarioRemovedEventAvro payload -> handleScenarioRemoved(event.getHubId(), payload);
            case DeviceAddedEventAvro payload -> handleDeviceAdded(event.getHubId(), payload);
            case DeviceRemovedEventAvro payload -> handleDeviceRemoved(event.getHubId(), payload);
            case null, default -> log.warn("Unknown or null payload type");
        }
    }

    @Transactional
    public void handleDeviceAdded(String hubId, DeviceAddedEventAvro payload) {
        String deviceId = payload.getId();
        log.info("Обработка DeviceAdded события: hubId={}, deviceId={}, type={}",
                hubId, deviceId, payload.getType());

        if (sensorRepository.existsById(deviceId)) {
            log.info("Датчик '{}' уже существует, пропускаем", deviceId);
            return;
        }

        Sensor sensor = Sensor.builder()
                .id(deviceId)
                .hubId(hubId)
                .build();

        sensorRepository.save(sensor);
        log.info("Датчик '{}' успешно добавлен в хаб '{}'", deviceId, hubId);
    }

    @Transactional
    public void handleScenarioRemoved(String hubId, ScenarioRemovedEventAvro payload) {
        String scenarioName = payload.getName();
        log.info("Обработка ScenarioRemoved события: hubId={}, name={}", hubId, scenarioName);

        Scenario scenario = scenarioRepository.findByHubIdAndName(hubId, scenarioName)
                .orElseThrow(() -> {
                    log.error("Сценарий '{}/{}' не найден", hubId, scenarioName);
                    return new IllegalArgumentException(String.format("Сценарий '%s' не найден для хаба '%s'", scenarioName, hubId)
                    );
                });

        // CascadeType.ALL и orphanRemoval -> все связанные записи в scenario_conditions и scenario_actions удалятся
        scenarioRepository.delete(scenario);

        log.info("Сценарий '{}/{}' успешно удален вместе со всеми условиями и действиями", hubId, scenarioName);

        snapshotService.invalidateCache(hubId);
    }

    @Transactional
    public void handleDeviceRemoved(String hubId, DeviceRemovedEventAvro payload) {
        String deviceId = payload.getId();
        log.info("Обработка DeviceRemoved события: hubId={}, deviceId={}", hubId, deviceId);

        Sensor sensor = sensorRepository.findByIdAndHubId(deviceId, hubId)
                .orElseThrow(() -> {
                    log.error("Датчик '{}/{}' не найден", hubId, deviceId);
                    return new IllegalArgumentException(String.format("Датчик '%s' не найден для хаба '%s'", deviceId, hubId)
                    );
                });

        try {
            sensorRepository.delete(sensor);
            log.info("Датчик '{}/{}' успешно удален", hubId, deviceId);
            snapshotService.invalidateCache(hubId);
        } catch (DataIntegrityViolationException e) {
            // Нарушение FOREIGN KEY - датчик используется в сценариях
            log.error("Невозможно удалить датчик '{}/{}' - он используется в сценариях", hubId, deviceId);
            throw new IllegalStateException(
                    String.format("Датчик '%s' используется в сценариях, не может быть удален", deviceId), e
            );
        }
    }

    @Transactional
    public void handleScenarioAdded(String hubId, ScenarioAddedEventAvro payload) {
        String scenarioName = payload.getName();
        log.info("Обработка ScenarioAdded события: hubId={}, name={}", hubId, scenarioName);


        // перед тем как что-то делать, проверяем что все сенсоры из сценария существуют
        validateSensors(hubId, payload);

        // Создаем сам сценарий (hubId + name)
        Scenario scenario = Scenario.builder()
                .hubId(hubId)
                .name(scenarioName)
                .build();

        try {
            scenario = scenarioRepository.save(scenario);
        } catch (
                DataIntegrityViolationException e) {  // вместо проверки на дубликат ловим ошибку UNIQUE SQL при дубликате
            log.warn("Сценарий '{}/{}' уже существует, пропускаем", hubId, scenarioName);

            return;
        }

        // Сохраняем условия в табл conditions (type(MOTION..), operation(EQUALS..), value)
        // порядок элементов такой же как был изначально в списке payload.getConditions()
        List<Condition> conditions = saveScenarioConditions(payload.getConditions());

        // Сохраняем действия в табл actions ( type(ACTIVATE..), value)
        List<Action> actions = saveScenarioActions(payload.getActions());

        // создаём связи вручную из-за составного первичного ключа
        try {
            // Связи для условий в табл. scenario-condition(scenario_id, sensor_id, condition_id)
            List<ScenarioCondition> scenarioConditions = new ArrayList<>();

            for (int i = 0; i < conditions.size(); i++) {
                Condition condition = conditions.get(i);  // порядок элементов совпадает с изначальным списком условий
                ScenarioConditionAvro conditionAvro = payload.getConditions().get(i);

                ScenarioConditionId id = new ScenarioConditionId(  // составной PK для таблицы scenario_conditions
                        scenario.getId(),
                        conditionAvro.getSensorId(),
                        condition.getId()
                );

                Sensor sensor = sensorRepository.getReferenceById(conditionAvro.getSensorId());

                ScenarioCondition scenarioCondition = ScenarioCondition.builder()
                        .id(id)
                        .scenario(scenario)      // ⬅️ Добавить
                        .sensor(sensor)          // ⬅️ Добавить
                        .condition(condition)    // ⬅️ Добавить
                        .build();

                scenarioConditions.add(scenarioCondition);

            }

            if (!scenarioConditions.isEmpty()) {
                scenarioConditionRepository.saveAll(scenarioConditions);
            }

            // Связи для действий в табл scenario_actions (scenario_id, sensor_id, action_id)
            List<ScenarioAction> scenarioActions = new ArrayList<>();
            for (int i = 0; i < actions.size(); i++) {
                Action action = actions.get(i);
                DeviceActionAvro actionAvro = payload.getActions().get(i);
                Sensor sensor = sensorRepository.getReferenceById(actionAvro.getSensorId());

                ScenarioActionId id = new ScenarioActionId( // составной PK для таблицы scenario_actions
                        scenario.getId(),
                        actionAvro.getSensorId(),
                        action.getId()
                );

                ScenarioAction scenarioAction = ScenarioAction.builder()
                        .id(id)
                        .scenario(scenario)  // ⬅️ Добавить
                        .sensor(sensor)      // ⬅️ Добавить
                        .action(action)      // ⬅️ Добавить
                        .build();

                scenarioActions.add(scenarioAction);

            }

            if (!scenarioActions.isEmpty()) {
                scenarioActionRepository.saveAll(scenarioActions);
            }

        } catch (DataIntegrityViolationException e) {
            log.error("Не удалось создать сценарий '{}' для хаба '{}': {}",
                    scenarioName, hubId, e.getMessage());

            throw new IllegalArgumentException("Invalid sensors in scenario: " + e.getMessage(), e);
        }

        log.info("Сценарий '{}' успешно добавлен с {} условиями и {} действиями",
                scenarioName, conditions.size(), actions.size());
        snapshotService.invalidateCache(hubId);
    }

    private Integer extractIntValue(Object value) {
        return switch (value) {
            case null -> null;
            case Integer i -> i;
            case Boolean b -> b ? 1 : 0;
            default -> {
                log.warn("Неожиданный тип значения: {}", value.getClass().getSimpleName());
                yield null;
            }
        };
    }

    private List<Condition> saveScenarioConditions(List<ScenarioConditionAvro> conditionsAvro) {
        List<Condition> conditions = new ArrayList<>();
        for (ScenarioConditionAvro conditionAvro : conditionsAvro) {
            conditions.add(Condition.builder()
                    .type(ConditionType.fromAvro(conditionAvro.getType()))
                    .operation(ConditionOperation.fromAvro(conditionAvro.getOperation()))
                    .value(extractIntValue(conditionAvro.getValue()))
                    .build());
        }

        if (!conditions.isEmpty()) {
            conditions = conditionRepository.saveAll(conditions); // порядок элементов такой же как был изначально в списке payload.getConditions()
        }
        return conditions;
    }

    private List<Action> saveScenarioActions(List<DeviceActionAvro> actionsAvro) {
        List<Action> actions = new ArrayList<>();
        for (DeviceActionAvro actionAvro : actionsAvro) {
            actions.add(Action.builder()
                    .type(ActionType.fromAvro(actionAvro.getType()))
                    .value(extractIntValue(actionAvro.getValue()))
                    .build());
        }

        if (!actions.isEmpty()) {
            actions = actionRepository.saveAll(actions);
        }
        return actions;
    }

    private void validateSensors(String hubId, ScenarioAddedEventAvro payload) {
        Set<String> sensorIds = new HashSet<>();
        payload.getConditions().forEach(c -> sensorIds.add(c.getSensorId()));
        payload.getActions().forEach(a -> sensorIds.add(a.getSensorId()));

        if (sensorIds.isEmpty()) return;

        if (!sensorRepository.existsByIdInAndHubId(sensorIds, hubId)) {
            throw new IllegalArgumentException(
                    String.format("Not all sensors exist for hub '%s'. Required: %s",
                            hubId, sensorIds)
            );
        }
    }


}

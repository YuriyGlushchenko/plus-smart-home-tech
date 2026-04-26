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
import java.util.List;

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

    public void handleHubEvent(HubEventAvro event) {
        switch (event.getPayload()) {
            case ScenarioAddedEventAvro payload -> handleScenarioAdded(event.getHubId(), payload);
            case ScenarioRemovedEventAvro payload -> handleScenarioRemoved(event.getHubId(), payload);
            case DeviceAddedEventAvro payload -> handleDeviceAdded(event.getHubId(), payload);
            case DeviceRemovedEventAvro payload -> handleDeviceRemoved(event.getHubId(), payload);
            case null, default -> log.warn("Unknown or null payload type");
        }
    }

//    private void handleScenarioAdded(String hubId, ScenarioAddedEventAvro payload){
//
//    }

    private void handleDeviceAdded(String hubId, DeviceAddedEventAvro payload){

    }

    private void handleScenarioRemoved(String hubId, ScenarioRemovedEventAvro payload){

    }

    private void handleDeviceRemoved(String hubId, DeviceRemovedEventAvro payload){

    }

    @Override
    @Transactional
    public void handleScenarioAdded(String hubId, ScenarioAddedEventAvro payload) {
        String scenarioName = payload.getName();
        log.info("Processing ScenarioAdded event: hubId={}, name={}", hubId, scenarioName);

        // 1. Создаем сценарий (БД проверит UNIQUE)
        Scenario scenario = Scenario.builder()
                .hubId(hubId)
                .name(scenarioName)
                .build();

        try {
            scenario = scenarioRepository.save(scenario);
        } catch (DataIntegrityViolationException e) {
            log.warn("Scenario '{}/{}' already exists, skipping", hubId, scenarioName);
            return;
        }

        // 2. Создаем условия
        List<Condition> conditions = new ArrayList<>();
        for (ScenarioConditionAvro conditionAvro : payload.getConditions()) {
            conditions.add(Condition.builder()
                    .type(convertConditionType(conditionAvro.getType()))
                    .operation(convertConditionOperation(conditionAvro.getOperation()))
                    .value(extractIntValue(conditionAvro.getValue()))
                    .build());
        }

        if (!conditions.isEmpty()) {
            conditions = conditionRepository.saveAll(conditions);
        }

        // 3. Создаем действия
        List<Action> actions = new ArrayList<>();
        for (DeviceActionAvro actionAvro : payload.getActions()) {
            actions.add(Action.builder()
                    .type(convertActionType(actionAvro.getType()))
                    .value(extractIntValue(actionAvro.getValue()))
                    .build());
        }

        if (!actions.isEmpty()) {
            actions = actionRepository.saveAll(actions);
        }

        // 4. Пытаемся создать связи - БД проверит FOREIGN KEY и триггеры
        try {
            // Связи условий
            List<ScenarioCondition> scenarioConditions = new ArrayList<>();
            for (int i = 0; i < conditions.size(); i++) {
                Condition condition = conditions.get(i);
                ScenarioConditionAvro conditionAvro = payload.getConditions().get(i);

                ScenarioConditionId id = new ScenarioConditionId(
                        scenario.getId(),
                        conditionAvro.getSensorId(),
                        condition.getId()
                );

                scenarioConditions.add(ScenarioCondition.builder()
                        .id(id)
                        .scenario(scenario)
                        .condition(condition)
                        .build());
            }

            if (!scenarioConditions.isEmpty()) {
                scenarioConditionRepository.saveAll(scenarioConditions);
            }

            // Связи действий
            List<ScenarioAction> scenarioActions = new ArrayList<>();
            for (int i = 0; i < actions.size(); i++) {
                Action action = actions.get(i);
                DeviceActionAvro actionAvro = payload.getActions().get(i);

                ScenarioActionId id = new ScenarioActionId(
                        scenario.getId(),
                        actionAvro.getSensorId(),
                        action.getId()
                );

                scenarioActions.add(ScenarioAction.builder()
                        .id(id)
                        .scenario(scenario)
                        .action(action)
                        .build());
            }

            if (!scenarioActions.isEmpty()) {
                scenarioActionRepository.saveAll(scenarioActions);
            }

        } catch (DataIntegrityViolationException e) {
            // Нарушение FOREIGN KEY - датчик не существует или hub_id не совпадает
            log.error("Failed to create scenario '{}' for hub '{}': {}",
                    scenarioName, hubId, e.getMessage());

            // Откатываем транзакцию (она и так откатится из-за unchecked exception)
            throw new IllegalArgumentException("Invalid sensors in scenario: " + e.getMessage(), e);
        }

        log.info("Successfully added scenario '{}' with {} conditions and {} actions",
                scenarioName, conditions.size(), actions.size());
    }



}

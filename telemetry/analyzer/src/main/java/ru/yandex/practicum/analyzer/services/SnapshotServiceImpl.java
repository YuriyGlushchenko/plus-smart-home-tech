package ru.yandex.practicum.analyzer.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.analyzer.repositories.*;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

@Service
@Slf4j
@RequiredArgsConstructor
public class SnapshotServiceImpl implements SnapshotService{
    private final ActionRepository actionRepository;
    private final ConditionRepository conditionRepository;
    private final ScenarioRepository scenarioRepository;
    private final SensorRepository sensorRepository;
    private final ScenarioActionRepository scenarioActionRepository;
    private final ScenarioConditionRepository scenarioConditionRepository;

    @Override
    public void handleSnapshot(SensorsSnapshotAvro snapshotAvro) {

    }
}

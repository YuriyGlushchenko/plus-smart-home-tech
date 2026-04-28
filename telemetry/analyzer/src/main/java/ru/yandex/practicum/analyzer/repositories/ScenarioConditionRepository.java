package ru.yandex.practicum.analyzer.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.analyzer.models.ScenarioCondition;
import ru.yandex.practicum.analyzer.models.ScenarioConditionId;

import java.util.List;

public interface ScenarioConditionRepository extends JpaRepository<ScenarioCondition, ScenarioConditionId> {
    List<ScenarioCondition> findByScenarioId(Long scenarioId);
}

package ru.yandex.practicum.analyzer.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.analyzer.models.ScenarioCondition;
import ru.yandex.practicum.analyzer.models.ScenarioConditionId;

public interface ScenarioConditionRepository extends JpaRepository<ScenarioCondition, ScenarioConditionId> {
}

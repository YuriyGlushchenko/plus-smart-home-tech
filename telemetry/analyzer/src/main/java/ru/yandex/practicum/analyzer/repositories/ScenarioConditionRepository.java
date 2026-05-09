package ru.yandex.practicum.analyzer.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.yandex.practicum.analyzer.models.ScenarioCondition;
import ru.yandex.practicum.analyzer.models.ScenarioConditionId;

import java.util.List;

public interface ScenarioConditionRepository extends JpaRepository<ScenarioCondition, ScenarioConditionId> {
    List<ScenarioCondition> findByScenarioId(Long scenarioId);

    @Query("SELECT sc FROM ScenarioCondition sc " +
            "LEFT JOIN FETCH sc.sensor " +
            "LEFT JOIN FETCH sc.condition " +
            "WHERE sc.scenario.id IN :scenarioIds")
    List<ScenarioCondition> findByScenarioIdsWithFetch(@Param("scenarioIds") List<Long> scenarioIds);
}

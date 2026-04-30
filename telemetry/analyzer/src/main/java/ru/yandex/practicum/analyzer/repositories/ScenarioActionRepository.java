package ru.yandex.practicum.analyzer.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.analyzer.models.ScenarioAction;
import ru.yandex.practicum.analyzer.models.ScenarioActionId;

import java.util.List;

@Repository
public interface ScenarioActionRepository extends JpaRepository<ScenarioAction, ScenarioActionId> {
    List<ScenarioAction> findByScenarioId(Long scenarioId);

    @Query("SELECT sa FROM ScenarioAction sa " +
            "LEFT JOIN FETCH sa.sensor " +
            "LEFT JOIN FETCH sa.action " +
            "WHERE sa.scenario.id IN :scenarioIds")
    List<ScenarioAction> findByScenarioIdsWithFetch(@Param("scenarioIds") List<Long> scenarioIds);
}

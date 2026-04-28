package ru.yandex.practicum.analyzer.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.analyzer.models.ScenarioAction;
import ru.yandex.practicum.analyzer.models.ScenarioActionId;

import java.util.List;

@Repository
public interface ScenarioActionRepository extends JpaRepository<ScenarioAction, ScenarioActionId> {
    List<ScenarioAction> findByScenarioId(Long scenarioId);
}

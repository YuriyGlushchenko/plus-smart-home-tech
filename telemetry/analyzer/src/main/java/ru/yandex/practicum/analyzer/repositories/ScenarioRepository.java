package ru.yandex.practicum.analyzer.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.yandex.practicum.analyzer.models.Scenario;

import java.util.List;
import java.util.Optional;

public interface ScenarioRepository extends JpaRepository<Scenario, Long> {

    List<Scenario> findByHubId(String hubId);

    Optional<Scenario> findByHubIdAndName(String hubId, String name);

    //  JOIN FETCH чтобы не грузить данные потом по отдельности в цикле для scenarioActions и scenarioConditions
    @Query("SELECT DISTINCT s FROM Scenario s " +
            "LEFT JOIN FETCH s.scenarioConditions sc " +
            "LEFT JOIN FETCH sc.sensor " +
            "LEFT JOIN FETCH sc.condition " +
            "LEFT JOIN FETCH s.scenarioActions sa " +
            "LEFT JOIN FETCH sa.sensor " +
            "LEFT JOIN FETCH sa.action " +
            "WHERE s.hubId = :hubId")
    List<Scenario> findByHubIdWithAllRelations(@Param("hubId") String hubId);
}

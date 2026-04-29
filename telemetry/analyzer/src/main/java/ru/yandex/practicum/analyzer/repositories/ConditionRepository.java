package ru.yandex.practicum.analyzer.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.analyzer.models.Condition;

public interface ConditionRepository extends JpaRepository<Condition, Long> {
}

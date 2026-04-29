package ru.yandex.practicum.analyzer.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.analyzer.models.Action;

public interface ActionRepository extends JpaRepository<Action, Long> {
}

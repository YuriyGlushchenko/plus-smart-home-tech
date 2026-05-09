package ru.yandex.practicum.analyzer.services;

import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

public interface SnapshotService {

    void handleSnapshot(SensorsSnapshotAvro snapshotAvro);

    void invalidateCache(String hubId);
}

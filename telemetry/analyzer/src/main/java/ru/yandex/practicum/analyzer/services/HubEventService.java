package ru.yandex.practicum.analyzer.services;

import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

public interface HubEventService {

    void handleHubEvent(HubEventAvro eventAvro);
}

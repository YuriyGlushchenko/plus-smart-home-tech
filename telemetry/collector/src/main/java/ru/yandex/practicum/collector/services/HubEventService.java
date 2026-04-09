package ru.yandex.practicum.collector.services;

import ru.yandex.practicum.collector.models.hubEvents.HubEvent;

public interface HubEventService {
    void processHubEvent(HubEvent event);

}

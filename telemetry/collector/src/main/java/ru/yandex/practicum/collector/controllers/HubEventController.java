package ru.yandex.practicum.collector.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.collector.models.hubEvents.HubEvent;
import ru.yandex.practicum.collector.services.HubEventService;


@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
public class HubEventController {
    private final HubEventService hubEventService;

    @PostMapping("/events/hubs")
    public void collectHubEvent(@Valid @RequestBody HubEvent event) {
        log.info("Received hub event: type={}, hubId={}", event.getType(), event.getHubId());
        log.debug("Hub event details: {}", event);

        hubEventService.processHubEvent(event);
    }
}

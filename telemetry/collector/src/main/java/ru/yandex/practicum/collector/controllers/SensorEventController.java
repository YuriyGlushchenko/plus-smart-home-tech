package ru.yandex.practicum.collector.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.collector.models.sensorEvents.SensorEvent;
import ru.yandex.practicum.collector.services.SensorEventService;


@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
public class SensorEventController {
    private final SensorEventService sensorEventService;

    @PostMapping("/events/sensors")
    public void collectSensorEvent(@Valid @RequestBody SensorEvent event) {
        sensorEventService.processSensorEvent(event);
    }
}

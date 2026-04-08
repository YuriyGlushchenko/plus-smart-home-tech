package ru.yandex.practicum.collector;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.collector.sensorEvents.SensorEvent;


@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
public class CollectorController {
    private final CollectorService collectorService;

    @PostMapping("/sensors")
    public void collectSensorEvent(@Valid @RequestBody SensorEvent event) {
        collectorService.processSensorEvent(event);
    }
}

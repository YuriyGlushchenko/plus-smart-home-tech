package ru.yandex.practicum.collector.services;

import ru.yandex.practicum.collector.models.sensorEvents.SensorEvent;

public interface SensorEventService {
    void processSensorEvent(SensorEvent event);

}

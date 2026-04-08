package ru.yandex.practicum.collector;

import ru.yandex.practicum.collector.sensorEvents.SensorEvent;

public interface CollectorService {
    void processSensorEvent(SensorEvent event);

}

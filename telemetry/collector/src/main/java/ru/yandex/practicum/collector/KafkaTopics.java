package ru.yandex.practicum.collector;

public final class KafkaTopics {

    private KafkaTopics() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static final String SENSORS_TOPIC = "telemetry.sensors.v1";
    public static final String HUBS_TOPIC = "telemetry.hubs.v1";
}
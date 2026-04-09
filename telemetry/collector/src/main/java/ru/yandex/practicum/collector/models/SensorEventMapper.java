package ru.yandex.practicum.collector.models;

import ru.yandex.practicum.collector.models.sensorEvents.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

public class SensorEventMapper {

    private SensorEventMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static SensorEventAvro toAvro(SensorEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("SensorEvent cannot be null");
        }

        return SensorEventAvro.newBuilder()
                .setId(event.getId())
                .setHubId(event.getHubId())
                .setTimestamp(event.getTimestamp())
                .setPayload(createPayload(event))  // смотря какой тип SensorEvent, будет вызываться нужная перегрузка createPayload
                .build();
    }

    private static Object createPayload(LightSensorEvent event) {
        return LightSensorAvro.newBuilder()
                .setLinkQuality(event.getLinkQuality())
                .setLuminosity(event.getLuminosity())
                .build();
    }

    private static Object createPayload(MotionSensorEvent event) {
        return MotionSensorAvro.newBuilder()
                .setLinkQuality(event.getLinkQuality())
                .setMotion(event.isMotion())
                .setVoltage(event.getVoltage())
                .build();
    }

    private static Object createPayload(ClimateSensorEvent event) {
        return ClimateSensorAvro.newBuilder()
                .setTemperatureC(event.getTemperature_c())
                .setHumidity(event.getHumidity())
                .setCo2Level(event.getCo2_level())
                .build();
    }

    private static Object createPayload(SwitchSensorEvent event) {
        return SwitchSensorAvro.newBuilder()
                .setState(event.isState())
                .build();
    }

    private static Object createPayload(TemperatureSensorEvent event) {
        return TemperatureSensorAvro.newBuilder()
                .setTemperatureC(event.getTemperature_c())
                .setTemperatureF(event.getTemperature_f())
                .build();
    }

    private static Object createPayload(SensorEvent event) {
        throw new IllegalArgumentException(
                "Unsupported sensor event type: " + event.getClass().getSimpleName()
        );
    }
}
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
                .setPayload(createPayloadByType(event))
                .build();
    }

    private static Object createPayloadByType(SensorEvent event) {
        if (event instanceof LightSensorEvent light) {
            return createPayload(light);
        }
        if (event instanceof MotionSensorEvent motion) {
            return createPayload(motion);
        }
        if (event instanceof ClimateSensorEvent climate) {
            return createPayload(climate);
        }
        if (event instanceof SwitchSensorEvent switchEvent) {
            return createPayload(switchEvent);
        }
        if (event instanceof TemperatureSensorEvent temp) {
            return createPayload(temp);
        }
        throw new IllegalArgumentException(
                "Unsupported sensor event type: " + event.getClass().getSimpleName()
        );
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
                .setTemperatureC(event.getTemperatureC())
                .setHumidity(event.getHumidity())
                .setCo2Level(event.getCo2Level())
                .build();
    }

    private static Object createPayload(SwitchSensorEvent event) {
        return SwitchSensorAvro.newBuilder()
                .setState(event.isState())
                .build();
    }

    private static Object createPayload(TemperatureSensorEvent event) {
        return TemperatureSensorAvro.newBuilder()
                .setTemperatureC(event.getTemperatureC())
                .setTemperatureF(event.getTemperatureF())
                .build();
    }

    private static Object createPayload(SensorEvent event) {
        throw new IllegalArgumentException(
                "Unsupported sensor event type: " + event.getClass().getSimpleName()
        );
    }
}
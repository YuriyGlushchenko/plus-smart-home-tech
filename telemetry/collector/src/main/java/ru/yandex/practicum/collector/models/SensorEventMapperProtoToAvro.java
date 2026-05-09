package ru.yandex.practicum.collector.models;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Instant;

@Component
public class SensorEventMapperProtoToAvro {

    public SensorEventAvro toAvro(SensorEventProto proto) {
        if (proto == null) {
            throw new IllegalArgumentException("SensorEventProto cannot be null");
        }

        return SensorEventAvro.newBuilder()
                .setId(proto.getId())
                .setHubId(proto.getHubId())
                .setTimestamp(convertToInstant(proto.getTimestamp()))
                .setPayload(createPayloadByType(proto))
                .build();
    }

    private Object createPayloadByType(SensorEventProto proto) {
        return switch (proto.getPayloadCase()) {
            case LIGHT_SENSOR -> createLightPayload(proto.getLightSensor());
            case MOTION_SENSOR -> createMotionPayload(proto.getMotionSensor());
            case CLIMATE_SENSOR -> createClimatePayload(proto.getClimateSensor());
            case SWITCH_SENSOR -> createSwitchPayload(proto.getSwitchSensor());
            case TEMPERATURE_SENSOR -> createTemperaturePayload(proto.getTemperatureSensor());
            case PAYLOAD_NOT_SET -> throw new IllegalArgumentException(
                    "Payload not set in SensorEventProto");
        };
    }

    private LightSensorAvro createLightPayload(LightSensorProto proto) {
        return LightSensorAvro.newBuilder()
                .setLinkQuality(proto.getLinkQuality())
                .setLuminosity(proto.getLuminosity())
                .build();
    }

    private MotionSensorAvro createMotionPayload(MotionSensorProto proto) {
        return MotionSensorAvro.newBuilder()
                .setLinkQuality(proto.getLinkQuality())
                .setMotion(proto.getMotion())
                .setVoltage(proto.getVoltage())
                .build();
    }

    private ClimateSensorAvro createClimatePayload(ClimateSensorProto proto) {
        return ClimateSensorAvro.newBuilder()
                .setTemperatureC(proto.getTemperatureC())
                .setHumidity(proto.getHumidity())
                .setCo2Level(proto.getCo2Level())
                .build();
    }

    private SwitchSensorAvro createSwitchPayload(SwitchSensorProto proto) {
        return SwitchSensorAvro.newBuilder()
                .setState(proto.getState())
                .build();
    }

    private TemperatureSensorAvro createTemperaturePayload(TemperatureSensorProto proto) {
        return TemperatureSensorAvro.newBuilder()
                .setTemperatureC(proto.getTemperatureC())
                .setTemperatureF(proto.getTemperatureF())
                .build();
    }

    private Instant convertToInstant(com.google.protobuf.Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }
}
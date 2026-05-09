package ru.yandex.practicum.collector.models;

import com.google.protobuf.Timestamp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class HubEventMapperProtoToAvro {

    public HubEventAvro toAvro(HubEventProto proto) {
        if (proto == null) {
            throw new IllegalArgumentException("HubEventProto cannot be null");
        }

        return HubEventAvro.newBuilder()
                .setHubId(proto.getHubId())
                .setTimestamp(convertToInstant(proto.getTimestamp()))
                .setPayload(createPayloadByType(proto))
                .build();
    }

    private Object createPayloadByType(HubEventProto proto) {
        return switch (proto.getPayloadCase()) {
            case DEVICE_ADDED -> createDeviceAddedPayload(proto.getDeviceAdded());
            case DEVICE_REMOVED -> createDeviceRemovedPayload(proto.getDeviceRemoved());
            case SCENARIO_ADDED -> createScenarioAddedPayload(proto.getScenarioAdded());
            case SCENARIO_REMOVED -> createScenarioRemovedPayload(proto.getScenarioRemoved());
            case PAYLOAD_NOT_SET -> throw new IllegalArgumentException(
                    "Payload not set in HubEventProto");
        };
    }

    private DeviceAddedEventAvro createDeviceAddedPayload(DeviceAddedEventProto proto) {
        return DeviceAddedEventAvro.newBuilder()
                .setId(proto.getId())
                .setType(convertDeviceType(proto.getType()))
                .build();
    }

    private DeviceRemovedEventAvro createDeviceRemovedPayload(DeviceRemovedEventProto proto) {
        return DeviceRemovedEventAvro.newBuilder()
                .setId(proto.getId())
                .build();
    }

    private ScenarioAddedEventAvro createScenarioAddedPayload(ScenarioAddedEventProto proto) {
        List<ScenarioConditionAvro> conditions = proto.getConditionList().stream()
                .map(this::convertCondition)
                .collect(Collectors.toList());

        List<DeviceActionAvro> actions = proto.getActionList().stream()
                .map(this::convertAction)
                .collect(Collectors.toList());

        return ScenarioAddedEventAvro.newBuilder()
                .setName(proto.getName())
                .setConditions(conditions)
                .setActions(actions)
                .build();
    }

    private ScenarioRemovedEventAvro createScenarioRemovedPayload(ScenarioRemovedEventProto proto) {
        return ScenarioRemovedEventAvro.newBuilder()
                .setName(proto.getName())
                .build();
    }

    private ScenarioConditionAvro convertCondition(ScenarioConditionProto proto) {
        Object value = switch (proto.getValueCase()) {
            case BOOL_VALUE -> proto.getBoolValue();
            case INT_VALUE -> proto.getIntValue();
            case VALUE_NOT_SET -> throw new IllegalArgumentException(
                    "Value not set in ScenarioConditionProto");
        };

        return ScenarioConditionAvro.newBuilder()
                .setSensorId(proto.getSensorId())
                .setType(convertConditionType(proto.getType()))
                .setOperation(convertConditionOperation(proto.getOperation()))
                .setValue(value)
                .build();
    }

    private DeviceActionAvro convertAction(DeviceActionProto proto) {
        DeviceActionAvro.Builder builder = DeviceActionAvro.newBuilder()
                .setSensorId(proto.getSensorId())
                .setType(convertActionType(proto.getType()));

        if (proto.hasValue()) {
            builder.setValue(proto.getValue());
        }

        return builder.build();
    }

    private DeviceTypeAvro convertDeviceType(DeviceTypeProto deviceType) {
        return switch (deviceType) {
            case MOTION_SENSOR -> DeviceTypeAvro.MOTION_SENSOR;
            case TEMPERATURE_SENSOR -> DeviceTypeAvro.TEMPERATURE_SENSOR;
            case LIGHT_SENSOR -> DeviceTypeAvro.LIGHT_SENSOR;
            case CLIMATE_SENSOR -> DeviceTypeAvro.CLIMATE_SENSOR;
            case SWITCH_SENSOR -> DeviceTypeAvro.SWITCH_SENSOR;
            case UNRECOGNIZED -> throw new IllegalArgumentException(
                    "Unknown device type: " + deviceType);
        };
    }

    private ConditionTypeAvro convertConditionType(ConditionTypeProto conditionType) {
        return switch (conditionType) {
            case MOTION -> ConditionTypeAvro.MOTION;
            case LUMINOSITY -> ConditionTypeAvro.LUMINOSITY;
            case SWITCH -> ConditionTypeAvro.SWITCH;
            case TEMPERATURE -> ConditionTypeAvro.TEMPERATURE;
            case CO2LEVEL -> ConditionTypeAvro.CO2LEVEL;
            case HUMIDITY -> ConditionTypeAvro.HUMIDITY;
            case UNRECOGNIZED -> throw new IllegalArgumentException(
                    "Unknown condition type: " + conditionType);
        };
    }

    private ConditionOperationAvro convertConditionOperation(ConditionOperationProto operation) {
        return switch (operation) {
            case EQUALS -> ConditionOperationAvro.EQUALS;
            case GREATER_THAN -> ConditionOperationAvro.GREATER_THAN;
            case LOWER_THAN -> ConditionOperationAvro.LOWER_THAN;
            case UNRECOGNIZED -> throw new IllegalArgumentException(
                    "Unknown operation: " + operation);
        };
    }

    private ActionTypeAvro convertActionType(ActionTypeProto actionType) {
        return switch (actionType) {
            case ACTIVATE -> ActionTypeAvro.ACTIVATE;
            case DEACTIVATE -> ActionTypeAvro.DEACTIVATE;
            case INVERSE -> ActionTypeAvro.INVERSE;
            case SET_VALUE -> ActionTypeAvro.SET_VALUE;
            case UNRECOGNIZED -> throw new IllegalArgumentException(
                    "Unknown action type: " + actionType);
        };
    }

    private Instant convertToInstant(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }
}
package ru.yandex.practicum.collector.models;

import ru.yandex.practicum.collector.models.hubEvents.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.util.List;

public class HubEventMapper {

    private HubEventMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static HubEventAvro toAvro(HubEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("HubEvent cannot be null");
        }

        return HubEventAvro.newBuilder()
                .setHubId(event.getHubId())
                .setTimestamp(event.getTimestamp())
                .setPayload(createPayload(event)) // будут вызываться разные перегрузки в зависимости от типа event в рантайме
                .build();
    }

    private static Object createPayload(DeviceAddedEvent event) {
        return DeviceAddedEventAvro.newBuilder()
                .setId(event.getId())
                .setType(convertDeviceType(event.getDeviceType()))
                .build();
    }

    private static Object createPayload(DeviceRemovedEvent event) {
        return DeviceRemovedEventAvro.newBuilder()
                .setId(event.getId())
                .build();
    }

    private static Object createPayload(ScenarioAddedEvent event) {
        List<ScenarioConditionAvro> conditions = event.getConditions().stream()
                .map(HubEventMapper::convertCondition)
                .toList();

        List<DeviceActionAvro> actions = event.getActions().stream()
                .map(HubEventMapper::convertAction)
                .toList();

        return ScenarioAddedEventAvro.newBuilder()
                .setName(event.getName())
                .setConditions(conditions)
                .setActions(actions)
                .build();
    }

    private static Object createPayload(ScenarioRemovedEvent event) {
        return ScenarioRemovedEventAvro.newBuilder()
                .setName(event.getName())
                .build();
    }

    private static Object createPayload(HubEvent event) {
        throw new IllegalArgumentException(
                "Unsupported hub event type: " + event.getClass().getSimpleName()
        );
    }


    private static ScenarioConditionAvro convertCondition(ScenarioCondition condition) {
        return ScenarioConditionAvro.newBuilder()
                .setSensorId(condition.getSensorId())
                .setType(convertConditionType(condition.getType()))
                .setOperation(convertConditionOperation(condition.getOperation()))
                .setValue(condition.getValue())
                .build();
    }

    private static DeviceActionAvro convertAction(DeviceAction action) {
        return DeviceActionAvro.newBuilder()
                .setSensorId(action.getSensorId())
                .setType(convertActionType(action.getType()))
                .setValue(action.getValue())
                .build();
    }


    private static DeviceTypeAvro convertDeviceType(DeviceType deviceType) {
        return switch (deviceType) {
            case MOTION_SENSOR -> DeviceTypeAvro.MOTION_SENSOR;
            case TEMPERATURE_SENSOR -> DeviceTypeAvro.TEMPERATURE_SENSOR;
            case LIGHT_SENSOR -> DeviceTypeAvro.LIGHT_SENSOR;
            case CLIMATE_SENSOR -> DeviceTypeAvro.CLIMATE_SENSOR;
            case SWITCH_SENSOR -> DeviceTypeAvro.SWITCH_SENSOR;
        };
    }

    private static ConditionTypeAvro convertConditionType(ConditionType conditionType) {
        return switch (conditionType) {
            case MOTION -> ConditionTypeAvro.MOTION;
            case LUMINOSITY -> ConditionTypeAvro.LUMINOSITY;
            case SWITCH -> ConditionTypeAvro.SWITCH;
            case TEMPERATURE -> ConditionTypeAvro.TEMPERATURE;
            case CO2LEVEL -> ConditionTypeAvro.CO2LEVEL;
            case HUMIDITY -> ConditionTypeAvro.HUMIDITY;
        };
    }

    private static ConditionOperationAvro convertConditionOperation(ConditionOperation operation) {
        return switch (operation) {
            case EQUALS -> ConditionOperationAvro.EQUALS;
            case GREATER_THAN -> ConditionOperationAvro.GREATER_THAN;
            case LOWER_THAN -> ConditionOperationAvro.LOWER_THAN;
        };
    }

    private static ActionTypeAvro convertActionType(ActionType actionType) {
        return switch (actionType) {
            case ACTIVATE -> ActionTypeAvro.ACTIVATE;
            case DEACTIVATE -> ActionTypeAvro.DEACTIVATE;
            case INVERSE -> ActionTypeAvro.INVERSE;
            case SET_VALUE -> ActionTypeAvro.SET_VALUE;
        };
    }
}

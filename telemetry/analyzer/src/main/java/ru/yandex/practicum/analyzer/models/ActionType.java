package ru.yandex.practicum.analyzer.models;

import lombok.extern.slf4j.Slf4j;
import ru.yandex.practicum.kafka.telemetry.event.ActionTypeAvro;

@Slf4j
public enum ActionType {
    ACTIVATE,
    DEACTIVATE,
    INVERSE,
    SET_VALUE;

    public static ActionType fromAvro(ActionTypeAvro avroType) {
        if (avroType == null) {
            return null;
        }

        try {
            return ActionType.valueOf(avroType.name());
        } catch (IllegalArgumentException e) {
            log.error("Unknown action type: {}", avroType);
            throw new IllegalArgumentException("Unknown action type: " + avroType, e);
        }
    }

    public ActionTypeAvro toAvro() {
        return ActionTypeAvro.valueOf(this.name());
    }

}
package ru.yandex.practicum.analyzer.models;

import lombok.extern.slf4j.Slf4j;
import ru.yandex.practicum.kafka.telemetry.event.ConditionOperationAvro;

@Slf4j
public enum ConditionOperation {
    EQUALS,
    GREATER_THAN,
    LOWER_THAN;


    public static ConditionOperation fromAvro(ConditionOperationAvro avroOperation) {
        if (avroOperation == null) {
            return null;
        }

        try {
            return ConditionOperation.valueOf(avroOperation.name());
        } catch (IllegalArgumentException e) {
            log.error("Unknown condition operation: {}", avroOperation);
            throw new IllegalArgumentException("Unknown condition operation: " + avroOperation, e);
        }
    }

    public ConditionOperationAvro toAvro() {
        return ConditionOperationAvro.valueOf(this.name());
    }

}

package ru.yandex.practicum.analyzer.models;

import lombok.extern.slf4j.Slf4j;
import ru.yandex.practicum.kafka.telemetry.event.ConditionTypeAvro;

@Slf4j
public enum ConditionType {
    MOTION,
    LUMINOSITY,
    SWITCH,
    TEMPERATURE,
    CO2LEVEL,
    HUMIDITY;

    public static ConditionType fromAvro(ConditionTypeAvro avroType) {
        if (avroType == null) {
            return null;
        }

        try {
            return ConditionType.valueOf(avroType.name());
        } catch (IllegalArgumentException e) {
            log.error("Unknown condition type: {}", avroType);
            throw new IllegalArgumentException("Unknown condition type: " + avroType, e);
        }
    }

    public ConditionTypeAvro toAvro() {
        return ConditionTypeAvro.valueOf(this.name());
    }
}
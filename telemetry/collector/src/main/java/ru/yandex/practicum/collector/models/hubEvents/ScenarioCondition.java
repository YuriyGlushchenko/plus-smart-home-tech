package ru.yandex.practicum.collector.models.hubEvents;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScenarioCondition {
    private String sensorId;

    private ConditionType type;

    private ConditionOperation operation;

    private Integer value;
}

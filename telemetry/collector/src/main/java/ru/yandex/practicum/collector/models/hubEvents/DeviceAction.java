package ru.yandex.practicum.collector.models.hubEvents;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeviceAction {
    private String sensorId;

    private ActionType type;

    private Integer value; // необязательное поле по спецификации -> Integer

}

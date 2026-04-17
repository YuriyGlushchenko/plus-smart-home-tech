package ru.yandex.practicum.collector.models.hubEvents;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeviceRemovedEvent extends HubEvent {

    @NotBlank
    private String id;

    @Override
    public HubEventType getType() {
        return HubEventType.DEVICE_REMOVED;
    }


}

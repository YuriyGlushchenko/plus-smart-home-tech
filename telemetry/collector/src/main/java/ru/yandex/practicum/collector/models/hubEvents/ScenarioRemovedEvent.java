package ru.yandex.practicum.collector.models.hubEvents;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScenarioRemovedEvent extends HubEvent {

    @Size(min = 3, max = 2147483647)
    @NotBlank
    private String name;

    @Override
    public HubEventType getType() {
        return HubEventType.SCENARIO_REMOVED;
    }


}

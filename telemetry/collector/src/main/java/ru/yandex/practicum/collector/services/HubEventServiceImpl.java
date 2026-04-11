package ru.yandex.practicum.collector.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.collector.KafkaEventProducer;
import ru.yandex.practicum.collector.models.HubEventMapper;
import ru.yandex.practicum.collector.models.hubEvents.HubEvent;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

@Service
@RequiredArgsConstructor
@Slf4j
public class HubEventServiceImpl implements HubEventService {

    private final KafkaEventProducer kafkaEventProducer;

    @Override
    public void processHubEvent(HubEvent event) {


        HubEventAvro avroHubEvent = HubEventMapper.toAvro(event);

        kafkaEventProducer.sendHubEvent(avroHubEvent)
                .thenAccept(metadata -> {
                    log.info("Sensor event sent successfully: hubId={}, offset={}, partition={}",
                            event.getHubId(), metadata.offset(), metadata.partition());
                })
                .exceptionally(exception -> {
                    log.error("Failed to send sensor event:  hubId={}, type={}",
                            event.getHubId(), event.getType(), exception);
                    return null;
                });
    }
}

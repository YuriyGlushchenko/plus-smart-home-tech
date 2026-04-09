package ru.yandex.practicum.collector.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.collector.KafkaTopics;
import ru.yandex.practicum.collector.models.HubEventMapper;
import ru.yandex.practicum.collector.models.hubEvents.HubEvent;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

@Service
@RequiredArgsConstructor
@Slf4j
public class HubEventServiceImpl implements HubEventService {

    private final Producer<String, SpecificRecordBase> producer;

    @Override
    public void processHubEvent(HubEvent event) {
        if (event == null) {
            log.warn("Получен null event");
            return;
        }

        HubEventAvro avroHubEvent = HubEventMapper.toAvro(event);

        ProducerRecord<String, SpecificRecordBase> record = new ProducerRecord<>(KafkaTopics.HUBS_TOPIC,
                event.getHubId(),
                avroHubEvent);

        producer.send(record);
    }
}

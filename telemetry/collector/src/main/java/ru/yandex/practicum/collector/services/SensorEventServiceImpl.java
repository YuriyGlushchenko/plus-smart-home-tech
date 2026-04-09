package ru.yandex.practicum.collector.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.collector.KafkaTopics;
import ru.yandex.practicum.collector.models.SensorEventMapper;
import ru.yandex.practicum.collector.models.sensorEvents.SensorEvent;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

@Service
@RequiredArgsConstructor
@Slf4j
public class SensorEventServiceImpl implements SensorEventService {

    private final Producer<String, SpecificRecordBase> producer;

    @Override
    public void processSensorEvent(SensorEvent event) {
        if (event == null) {
            log.warn("Получен null event");
            return;
        }

        SensorEventAvro avroSensorEvent = SensorEventMapper.toAvro(event);

        ProducerRecord<String, SpecificRecordBase> record = new ProducerRecord<>(KafkaTopics.SENSORS_TOPIC,
                event.getId(),
                avroSensorEvent);

        producer.send(record);
    }
}

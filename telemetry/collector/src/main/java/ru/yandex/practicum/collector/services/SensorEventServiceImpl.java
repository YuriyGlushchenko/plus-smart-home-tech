package ru.yandex.practicum.collector.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.collector.KafkaEventProducer;
import ru.yandex.practicum.collector.models.SensorEventMapper;
import ru.yandex.practicum.collector.models.sensorEvents.SensorEvent;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

@Service
@RequiredArgsConstructor
@Slf4j
public class SensorEventServiceImpl implements SensorEventService {

    private final KafkaEventProducer kafkaEventProducer;

    @Override
    public void processSensorEvent(SensorEvent event) {

        SensorEventAvro avroSensorEvent = SensorEventMapper.toAvro(event);

        kafkaEventProducer.sendSensorEvent(avroSensorEvent)
                .thenAccept(metadata -> {
                    log.info("Sensor event sent successfully: id={}, hubId={}, offset={}, partition={}",
                            event.getId(), event.getHubId(), metadata.offset(), metadata.partition());
                })
                .exceptionally(exception -> {
                    log.error("Failed to send sensor event: id={}, hubId={}, type={}",
                            event.getId(), event.getHubId(), event.getType(), exception);
                    return null;
                });

    }
}

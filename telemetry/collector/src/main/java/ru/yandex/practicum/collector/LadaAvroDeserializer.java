package ru.yandex.practicum.collector;

import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import ru.yandex.practicum.collector.sensorEvents.SensorEventType;
import ru.yandex.practicum.kafka.telemetry.event.ClimateSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.MotionSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.TemperatureSensorAvro;

public class LadaAvroDeserializer implements Deserializer<SpecificRecordBase> {
    private final DecoderFactory decoderFactory = DecoderFactory.get();

    @Override
    public SpecificRecordBase deserialize(String topic, byte[] bytes) {
        try {
            if (bytes != null) {
                BinaryDecoder decoder = decoderFactory.binaryDecoder(bytes, null);
                DatumReader<SpecificRecordBase> reader;

                switch (topic) {
                    case SensorEventType.LIGHT_SENSOR_EVENT:
                        reader = new SpecificDatumReader<>(ClimateSensorAvro.getClassSchema());
                        break;
                    case SensorEventType.MOTION_SENSOR_EVENT:
                        reader = new SpecificDatumReader<>(MotionSensorAvro.getClassSchema());
                        break;
                    case SensorEventType.TEMPERATURE_SENSOR_EVENT:
                        reader = new SpecificDatumReader<>(TemperatureSensorAvro.getClassSchema());
                        break;
                    default:
                        throw new IllegalArgumentException("Неизвестный топик: " + topic);
                }

                return reader.read(null, decoder);
            }
            return null;
        } catch (Exception e) {
            throw new SerializationException("Ошибка десереализации данных из топика [" + topic + "]", e);
        }
    }
}

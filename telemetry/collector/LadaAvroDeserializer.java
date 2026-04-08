package ru.practicum.lada;

import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import ru.practicum.avro_example.CheckOkEventAvro;
import ru.practicum.avro_example.ECUCheckEventAvro;
import ru.practicum.avro_example.LightsEventAvro;

public class LadaAvroDeserializer implements Deserializer<SpecificRecordBase> {
    private final DecoderFactory decoderFactory = DecoderFactory.get();

    @Override
    public SpecificRecordBase deserialize(String topic, byte[] bytes) {
        try {
            if (bytes != null) {
                BinaryDecoder decoder = decoderFactory.binaryDecoder(bytes, null);
                DatumReader<SpecificRecordBase> reader;

                switch (topic) {
                    case LadaTopics.ECU_CHECK_REQUESTS_TOPIC:
                        reader = new SpecificDatumReader<>(ECUCheckEventAvro.getClassSchema());
                        break;
                    case LadaTopics.ECU_CHECK_RESPONSES_TOPIC:
                        reader = new SpecificDatumReader<>(CheckOkEventAvro.getClassSchema());
                        break;
                    case LadaTopics.LIGHTS_EVENTS_TOPIC:
                        reader = new SpecificDatumReader<>(LightsEventAvro.getClassSchema());
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

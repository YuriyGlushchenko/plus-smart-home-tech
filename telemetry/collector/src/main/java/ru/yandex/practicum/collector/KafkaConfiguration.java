package ru.yandex.practicum.collector;

import lombok.Getter;
import lombok.Setter;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Getter
@Setter
@Configuration
public class KafkaConfiguration {


    @Value("${kafka.bootstrap-server:localhost:9092}")
    private String bootstrapServer;

    @Value("${kafka.topics.sensors}")
    private String sensorsTopic;

    @Value("${kafka.topics.hubs}")
    private String hubsTopic;

    @Value("${kafka.producer.key-serializer}")
    private String keySerializer;

    @Value("${kafka.producer.value-serializer}")
    private String valueSerializer;

    @Bean
    public Producer<String, SpecificRecordBase> kafkaProducer() {
        Properties config = new Properties();

        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializer);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializer);

        return new KafkaProducer<>(config);
    }
}
package ru.yandex.practicum.aggregator.config;

import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Properties;

@Configuration
@RequiredArgsConstructor
public class KafkaConfiguration {

    private final KafkaProperties kafkaProps;

    @Bean
    public Producer<String, SpecificRecordBase> kafkaProducer() {
        Properties config = new Properties();

        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProps.getBootstrapServer());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, kafkaProps.getProducer().getKeySerializer());
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, kafkaProps.getProducer().getValueSerializer());

        return new KafkaProducer<>(config);
    }

    @Bean
    public KafkaConsumer<String, SpecificRecordBase> kafkaConsumer() {
        Properties config = new Properties();

        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProps.getBootstrapServer());
        config.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaProps.getConsumer().getGroupId());
        config.put(ConsumerConfig.CLIENT_ID_CONFIG, kafkaProps.getConsumer().getClientId());
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, kafkaProps.getConsumer().getKeyDeserializer());
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, kafkaProps.getConsumer().getValueDeserializer());
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, kafkaProps.getConsumer().getMaxPollRecords());
        config.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, kafkaProps.getConsumer().getFetchMaxBytes());
        config.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, kafkaProps.getConsumer().getMaxPartitionFetchBytes());

        KafkaConsumer<String, SpecificRecordBase> consumer = new KafkaConsumer<>(config);
        List<String> topics = List.of(kafkaProps.getTopics().getSensors());
        consumer.subscribe(topics);

        return consumer;
    }
}
package ru.yandex.practicum.aggregator.config;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Properties;

@Data
@RequiredArgsConstructor
//@Component
@ConfigurationProperties("kafka")
public class KafkaProps {
    private final ProducerConfig producer;
    private final ConsumerConfig consumer;

    @Setter
    @Getter
    @AllArgsConstructor
    public static class ProducerConfig {
        private String topic;
        private Properties properties;
    }

    @Setter
    @Getter
    @AllArgsConstructor
    public static class ConsumerConfig {
        private String topic;
        private Duration pollTimeout;
        private Properties properties;
    }
}
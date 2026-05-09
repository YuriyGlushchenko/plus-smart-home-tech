package ru.yandex.practicum.analyzer;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import ru.yandex.practicum.analyzer.config.KafkaProps;
import ru.yandex.practicum.analyzer.processors.HubEventProcessor;
import ru.yandex.practicum.analyzer.processors.SnapshotProcessor;

@SpringBootApplication
@EnableConfigurationProperties(KafkaProps.class)
public class AnalyzerApp {
    public static void main(String[] args) {
        ConfigurableApplicationContext context =
                SpringApplication.run(AnalyzerApp.class, args);

        final HubEventProcessor hubEventProcessor = context.getBean(HubEventProcessor.class);
        SnapshotProcessor snapshotProcessor = context.getBean(SnapshotProcessor.class);

        // запускаем в отдельном потоке обработчик событий
        // от пользовательских хабов
        Thread hubEventsThread = new Thread(hubEventProcessor);
        hubEventsThread.setName("HubEventHandlerThread");
        hubEventsThread.start();

        // В текущем потоке начинаем обработку
        // снимков состояния датчиков
        snapshotProcessor.start();
    }
}

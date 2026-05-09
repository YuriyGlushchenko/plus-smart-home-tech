package ru.yandex.practicum.collector.services;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.yandex.practicum.collector.KafkaEventProducer;
import ru.yandex.practicum.collector.models.HubEventMapperProtoToAvro;
import ru.yandex.practicum.collector.models.SensorEventMapperProtoToAvro;
import ru.yandex.practicum.grpc.telemetry.collector.CollectorControllerGrpc;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class EventGrpcController extends CollectorControllerGrpc.CollectorControllerImplBase {

    private final KafkaEventProducer kafkaEventProducer;
    private final SensorEventMapperProtoToAvro sensorEventMapper;
    private final HubEventMapperProtoToAvro hubEventMapper;

    @Override
    public void collectSensorEvent(SensorEventProto request, StreamObserver<Empty> responseObserver) {
        try {
            SensorEventAvro avroEvent = sensorEventMapper.toAvro(request);

            kafkaEventProducer.sendSensorEvent(avroEvent)
                    .thenAccept(metadata -> {
                        log.info("Sensor event sent: id={}, hubId={}, offset={}",
                                avroEvent.getId(), avroEvent.getHubId(), metadata.offset());
                        responseObserver.onNext(Empty.getDefaultInstance());
                        responseObserver.onCompleted();
                    })
                    .exceptionally(exception -> {
                        log.error("Failed to send sensor event", exception);
                        responseObserver.onError(
                                new StatusRuntimeException(Status.INTERNAL.withCause(exception))
                        );
                        return null;
                    });

        } catch (Exception e) {
            responseObserver.onError(
                    new StatusRuntimeException(Status.INVALID_ARGUMENT.withCause(e))
            );
        }
    }

    @Override
    public void collectHubEvent(HubEventProto request, StreamObserver<Empty> responseObserver) {
        try {
            HubEventAvro avroEvent = hubEventMapper.toAvro(request);

            kafkaEventProducer.sendHubEvent(avroEvent)
                    .thenAccept(metadata -> {
                        log.info("Hub event sent: hubId={}, offset={}", avroEvent.getHubId(), metadata.offset());
                        responseObserver.onNext(Empty.getDefaultInstance());
                        responseObserver.onCompleted();
                    })
                    .exceptionally(exception -> {
                        log.error("Failed to send hub event", exception);
                        responseObserver.onError(
                                new StatusRuntimeException(Status.INTERNAL.withCause(exception))
                        );
                        return null;
                    });

        } catch (Exception e) {
            responseObserver.onError(
                    new StatusRuntimeException(Status.INVALID_ARGUMENT.withCause(e))
            );
        }
    }
}
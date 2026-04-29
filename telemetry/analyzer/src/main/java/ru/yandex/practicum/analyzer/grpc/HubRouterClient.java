package ru.yandex.practicum.analyzer.grpc;

import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.analyzer.models.Action;
import ru.yandex.practicum.analyzer.models.ActionType;
import ru.yandex.practicum.analyzer.models.ScenarioAction;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequest;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;

import java.time.Instant;

@Component
@Slf4j
public class HubRouterClient {

    @GrpcClient("hub-router")
    private HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient;

    public DeviceActionRequest createActionRequest(String hubId, String scenarioName,
                                                   ScenarioAction scenarioAction) {
        Action action = scenarioAction.getAction();
        String sensorId = scenarioAction.getSensor().getId();

        DeviceActionProto.Builder actionBuilder = DeviceActionProto.newBuilder()
                .setSensorId(sensorId)
                .setType(convertActionType(action.getType()));

        if (action.getValue() != null) {
            actionBuilder.setValue(action.getValue());
        }

        return DeviceActionRequest.newBuilder()
                .setHubId(hubId)
                .setScenarioName(scenarioName)
                .setTimestamp(com.google.protobuf.Timestamp.newBuilder()
                        .setSeconds(Instant.now().getEpochSecond())
                        .setNanos(Instant.now().getNano())
                        .build())
                .setAction(actionBuilder.build())
                .build();
    }

    public void executeAction(DeviceActionRequest action) {
        try {
            hubRouterClient.handleDeviceAction(action);
            log.debug("Выполнено действие: hub={}, scenario={}, sensor={}, type={}",
                    action.getHubId(),
                    action.getScenarioName(),
                    action.getAction().getSensorId(),
                    action.getAction().getType());
        } catch (StatusRuntimeException e) {
            log.error("gRPC ошибка при выполнении действия для хаба {}: status={}, message={}",
                    action.getHubId(), e.getStatus().getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Ошибка при выполнении действия для хаба {}: {}",
                    action.getHubId(), e.getMessage());
        }
    }

    private ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto convertActionType(ActionType type) {
        return switch (type) {
            case ACTIVATE -> ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto.ACTIVATE;
            case DEACTIVATE -> ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto.DEACTIVATE;
            case INVERSE -> ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto.INVERSE;
            case SET_VALUE -> ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto.SET_VALUE;
        };
    }
}
package ru.yandex.practicum.delivery.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.delivery.client.OrderClient;
import ru.yandex.practicum.delivery.client.WarehouseClient;
import ru.yandex.practicum.delivery.mapper.DeliveryMapper;
import ru.yandex.practicum.delivery.model.Delivery;
import ru.yandex.practicum.delivery.model.DeliveryState;
import ru.yandex.practicum.delivery.repository.DeliveryRepository;
import ru.yandex.practicum.dto.AddressDto;
import ru.yandex.practicum.dto.DeliveryDto;
import ru.yandex.practicum.dto.OrderDto;
import ru.yandex.practicum.dto.ShippedToDeliveryRequest;
import ru.yandex.practicum.exceptions.exceptions.NoDeliveryFoundException;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryMapper deliveryMapper;
    private final OrderClient orderClient;
    private final WarehouseClient warehouseClient;

    @Override
    @Transactional
    public DeliveryDto planDelivery(DeliveryDto deliveryDto) {
        log.debug("Создание доставки для заказа: {}", deliveryDto.getOrderId());

        if (deliveryRepository.findByOrderId(deliveryDto.getOrderId()).isPresent()) {
            log.warn("Доставка для заказа {} уже существует", deliveryDto.getOrderId());
            throw new IllegalStateException("Доставка для этого заказа уже создана");
        }

        Delivery delivery = Delivery.builder()
                .orderId(deliveryDto.getOrderId())
                .fromAddress(deliveryMapper.toAddress(deliveryDto.getFromAddress()))
                .toAddress(deliveryMapper.toAddress(deliveryDto.getToAddress()))
                .deliveryWeight(deliveryDto.getDeliveryWeight())
                .deliveryVolume(deliveryDto.getDeliveryVolume())
                .fragile(deliveryDto.getFragile())
                .state(DeliveryState.CREATED)
                .build();

        Delivery savedDelivery = deliveryRepository.save(delivery);
        log.debug("Доставка создана с id: {} для заказа: {}", savedDelivery.getId(), deliveryDto.getOrderId());

        return deliveryMapper.toDto(savedDelivery);
    }

    @Override
    public Double deliveryCost(OrderDto orderDto, AddressDto toAddress) {
        log.debug("Расчёт стоимости доставки: from={}, to={}, weight={}, volume={}, fragile={}",
                warehouseClient.getWarehouseAddress().getStreet(),
                toAddress.getStreet(),
                orderDto.getDeliveryWeight(),
                orderDto.getDeliveryVolume(),
                orderDto.getFragile());

        // Получаем адрес склада
        AddressDto fromAddress = warehouseClient.getWarehouseAddress();

        BigDecimal cost = BigDecimal.valueOf(5.0);

        // коэффициент склада
        BigDecimal addressMultiplier = getAddressMultiplier(fromAddress);
        cost = cost.add(cost.multiply(addressMultiplier));
        log.trace("После учёта адреса склада (множитель {}): {}", addressMultiplier, cost);

        // коэфф. хрупкости
        if (orderDto.getFragile() != null && orderDto.getFragile()) {
            BigDecimal fragileAddition = cost.multiply(BigDecimal.valueOf(0.2));
            cost = cost.add(fragileAddition);
            log.trace("После учёта хрупкости (+{}): {}", fragileAddition, cost);
        }

        // коэффициент веса
        if (orderDto.getDeliveryWeight() != null) {
            BigDecimal weightAddition = BigDecimal.valueOf(orderDto.getDeliveryWeight())
                    .multiply(BigDecimal.valueOf(0.3));
            cost = cost.add(weightAddition);
            log.trace("После учёта веса (+{}): {}", weightAddition, cost);
        }

        // коэффициент объема
        if (orderDto.getDeliveryVolume() != null) {
            BigDecimal volumeAddition = BigDecimal.valueOf(orderDto.getDeliveryVolume())
                    .multiply(BigDecimal.valueOf(0.2));
            cost = cost.add(volumeAddition);
            log.trace("После учёта объёма (+{}): {}", volumeAddition, cost);
        }

        // коэффициент совпадения адреса
        if (!isSameStreet(fromAddress, toAddress)) {
            BigDecimal streetAddition = cost.multiply(BigDecimal.valueOf(0.2));
            cost = cost.add(streetAddition);
            log.trace("После учёта адреса доставки (улица не совпадает, +{}): {}", streetAddition, cost);
        }

        double result = cost.doubleValue();
        log.debug("Итоговая стоимость доставки: {}", result);
        return result;
    }

    @Override
    @Transactional
    public void deliveryPicked(UUID orderId) {
        log.debug("Приём товаров в доставку для заказа: {}", orderId);

        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NoDeliveryFoundException("Доставка для заказа " + orderId + " не найдена"));

        if (delivery.getState() != DeliveryState.CREATED) {
            log.warn("Доставка для заказа {} не в статусе CREATED, текущий статус: {}", orderId, delivery.getState());
        }

        delivery.setState(DeliveryState.IN_PROGRESS);
        deliveryRepository.save(delivery);
        log.trace("Статус доставки для заказа {} изменён на IN_PROGRESS", orderId);

        try {
            orderClient.assembly(orderId);
            log.debug("Обновление статуса заказа на  ASSEMBLED, заказ: {}", orderId);
        } catch (Exception e) {
            log.error("Не удалось обновить статус заказа на  ASSEMBLED, заказ: {}, {}",orderId,  e.getMessage());
        }

        try {
            ShippedToDeliveryRequest request = ShippedToDeliveryRequest.builder()
                    .orderId(orderId)
                    .deliveryId(delivery.getId())
                    .build();
            warehouseClient.shippedToDelivery(request);
            log.debug("Склад уведомлён о передаче товаров в доставку для заказа {}", orderId);
        } catch (Exception e) {
            log.error("Не удалось уведомить склад о передаче товаров в доставку: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deliverySuccessful(UUID orderId) {
        log.debug("Успешная доставка внешней системой доставки для заказа: {}", orderId);

        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NoDeliveryFoundException("Доставка для заказа " + orderId + " не найдена"));

        if (delivery.getState() != DeliveryState.IN_PROGRESS) {
            log.warn("Доставка для заказа {} не в статусе IN_PROGRESS, текущий статус: {}", orderId, delivery.getState());
        }

        delivery.setState(DeliveryState.DELIVERED);
        deliveryRepository.save(delivery);
        log.trace("Статус доставки для заказа {} изменён на DELIVERED", orderId);

        try {
            orderClient.delivery(orderId);
            log.trace("Сервис заказов уведомлён об успешной доставке заказа {}", orderId);
        } catch (Exception e) {
            log.error("Не удалось уведомить сервис заказов об успешной доставке: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deliveryFailed(UUID orderId) {
        log.debug("Ошибка доставки от внешней системы доставки для заказа: {}", orderId);

        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NoDeliveryFoundException("Доставка для заказа " + orderId + " не найдена"));

        if (delivery.getState() != DeliveryState.IN_PROGRESS) {
            log.warn("Доставка для заказа {} не в статусе IN_PROGRESS, текущий статус: {}", orderId, delivery.getState());
        }

        delivery.setState(DeliveryState.FAILED);
        deliveryRepository.save(delivery);
        log.trace("Статус доставки для заказа {} изменён на FAILED", orderId);

        try {
            orderClient.deliveryFailed(orderId);
            log.debug("Сервис заказов уведомлён об ошибке доставки заказа {}", orderId);
        } catch (Exception e) {
            log.error("Не удалось уведомить сервис заказов об ошибке доставки: {}", e.getMessage());
        }
    }

    private BigDecimal getAddressMultiplier(AddressDto address) {
        if (address == null || address.getStreet() == null) {
            return BigDecimal.ONE;
        }
        return address.getStreet().contains("ADDRESS_2") ? BigDecimal.valueOf(2.0) : BigDecimal.ONE;
    }

    private boolean isSameStreet(AddressDto fromAddress, AddressDto toAddress) {
        if (fromAddress == null || toAddress == null) {
            return false;
        }
        if (fromAddress.getStreet() == null || toAddress.getStreet() == null) {
            return false;
        }
        return fromAddress.getStreet().equals(toAddress.getStreet());
    }
}
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
import ru.yandex.practicum.dto.ShippedToDeliveryRequest;
import ru.yandex.practicum.exceptions.exceptions.NoDeliveryFoundException;

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
    public DeliveryDto planDelivery(AddressDto fromAddress, AddressDto toAddress, UUID orderId,
                                    Double deliveryWeight, Double deliveryVolume, Boolean fragile) {
        log.debug("Планирование доставки для заказа: {}", orderId);

        // Проверяем, нет ли уже доставки для этого заказа
        if (deliveryRepository.findByOrderId(orderId).isPresent()) {
            log.warn("Доставка для заказа {} уже существует", orderId);
            throw new IllegalStateException("Доставка для этого заказа уже создана");
        }

        // Создаём доставку
        Delivery delivery = Delivery.builder()
                .orderId(orderId)
                .fromAddress(deliveryMapper.toAddress(fromAddress))
                .toAddress(deliveryMapper.toAddress(toAddress))
                .deliveryWeight(deliveryWeight)
                .deliveryVolume(deliveryVolume)
                .fragile(fragile)
                .state(DeliveryState.CREATED)
                .build();

        Delivery savedDelivery = deliveryRepository.save(delivery);
        log.debug("Доставка создана с id: {} для заказа: {}", savedDelivery.getId(), orderId);

        return deliveryMapper.toDto(savedDelivery);
    }

    @Override
    public Double deliveryCost(AddressDto fromAddress, AddressDto toAddress,
                               Double weight, Double volume, Boolean fragile) {
        log.debug("Расчёт стоимости доставки: from={}, to={}, weight={}, volume={}, fragile={}",
                fromAddress.getStreet(), toAddress.getStreet(), weight, volume, fragile);

        // Базовая стоимость
        double cost = 5.0;
        log.debug("Базовая стоимость: {}", cost);

        // Умножаем базовую стоимость на число, зависящее от адреса склада
        double addressMultiplier = getAddressMultiplier(fromAddress);
        cost = cost + (cost * addressMultiplier);
        log.debug("После учёта адреса склада (множитель {}): {}", addressMultiplier, cost);

        // Если есть признак хрупкости
        if (fragile != null && fragile) {
            double fragileAddition = cost * 0.2;
            cost = cost + fragileAddition;
            log.debug("После учёта хрупкости (+{}): {}", fragileAddition, cost);
        }

        // Добавляем вес, умноженный на 0.3
        double weightAddition = weight * 0.3;
        cost = cost + weightAddition;
        log.debug("После учёта веса (+{}): {}", weightAddition, cost);

        // Добавляем объём, умноженный на 0.2
        double volumeAddition = volume * 0.2;
        cost = cost + volumeAddition;
        log.debug("После учёта объёма (+{}): {}", volumeAddition, cost);

        // Учёт адреса доставки (если улица не совпадает)
        if (!isSameStreet(fromAddress, toAddress)) {
            double streetAddition = cost * 0.2;
            cost = cost + streetAddition;
            log.debug("После учёта адреса доставки (улица не совпадает, +{}): {}", streetAddition, cost);
        }

        log.debug("Итоговая стоимость доставки: {}", cost);
        return cost;
    }

    @Override
    @Transactional
    public void deliveryPicked(UUID orderId) {
        log.debug("Приём товаров в доставку для заказа: {}", orderId);

        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NoDeliveryFoundException("Доставка для заказа " + orderId + " не найдена"));

        if (delivery.getState() != DeliveryState.CREATED) {
            log.warn("Доставка для заказа {} не в статусе CREATED, текущий статус: {}", orderId, delivery.getState());
            throw new IllegalStateException("Доставка не может быть начата в текущем статусе");
        }

        delivery.setState(DeliveryState.IN_PROGRESS);
        deliveryRepository.save(delivery);
        log.debug("Статус доставки для заказа {} изменён на IN_PROGRESS", orderId);

        // Уведомляем склад о передаче товаров в доставку
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
        log.debug("Успешная доставка для заказа: {}", orderId);

        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NoDeliveryFoundException("Доставка для заказа " + orderId + " не найдена"));

        if (delivery.getState() != DeliveryState.IN_PROGRESS) {
            log.warn("Доставка для заказа {} не в статусе IN_PROGRESS, текущий статус: {}", orderId, delivery.getState());
            throw new IllegalStateException("Доставка не может быть завершена в текущем статусе");
        }

        delivery.setState(DeliveryState.DELIVERED);
        deliveryRepository.save(delivery);
        log.debug("Статус доставки для заказа {} изменён на DELIVERED", orderId);

        // Уведомляем сервис заказов об успешной доставке
        try {
            orderClient.delivery(orderId);
            log.debug("Сервис заказов уведомлён об успешной доставке заказа {}", orderId);
        } catch (Exception e) {
            log.error("Не удалось уведомить сервис заказов об успешной доставке: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deliveryFailed(UUID orderId) {
        log.debug("Ошибка доставки для заказа: {}", orderId);

        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NoDeliveryFoundException("Доставка для заказа " + orderId + " не найдена"));

        if (delivery.getState() != DeliveryState.IN_PROGRESS) {
            log.warn("Доставка для заказа {} не в статусе IN_PROGRESS, текущий статус: {}", orderId, delivery.getState());
            throw new IllegalStateException("Доставка не может быть завершена с ошибкой в текущем статусе");
        }

        delivery.setState(DeliveryState.FAILED);
        deliveryRepository.save(delivery);
        log.debug("Статус доставки для заказа {} изменён на FAILED", orderId);

        // Уведомляем сервис заказов об ошибке доставки
        try {
            orderClient.deliveryFailed(orderId);
            log.debug("Сервис заказов уведомлён об ошибке доставки заказа {}", orderId);
        } catch (Exception e) {
            log.error("Не удалось уведомить сервис заказов об ошибке доставки: {}", e.getMessage());
        }
    }

    // ========== Private методы ==========

    private double getAddressMultiplier(AddressDto address) {
        if (address == null || address.getStreet() == null) {
            return 1.0;
        }
        if (address.getStreet().contains("ADDRESS_2")) {
            return 2.0;
        }
        return 1.0;
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
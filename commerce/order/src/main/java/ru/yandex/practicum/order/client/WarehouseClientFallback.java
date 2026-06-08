package ru.yandex.practicum.order.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.dto.*;

import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class WarehouseClientFallback implements WarehouseClient {

    @Override
    public BookedProductsDto checkProductQuantityEnoughForShoppingCart(ShoppingCartDto shoppingCart) {
        log.warn("Сервис warehouse недоступен. Возвращаем значения по умолчанию для корзины: {}",
                shoppingCart.getShoppingCartId());

        return BookedProductsDto.builder()
                .deliveryWeight(0.0)
                .deliveryVolume(0.0)
                .fragile(false)
                .build();
    }

    @Override
    public void newProductInWarehouse(NewProductInWarehouseRequest request) {
        log.warn("newProductInWarehouse - Сервис warehouse недоступен");
        throw new RuntimeException("Сервис склада временно недоступен");
    }

    @Override
    public void addProductToWarehouse(AddProductToWarehouseRequest request) {
        log.warn("addProductToWarehouse - Сервис warehouse недоступен");
        throw new RuntimeException("Сервис склада временно недоступен");
    }

    @Override
    public AddressDto getWarehouseAddress() {
        log.warn("getWarehouseAddress - Сервис warehouse недоступен");
        throw new RuntimeException("Сервис склада временно недоступен");
    }

    @Override
    public BookedProductsDto assemblyProductsForOrder(AssemblyProductsForOrderRequest request) {
        return null;
    }

    @Override
    public void shippedToDelivery(ShippedToDeliveryRequest request) {

    }

    @Override
    public void acceptReturn(Map<UUID, Integer> products) {
    }
}
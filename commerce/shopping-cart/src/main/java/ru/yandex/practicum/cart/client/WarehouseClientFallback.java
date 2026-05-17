package ru.yandex.practicum.cart.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.dto.*;

@Component  // ← Обязательно! Spring должен создать бин
@Slf4j
public class WarehouseClientFallback implements WarehouseClient {

    @Override
    public BookedProductsDto checkShoppingCart(ShoppingCartDto shoppingCart) {
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
}
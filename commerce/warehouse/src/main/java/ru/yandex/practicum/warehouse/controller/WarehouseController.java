package ru.yandex.practicum.warehouse.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.api.WarehouseApi;
import ru.yandex.practicum.dto.*;
import ru.yandex.practicum.validation.CheckCart;
import ru.yandex.practicum.warehouse.service.WarehouseService;

@RestController
@RequestMapping("/api/v1/warehouse")
@RequiredArgsConstructor
@Slf4j
@Validated
public class WarehouseController implements WarehouseApi {

    private final WarehouseService warehouseService;

    @Override
    @PutMapping
    public void newProductInWarehouse(@Valid @RequestBody NewProductInWarehouseRequest request) {
        log.debug("PUT /api/v1/warehouse - Добавление нового товара на склад");
        warehouseService.newProductInWarehouse(request);
    }

    @Override
    @PostMapping("/add")
    public void addProductToWarehouse(@Valid @RequestBody AddProductToWarehouseRequest request) {
        log.debug("POST /api/v1/warehouse/add - Приём товара на склад");
        warehouseService.addProductToWarehouse(request);
    }

    @Override
    @PostMapping("/check")
    public BookedProductsDto checkShoppingCart(
            @Validated(CheckCart.class) @RequestBody ShoppingCartDto shoppingCart) {
        log.debug("POST /api/v1/warehouse/check - Проверка наличия товаров для корзины: {}",
                shoppingCart.getShoppingCartId());
        return warehouseService.checkShoppingCart(shoppingCart);
    }

    @Override
    @GetMapping("/address")
    public AddressDto getWarehouseAddress() {
        log.debug("GET /api/v1/warehouse/address - Получение адреса склада");
        return warehouseService.getWarehouseAddress();
    }
}
package ru.yandex.practicum.warehouse.service;


import ru.yandex.practicum.dto.*;

public interface WarehouseService {

    void newProductInWarehouse(NewProductInWarehouseRequest request);

    void addProductToWarehouse(AddProductToWarehouseRequest request);

    BookedProductsDto checkProductQuantityEnoughForShoppingCart(ShoppingCartDto shoppingCart);

    AddressDto getWarehouseAddress();
}
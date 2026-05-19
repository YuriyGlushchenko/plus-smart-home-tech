package ru.yandex.practicum.warehouse.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.dto.*;
import ru.yandex.practicum.exceptions.exceptions.NoSpecifiedProductInWarehouseException;
import ru.yandex.practicum.exceptions.exceptions.ProductInShoppingCartLowQuantityInWarehouse;
import ru.yandex.practicum.exceptions.exceptions.SpecifiedProductAlreadyInWarehouseException;
import ru.yandex.practicum.warehouse.mapper.WarehouseMapper;
import ru.yandex.practicum.warehouse.model.WarehouseProduct;
import ru.yandex.practicum.warehouse.repository.WarehouseRepository;

import java.security.SecureRandom;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final WarehouseMapper warehouseMapper;

    private static final String[] ADDRESSES = new String[]{"ADDRESS_1", "ADDRESS_2"};
    private static final String CURRENT_ADDRESS = ADDRESSES[new Random().nextInt(0, ADDRESSES.length)];

    @Override
    @Transactional
    public void newProductInWarehouse(NewProductInWarehouseRequest request) {
        log.debug("Добавление нового товара на склад: {}", request.getProductId());

        if (warehouseRepository.existsByProductId(request.getProductId())) {
            throw new SpecifiedProductAlreadyInWarehouseException(
                    "Товар с id " + request.getProductId() + " уже зарегистрирован на складе");
        }

        WarehouseProduct product = warehouseMapper.toEntity(request);
        warehouseRepository.save(product);

        log.debug("Новый товар добавлен на склад: {}", request.getProductId());
    }

    @Override
    @Transactional
    public void addProductToWarehouse(AddProductToWarehouseRequest request) {
        log.debug("Пополнение товара на складе: productId={}, quantity={}", request.getProductId(), request.getQuantity());

        WarehouseProduct product = warehouseRepository.findByProductId(request.getProductId())
                .orElseThrow(() -> new NoSpecifiedProductInWarehouseException(
                        "Товар с id " + request.getProductId() + " не найден на складе"));

        product.setQuantity(product.getQuantity() + request.getQuantity());
        warehouseRepository.save(product);

        log.debug("Товар добавлен на склад: productId={}, new quantity={}", request.getProductId(), product.getQuantity());
    }

    @Override
    public BookedProductsDto checkProductQuantityEnoughForShoppingCart(ShoppingCartDto shoppingCart) {
        log.debug("Проверка наличия товаров на складе для корзины: {}", shoppingCart.getShoppingCartId());

        List<UUID> productIds = new ArrayList<>(shoppingCart.getProducts().keySet());

        List<WarehouseProduct> products = warehouseRepository.findByProductIdIn(productIds);

        Map<UUID, WarehouseProduct> productMap = products.stream()
                .collect(Collectors.toMap(WarehouseProduct::getProductId, Function.identity()));

        double totalWeight = 0.0;
        double totalVolume = 0.0;
        boolean hasFragile = false;

        for (Map.Entry<UUID, Integer> entry : shoppingCart.getProducts().entrySet()) {
            UUID productId = entry.getKey();
            Integer requestedQuantity = entry.getValue();

            WarehouseProduct product = productMap.get(productId);
            if (product == null) {
                throw new ProductInShoppingCartLowQuantityInWarehouse(
                        "Товар с id " + productId + " не найден на складе");
            }

            if (product.getQuantity() < requestedQuantity) {
                throw new ProductInShoppingCartLowQuantityInWarehouse(
                        "Товара с id " + productId + " недостаточно на складе. " +
                                "Доступно: " + product.getQuantity() + ", запрошено: " + requestedQuantity);
            }

            totalWeight += product.getWeight() * requestedQuantity;
            totalVolume += product.getWidth() * product.getHeight() * product.getDepth() * requestedQuantity;

            if (product.getFragile()) {
                hasFragile = true;
            }
        }

        log.debug("Проверка пройдена. Вес: {}, Объём: {}, Хрупкие: {}", totalWeight, totalVolume, hasFragile);

        return BookedProductsDto.builder()
                .deliveryWeight(totalWeight)
                .deliveryVolume(totalVolume)
                .fragile(hasFragile)
                .build();
    }

    @Override
    public AddressDto getWarehouseAddress() {
        log.debug("Получение адреса склада: {}", CURRENT_ADDRESS);

        return AddressDto.builder()
                .country(CURRENT_ADDRESS)
                .city(CURRENT_ADDRESS)
                .street(CURRENT_ADDRESS)
                .house(CURRENT_ADDRESS)
                .flat(CURRENT_ADDRESS)
                .build();
    }
}
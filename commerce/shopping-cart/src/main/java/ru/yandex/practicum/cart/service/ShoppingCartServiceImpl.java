package ru.yandex.practicum.cart.service;

import ru.yandex.practicum.cart.client.WarehouseClient;
import ru.yandex.practicum.dto.BookedProductsDto;
import ru.yandex.practicum.dto.ShoppingCartDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.exceptions.exceptions.CartDeactivatedException;
import ru.yandex.practicum.exceptions.exceptions.CartNotFoundException;
import ru.yandex.practicum.exceptions.exceptions.NoProductsInShoppingCartException;
import ru.yandex.practicum.cart.mapper.ShoppingCartMapper;
import ru.yandex.practicum.cart.model.CartState;
import ru.yandex.practicum.cart.model.ShoppingCart;
import ru.yandex.practicum.cart.repository.ShoppingCartRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ShoppingCartServiceImpl implements ShoppingCartService {

    private final ShoppingCartRepository cartRepository;
    private final ShoppingCartMapper cartMapper;
    private final WarehouseClient warehouseClient;

    @Override
    public ShoppingCartDto getShoppingCart(String username) {
        log.debug("Получение корзины для пользователя: {}", username);

        ShoppingCart cart = cartRepository.findByUsername(username)
                .orElseGet(() -> createNewCart(username));

        return cartMapper.toDto(cart);
    }

    @Override
    @Transactional
    public ShoppingCartDto addProductToShoppingCart(String username, Map<UUID, Integer> products) {
        log.debug("Добавление товаров в корзину пользователя: {}, товары: {}", username, products);

        ShoppingCart cart = cartRepository.findByUsername(username)
                .orElseGet(() -> createNewCart(username));

        if (cart.getState() == CartState.DEACTIVATE) {
            throw new CartDeactivatedException("Корзина деактивирована. Невозможно добавить товары.");
        }

        // проверка на складе
        ShoppingCartDto checkRequest = ShoppingCartDto.builder()
                .shoppingCartId(cart.getId())
                .products(products)
                .build();

        try {
            BookedProductsDto bookedProducts = warehouseClient.checkShoppingCart(checkRequest);
            log.debug("Проверка склада пройдена. Вес: {}, Объём: {}, Хрупкие: {}",
                    bookedProducts.getDeliveryWeight(),
                    bookedProducts.getDeliveryVolume(),
                    bookedProducts.getFragile());
        } catch (feign.FeignException e) {
            if (e.status() == 400) {
                log.error("Ошибка валидации при проверке склада: {}", e.getMessage());
                throw new IllegalArgumentException("Неверный запрос к складу");
            } else if (e.status() == 404) {
                log.error("Сервис склада не найден: {}", e.getMessage());
                throw new RuntimeException("Сервис склада временно недоступен");
            } else {
                log.error("Ошибка при проверке склада: status={}, message={}", e.status(), e.getMessage());
                throw new RuntimeException("Ошибка при проверке наличия товаров: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("Неожиданная ошибка при проверке склада: {}", e.getMessage());
            throw new RuntimeException("Невозможно проверить наличие товаров");
        }

        for (Map.Entry<UUID, Integer> entry : products.entrySet()) {
            UUID productId = entry.getKey();
            Integer quantity = entry.getValue();

            if (quantity == null || quantity <= 0) {
                log.warn("Товар {} с некорректным количеством: {}", productId, quantity);
                continue;
            }

            cart.getProducts().merge(productId, quantity, Integer::sum);
        }

        ShoppingCart savedCart = cartRepository.save(cart);
        log.debug("Товары добавлены в корзину пользователя: {}", username);

        return cartMapper.toDto(savedCart);
    }

    @Override
    @Transactional
    public ShoppingCartDto removeFromShoppingCart(String username, List<UUID> productIds) {
        log.debug("Удаление товаров из корзины пользователя: {}, товары: {}", username, productIds);

        ShoppingCart cart = cartRepository.findByUsername(username)
                .orElseThrow(() -> new CartNotFoundException("Корзина пользователя " + username + " не найдена"));

        if (cart.getState() == CartState.DEACTIVATE) {
            throw new CartDeactivatedException("Корзина деактивирована. Невозможно удалить товары.");
        }

        if (cart.getProducts().isEmpty()) {
            throw new NoProductsInShoppingCartException("Корзина пользователя " + username + " уже пуста");
        }

        boolean anyRemoved = false;
        for (UUID productId : productIds) {
            if (cart.getProducts().remove(productId) != null) {
                anyRemoved = true;
            }
        }

        if (!anyRemoved) {
            throw new NoProductsInShoppingCartException("Указанные товары не найдены в корзине пользователя " + username);
        }

        ShoppingCart savedCart = cartRepository.save(cart);
        log.debug("Товары удалены из корзины пользователя: {}", username);

        return cartMapper.toDto(savedCart);
    }

    @Override
    @Transactional
    public ShoppingCartDto changeProductQuantity(String username, UUID productId, Long newQuantity) {
        log.debug("Изменение количества товара {} в корзине пользователя {} на {}", productId, username, newQuantity);

        ShoppingCart cart = cartRepository.findByUsername(username)
                .orElseThrow(() -> new CartNotFoundException("Корзина пользователя " + username + " не найдена"));

        if (cart.getState() == CartState.DEACTIVATE) {
            throw new CartDeactivatedException("Корзина деактивирована. Невозможно изменить количество товаров.");
        }

        if (!cart.getProducts().containsKey(productId)) {
            throw new NoProductsInShoppingCartException("Товар " + productId + " не найден в корзине пользователя " + username);
        }

        if (newQuantity == 0) {
            cart.getProducts().remove(productId);
            log.debug("Товар {} удалён из корзины", productId);
        } else {
            cart.getProducts().put(productId, newQuantity.intValue());
        }

        ShoppingCart savedCart = cartRepository.save(cart);
        log.debug("Количество товара {} изменено на {}", productId, newQuantity);

        return cartMapper.toDto(savedCart);
    }

    @Override
    @Transactional
    public void deactivateCurrentShoppingCart(String username) {
        log.debug("Деактивация корзины пользователя: {}", username);

        ShoppingCart cart = cartRepository.findByUsername(username)
                .orElseThrow(() -> new CartNotFoundException("Корзина пользователя " + username + " не найдена"));

        if (cart.getState() == CartState.DEACTIVATE) {
            log.warn("Корзина пользователя {} уже деактивирована", username);
            return;
        }

        cart.setState(CartState.DEACTIVATE);
        cartRepository.save(cart);
        log.debug("Корзина пользователя {} деактивирована", username);
    }

    private ShoppingCart createNewCart(String username) {
        log.debug("Создание новой корзины для пользователя: {}", username);
        ShoppingCart newCart = ShoppingCart.builder()
                .username(username)
                .state(CartState.ACTIVE)
                .build();
        return cartRepository.save(newCart);
    }
}
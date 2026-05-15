package ru.yandex.practicum.cart.service;

import dto.ShoppingCartDto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ShoppingCartService {

    ShoppingCartDto getShoppingCart(String username);

    ShoppingCartDto addProductToShoppingCart(String username, Map<UUID, Integer> products);

    ShoppingCartDto removeFromShoppingCart(String username, List<UUID> productIds);

    ShoppingCartDto changeProductQuantity(String username, UUID productId, Long newQuantity);

    void deactivateCurrentShoppingCart(String username);
}
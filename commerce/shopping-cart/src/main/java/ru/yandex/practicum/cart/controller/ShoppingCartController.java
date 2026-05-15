package ru.yandex.practicum.cart.controller;

import ru.yandex.practicum.dto.ChangeProductQuantityRequest;
import ru.yandex.practicum.dto.ShoppingCartDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.cart.service.ShoppingCartService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shopping-cart")
@RequiredArgsConstructor
public class ShoppingCartController {

    private final ShoppingCartService cartService;

    @GetMapping
    public ShoppingCartDto getShoppingCart(@RequestParam String username) {
        return cartService.getShoppingCart(username);
    }

    @PutMapping
    public ShoppingCartDto addProductToShoppingCart(@RequestParam String username, @RequestBody Map<UUID, Integer> products) {
        return cartService.addProductToShoppingCart(username, products);
    }

    @PostMapping("/remove")
    public ShoppingCartDto removeFromShoppingCart(@RequestParam String username, @RequestBody List<UUID> productIds) {
        return cartService.removeFromShoppingCart(username, productIds);
    }

    @PostMapping("/change-quantity")
    public ShoppingCartDto changeProductQuantity(
            @RequestParam String username,
            @Valid @RequestBody ChangeProductQuantityRequest request) {
        return cartService.changeProductQuantity(username, request.getProductId(), request.getNewQuantity());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.OK)
    public void deactivateCurrentShoppingCart(@RequestParam String username) {
        cartService.deactivateCurrentShoppingCart(username);
    }
}
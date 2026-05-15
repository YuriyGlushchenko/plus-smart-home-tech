package ru.yandex.practicum.cart.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.cart.model.CartState;
import ru.yandex.practicum.cart.model.ShoppingCart;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShoppingCartRepository extends JpaRepository<ShoppingCart, UUID> {

    Optional<ShoppingCart> findByUsername(String username);

    Optional<ShoppingCart> findByUsernameAndState(String username, CartState state);

    boolean existsByUsername(String username);
}
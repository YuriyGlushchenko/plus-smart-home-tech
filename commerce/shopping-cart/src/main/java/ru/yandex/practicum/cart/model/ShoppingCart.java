package ru.yandex.practicum.cart.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "shopping_carts", schema = "cart")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShoppingCart {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    @Builder.Default
    private CartState state = CartState.ACTIVE;

    // https://practicum.yandex.ru/trainer/java-developer-plus/lesson/74aa3086-3206-4021-9738-aa4510818308/
    // т.к. в данном случае список продуктов - это просто список ключей UUID, нет смысла в отдельной сущности @Entity
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "cart_products", schema = "cart", joinColumns = @JoinColumn(name = "cart_id"))
    @MapKeyColumn(name = "product_id")  // указывает на колонку, которая будет являться ключом в HashMap
    @Column(name = "quantity")  // указывает на колонку, которая будет являться значением в HashMap
    private Map<UUID, Integer> products = new HashMap<>();
}
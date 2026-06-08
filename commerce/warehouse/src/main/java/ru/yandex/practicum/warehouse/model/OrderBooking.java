package ru.yandex.practicum.warehouse.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "order_bookings", schema = "warehouse")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "delivery_id")
    private UUID deliveryId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_booking_products", schema = "warehouse",
            joinColumns = @JoinColumn(name = "order_booking_id"))
    @MapKeyColumn(name = "product_id")
    @Column(name = "quantity")
    @Builder.Default
    private Map<UUID, Integer> products = new HashMap<>();

    @CreationTimestamp
    @Column(name = "booked_at", updatable = false)
    private Instant bookedAt;
}
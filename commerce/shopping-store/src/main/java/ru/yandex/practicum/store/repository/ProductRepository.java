package ru.yandex.practicum.store.repository;

import model.ProductCategory;
import model.ProductState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.store.model.Product;

import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Page<Product> findByProductCategoryAndProductState(
            ProductCategory category,
            ProductState state,
            Pageable pageable
    );

    Page<Product> findByProductState(ProductState state, Pageable pageable);

    boolean existsByIdAndProductState(UUID id, ProductState state);
}
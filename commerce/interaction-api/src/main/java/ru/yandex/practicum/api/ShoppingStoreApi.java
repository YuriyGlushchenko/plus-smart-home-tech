package ru.yandex.practicum.api;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.ProductDto;
import ru.yandex.practicum.dto.SetProductQuantityStateRequest;
import ru.yandex.practicum.model.ProductCategory;
import ru.yandex.practicum.validation.Create;
import ru.yandex.practicum.validation.Update;

import java.util.UUID;

public interface ShoppingStoreApi {

    @GetMapping("/api/v1/shopping-store")
    Page<ProductDto> getProducts(
            @RequestParam ProductCategory category,
            @PageableDefault(size = 20, sort = "productName", direction = Sort.Direction.ASC) Pageable pageable);

    @GetMapping("/api/v1/shopping-store/{productId}")
    ProductDto getProduct(@PathVariable UUID productId);

    @PutMapping("/api/v1/shopping-store")
    ProductDto createNewProduct(@Validated(Create.class) @RequestBody ProductDto productDto);

    @PostMapping("/api/v1/shopping-store")
    ProductDto updateProduct(@Validated(Update.class) @RequestBody ProductDto productDto);

    @PostMapping("/api/v1/shopping-store/removeProductFromStore")
    boolean removeProductFromStore(@RequestBody UUID productId);

//    @PostMapping("/api/v1/shopping-store/quantityState")
//    boolean setProductQuantityState(@Valid @RequestBody SetProductQuantityStateRequest request);

    @PostMapping("/api/v1/shopping-store/quantityState")
    boolean setProductQuantityState(@RequestParam UUID productId,
                                    @RequestParam String quantityState);
}
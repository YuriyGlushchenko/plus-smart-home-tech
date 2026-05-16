package ru.yandex.practicum.store.controller;

import ru.yandex.practicum.api.ShoppingStoreApi;
import ru.yandex.practicum.dto.ProductDto;
import ru.yandex.practicum.dto.SetProductQuantityStateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ru.yandex.practicum.model.ProductCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.store.service.ProductService;
import ru.yandex.practicum.validation.Create;
import ru.yandex.practicum.validation.Update;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shopping-store")
@RequiredArgsConstructor
@Validated
public class ShoppingStoreController implements ShoppingStoreApi {

    private final ProductService productService;

    //   GET /api/v1/shopping-store?category=LIGHTING&page=2&size=5&sort=price,desc&sort=productName,asc
    // спринг сам распарсит параметры и создаст из них pageable ( можно несколько параметров сортировки)
    @GetMapping
    @Override
    public Page<ProductDto> getProducts(
            @RequestParam ProductCategory category,
            @PageableDefault(size = 20, sort = "productName", direction = Sort.Direction.ASC) Pageable pageable) {

        return productService.getProducts(category, pageable);
    }

    @GetMapping("/{productId}")
    @Override
    public ProductDto getProduct(@PathVariable UUID productId) {
        return productService.getProduct(productId);
    }

    @PutMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Override
    public ProductDto createNewProduct(@Validated(Create.class) @RequestBody ProductDto productDto) {
        return productService.createNewProduct(productDto);
    }

    @PostMapping
    @Override
    public ProductDto updateProduct(@Validated(Update.class) @RequestBody ProductDto productDto) {
        return productService.updateProduct(productDto);
    }

    @PostMapping("/removeProductFromStore")
    @Override
    public boolean removeProductFromStore(@RequestBody UUID productId) {
        return productService.removeProductFromStore(productId);
    }

    @PostMapping("/quantityState")
    @Override
    public boolean setProductQuantityState(@Valid @RequestBody SetProductQuantityStateRequest request) {
        return productService.setProductQuantityState(request);
    }
}
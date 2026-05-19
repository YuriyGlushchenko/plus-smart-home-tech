package ru.yandex.practicum.store.controller;

import ru.yandex.practicum.api.ShoppingStoreApi;
import ru.yandex.practicum.dto.ProductDto;
import ru.yandex.practicum.dto.SetProductQuantityStateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.yandex.practicum.model.ProductCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.model.QuantityState;
import ru.yandex.practicum.store.service.ProductService;
import ru.yandex.practicum.validation.Create;
import ru.yandex.practicum.validation.Update;

import java.util.UUID;

@Slf4j
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

        log.debug("GET /api/v1/shopping-store - Получение товаров по категории: {}, страница: {}, размер: {}, сортировка: {}",
                category, pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        return productService.getProducts(category, pageable);
    }

    @GetMapping("/{productId}")
    @Override
    public ProductDto getProduct(@PathVariable UUID productId) {
        log.debug("GET /api/v1/shopping-store/{} - Получение товара по id", productId);
        return productService.getProduct(productId);
    }

    @PutMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Override
    public ProductDto createNewProduct(@Validated(Create.class) @RequestBody ProductDto productDto) {
        log.debug("PUT /api/v1/shopping-store - Создание нового товара: {}", productDto);
        return productService.createNewProduct(productDto);
    }

    @PostMapping
    @Override
    public ProductDto updateProduct(@Validated(Update.class) @RequestBody ProductDto productDto) {
        log.debug("POST /api/v1/shopping-store - Обновление товара: {}", productDto);
        return productService.updateProduct(productDto);
    }

    @PostMapping("/removeProductFromStore")
    @Override
    public boolean removeProductFromStore(@RequestBody UUID productId) {
        log.debug("POST /api/v1/shopping-store/removeProductFromStore - Удаление товара: {}", productId);
        return productService.removeProductFromStore(productId);
    }

    @PostMapping(value = "/quantityState")
    @ResponseStatus(HttpStatus.OK)
    @Override
    public boolean setProductQuantityState(
            @RequestParam UUID productId,
            @RequestParam QuantityState  quantityState) {

        log.debug("POST /api/v1/shopping-store/quantityState (query params) - Изменение статуса количества товара: productId={}, quantityState={}",
                productId, quantityState);

        SetProductQuantityStateRequest request = SetProductQuantityStateRequest.builder()
                .productId(productId)
                .quantityState(quantityState)
                .build();

        return productService.setProductQuantityState(request);
    }

}
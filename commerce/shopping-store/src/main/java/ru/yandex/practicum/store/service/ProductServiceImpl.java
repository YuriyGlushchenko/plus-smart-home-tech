package ru.yandex.practicum.store.service;

import dto.ProductDto;
import dto.SetProductQuantityStateRequest;
import ru.yandex.practicum.store.exceptions.exceptions.ProductNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import model.ProductCategory;
import model.ProductState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.store.mapper.ProductMapper;
import ru.yandex.practicum.store.model.Product;
import ru.yandex.practicum.store.repository.ProductRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    public Page<ProductDto> getProducts(ProductCategory category, Pageable pageable) {
        log.info("Получение товаров по категории: {}, страница: {}, размер: {}",
                category, pageable.getPageNumber(), pageable.getPageSize());

        Page<Product> products = productRepository.findByProductCategoryAndProductState(
                category,
                ProductState.ACTIVE,
                pageable
        );

        return products.map(productMapper::toDto);  // map - встроенный метод самого Page<T>, мапит контент страницы
    }

    @Override
    public ProductDto getProduct(UUID productId) {
        log.info("Получение товара по id: {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Товар с id: " + productId + " не найден"));

        return productMapper.toDto(product);
    }

    @Override
    @Transactional
    public ProductDto createNewProduct(ProductDto productDto) {
        log.info("Создание нового товара: {}", productDto.getProductName());

        Product product = productMapper.toEntity(productDto);
        product.setProductState(ProductState.ACTIVE);

        Product savedProduct = productRepository.save(product);
        log.info("Товар создан с id: {}", savedProduct.getId());

        return productMapper.toDto(savedProduct);
    }

    @Override
    @Transactional
    public ProductDto updateProduct(ProductDto productDto) {
        log.info("Обновление товара с id: {}", productDto.getProductId());

        Product existingProduct = productRepository.findById(productDto.getProductId())
                .orElseThrow(() -> new ProductNotFoundException("Товар с id: " + productDto.getProductId() + " не найден"));

        if (productDto.getProductName() != null) {
            existingProduct.setProductName(productDto.getProductName());
        }
        if (productDto.getDescription() != null) {
            existingProduct.setDescription(productDto.getDescription());
        }
        if (productDto.getImageSrc() != null) {
            existingProduct.setImageSrc(productDto.getImageSrc());
        }
        if (productDto.getPrice() != null) {
            existingProduct.setPrice(productDto.getPrice());
        }
        if (productDto.getProductCategory() != null) {
            existingProduct.setProductCategory(productDto.getProductCategory());
        }
        if (productDto.getProductState() != null) {
            existingProduct.setProductState(productDto.getProductState());
        }
        if (productDto.getQuantityState() != null) {
            existingProduct.setQuantityState(productDto.getQuantityState());
        }

        Product updatedProduct = productRepository.save(existingProduct);
        log.info("Обновлён товар с id: {}", updatedProduct.getId());

        return productMapper.toDto(updatedProduct);
    }

    @Override
    @Transactional
    public boolean removeProductFromStore(UUID productId) {
        log.info("Удаление товара (деактивация) с id: {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Товар с id: " + productId + " не найден"));

        product.setProductState(ProductState.DEACTIVATE);
        productRepository.save(product);
        log.info("Деактивирован товар с id: {}", productId);

        return true;
    }

    @Override
    @Transactional
    public boolean setProductQuantityState(SetProductQuantityStateRequest request) {
        log.info("Установка статуса количества для товара {} в {}",
                request.getProductId(), request.getQuantityState());

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ProductNotFoundException("Товар с id: " + request.getProductId() + " не найден"));

        product.setQuantityState(request.getQuantityState());
        productRepository.save(product);
        log.info("Статус количества обновлён для товара c id {}", request.getProductId());

        return true;
    }
}
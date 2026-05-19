package ru.yandex.practicum.store.mapper;

import ru.yandex.practicum.dto.ProductDto;
import org.mapstruct.*;
import ru.yandex.practicum.store.model.Product;

@Mapper(componentModel = "spring") // из интерфейса будет сгенерирован маппер, и сгенерирован как компонент
public interface ProductMapper {

    @Mapping(source = "id", target = "productId")
    ProductDto toDto(Product product);

    @Mapping(target = "id", source = "productId")
    Product toEntity(ProductDto productDto);

    // @MappingTarget - указывает, что параметр - это целевой объект, который нужно обновить, а не создавать новый
    @Mapping(target = "id", ignore = true) // ID нельзя менять
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)  // Обновляет только не-null поля
    void updateEntity(ProductDto productDto, @MappingTarget Product product);
}
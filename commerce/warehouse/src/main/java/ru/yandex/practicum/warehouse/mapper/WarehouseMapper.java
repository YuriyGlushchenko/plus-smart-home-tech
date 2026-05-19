package ru.yandex.practicum.warehouse.mapper;


import org.mapstruct.*;
import ru.yandex.practicum.dto.BookedProductsDto;
import ru.yandex.practicum.dto.NewProductInWarehouseRequest;
import ru.yandex.practicum.warehouse.model.WarehouseProduct;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface WarehouseMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "quantity", constant = "0L")
    @Mapping(target = "width", source = "dimension.width")
    @Mapping(target = "height", source = "dimension.height")
    @Mapping(target = "depth", source = "dimension.depth")
    WarehouseProduct toEntity(NewProductInWarehouseRequest request);

    @Mapping(target = "deliveryWeight", expression = "java(product.getWeight() * quantity)")
    @Mapping(target = "deliveryVolume", expression = "java(product.getWidth() * product.getHeight() * product.getDepth() * quantity)")
    BookedProductsDto toBookedProductsDto(WarehouseProduct product, Long quantity);
}
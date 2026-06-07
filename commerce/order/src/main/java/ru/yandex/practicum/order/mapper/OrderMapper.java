package ru.yandex.practicum.order.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.data.domain.jaxb.SpringDataJaxb;
import ru.yandex.practicum.dto.CreateNewOrderRequest;
import ru.yandex.practicum.dto.OrderDto;
import ru.yandex.practicum.order.model.Order;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface OrderMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "shoppingCartId", source = "request.shoppingCart.shoppingCartId")
    @Mapping(target = "products", source = "request.shoppingCart.products")
    @Mapping(target = "state", constant = "NEW")
    @Mapping(target = "paymentId", ignore = true)
    @Mapping(target = "deliveryId", ignore = true)
    @Mapping(target = "deliveryWeight", ignore = true)
    @Mapping(target = "deliveryVolume", ignore = true)
    @Mapping(target = "fragile", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "deliveryPrice", ignore = true)
    @Mapping(target = "productPrice", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Order toEntity(CreateNewOrderRequest request);

    @Mapping(source = "id", target = "orderId")
    OrderDto toDto(Order order);
}
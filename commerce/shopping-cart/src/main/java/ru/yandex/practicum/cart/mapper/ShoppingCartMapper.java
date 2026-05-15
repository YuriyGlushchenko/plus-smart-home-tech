package ru.yandex.practicum.cart.mapper;


import dto.ShoppingCartDto;
import org.mapstruct.*;
import ru.yandex.practicum.cart.model.ShoppingCart;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ShoppingCartMapper {

    @Mapping(source = "id", target = "shoppingCartId")
    ShoppingCartDto toDto(ShoppingCart cart);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "state", ignore = true)
    void updateEntity(ShoppingCartDto cartDto, @MappingTarget ShoppingCart cart);
}
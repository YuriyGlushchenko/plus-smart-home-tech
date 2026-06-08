package ru.yandex.practicum.delivery.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.yandex.practicum.delivery.model.Address;
import ru.yandex.practicum.delivery.model.Delivery;
import ru.yandex.practicum.dto.AddressDto;
import ru.yandex.practicum.dto.DeliveryDto;

@Mapper(componentModel = "spring")
public interface DeliveryMapper {

    @Mapping(source = "id", target = "deliveryId")
    @Mapping(source = "state", target = "deliveryState")
    DeliveryDto toDto(Delivery delivery);

    Address toAddress(AddressDto addressDto);

    AddressDto toAddressDto(Address address);
}
package ru.yandex.practicum.payment.mapper;



import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.yandex.practicum.dto.PaymentDto;
import ru.yandex.practicum.payment.model.Payment;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(source = "id", target = "paymentId")
    PaymentDto toDto(Payment payment);
}

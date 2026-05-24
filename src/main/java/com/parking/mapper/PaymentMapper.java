package com.parking.mapper;

import com.parking.dto.payment.PaymentResponse;
import com.parking.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    @Mapping(target = "bookingId", source = "booking.id")
    PaymentResponse toResponse(Payment payment);
}

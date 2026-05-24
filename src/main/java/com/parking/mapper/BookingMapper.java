package com.parking.mapper;

import com.parking.dto.booking.BookingResponse;
import com.parking.entity.Booking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "spotId", source = "spot.id")
    @Mapping(target = "spotNumber", source = "spot.spotNumber")
    @Mapping(target = "parkingId", source = "spot.parking.id")
    @Mapping(target = "parkingName", source = "spot.parking.name")
    @Mapping(target = "vehicleId", source = "vehicle.id")
    BookingResponse toResponse(Booking booking);
}

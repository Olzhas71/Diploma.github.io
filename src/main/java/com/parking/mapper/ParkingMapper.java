package com.parking.mapper;

import com.parking.dto.parking.*;
import com.parking.entity.Parking;
import com.parking.entity.ParkingSpot;
import com.parking.entity.Tariff;
import org.mapstruct.Builder;
import org.mapstruct.*;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface ParkingMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "spots", ignore = true)
    @Mapping(target = "tariffs", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Parking toEntity(ParkingRequest request);

    @Mapping(target = "freeSpots", ignore = true)
    ParkingResponse toResponse(Parking parking);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "spots", ignore = true)
    @Mapping(target = "tariffs", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void update(ParkingRequest request, @MappingTarget Parking parking);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parking", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ParkingSpot toSpotEntity(SpotRequest request);

    @Mapping(target = "parkingId", source = "parking.id")
    SpotResponse toSpotResponse(ParkingSpot spot);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parking", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateSpot(SpotRequest request, @MappingTarget ParkingSpot spot);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parking", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Tariff toTariffEntity(TariffRequest request);

    @Mapping(target = "parkingId", source = "parking.id")
    TariffResponse toTariffResponse(Tariff tariff);
}

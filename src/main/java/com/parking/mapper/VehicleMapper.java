package com.parking.mapper;

import com.parking.dto.vehicle.VehicleRequest;
import com.parking.dto.vehicle.VehicleResponse;
import com.parking.entity.Vehicle;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface VehicleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Vehicle toEntity(VehicleRequest request);

    @Mapping(target = "ownerId", source = "owner.id")
    VehicleResponse toResponse(Vehicle vehicle);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void update(VehicleRequest request, @MappingTarget Vehicle vehicle);
}

package com.parking.mapper;

import com.parking.dto.user.UserResponse;
import com.parking.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);
}

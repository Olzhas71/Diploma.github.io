package com.parking.service;

import com.parking.dto.user.UserResponse;
import com.parking.entity.User;
import com.parking.exception.ResourceNotFoundException;
import com.parking.mapper.UserMapper;
import com.parking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return userMapper.toResponse(loadById(id));
    }

    @Transactional(readOnly = true)
    public User loadById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id));
    }
}

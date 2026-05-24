package com.parking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parking.dto.parking.ParkingRequest;
import com.parking.entity.ParkingType;
import com.parking.entity.Role;
import com.parking.entity.User;
import com.parking.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ParkingControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void anonymousCannotListParkings() throws Exception {
        // Spring Security 6 default rejects unauthenticated requests with 403 when no
        // AuthenticationEntryPoint is configured. Either 401 or 403 means "denied".
        mvc.perform(get("/parkings"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void driverCanListParkings() throws Exception {
        mvc.perform(get("/parkings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void driverCannotCreateParking() throws Exception {
        ParkingRequest req = sampleRequest();
        mvc.perform(post("/parkings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanCreateParking() throws Exception {
        ensureUser("admin-it@example.com", Role.ADMIN);
        ParkingRequest req = sampleRequest();
        mvc.perform(post("/parkings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("IT Parking"))
                .andExpect(jsonPath("$.totalSpots").value(20));
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void invalidParkingPayloadReturns400() throws Exception {
        // latitude 99 is out of [-90,90], totalSpots 0 fails @Min
        String bad = """
                {"name":"x","address":"y","latitude":99.0,"longitude":0.0,"type":"GROUND","totalSpots":0}
                """;
        mvc.perform(post("/parkings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bad))
                .andExpect(status().isBadRequest());
    }

    private ParkingRequest sampleRequest() {
        return new ParkingRequest(
                "IT Parking", "1 Test St", 50.0, 30.0,
                ParkingType.GROUND, 20,
                LocalTime.of(0, 0), LocalTime.of(23, 59));
    }

    private void ensureUser(String email, Role role) {
        userRepository.findByEmail(email).orElseGet(() -> userRepository.save(
                User.builder()
                        .email(email)
                        .passwordHash(passwordEncoder.encode("password123"))
                        .fullName("IT User")
                        .role(role)
                        .enabled(true)
                        .build()));
    }
}

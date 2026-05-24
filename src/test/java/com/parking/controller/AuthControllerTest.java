package com.parking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parking.dto.auth.LoginRequest;
import com.parking.dto.auth.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void registerAndLoginRoundTrip() throws Exception {
        RegisterRequest reg = new RegisterRequest(
                "alice@example.com", "password123", "Alice Driver", "+10000001");
        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(reg)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.role").value("DRIVER"))
                .andExpect(jsonPath("$.email").value("alice@example.com"));

        LoginRequest login = new LoginRequest("alice@example.com", "password123");
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void duplicateRegistrationFails() throws Exception {
        RegisterRequest reg = new RegisterRequest(
                "bob@example.com", "password123", "Bob Driver", null);
        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(reg)))
                .andExpect(status().isOk());
        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(reg)))
                .andExpect(status().isConflict());
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        RegisterRequest reg = new RegisterRequest(
                "carol@example.com", "password123", "Carol Driver", null);
        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(reg)))
                .andExpect(status().isOk());

        LoginRequest wrong = new LoginRequest("carol@example.com", "wrongpassword");
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(wrong)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidEmailReturns400() throws Exception {
        RegisterRequest bad = new RegisterRequest(
                "not-an-email", "password123", "X", null);
        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }
}

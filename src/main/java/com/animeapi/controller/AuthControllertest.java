package com.animeapi.controller;

import com.animeapi.dto.request.LoginRequest;
import com.animeapi.dto.request.RegisterRequest;
import com.animeapi.dto.response.JwtResponse;
import com.animeapi.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private AuthService authService;

    @Test
    void register_ShouldReturn201_WhenValidRequest() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@test.com");
        request.setPassword("password123");

        JwtResponse jwtResponse = new JwtResponse("token123", "newuser", "USER");
        when(authService.register(any(RegisterRequest.class))).thenReturn(jwtResponse);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("token123"))
                .andExpect(jsonPath("$.username").value("newuser"));
    }

    @Test
    void register_ShouldReturn400_WhenInvalidRequest() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(""); // inválido
        request.setEmail("not-an-email"); // inválido
        request.setPassword("123"); // muito curto

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_ShouldReturn200_WhenValidCredentials() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        JwtResponse jwtResponse = new JwtResponse("token123", "testuser", "USER");
        when(authService.login(any(LoginRequest.class))).thenReturn(jwtResponse);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token123"))
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void login_ShouldReturn400_WhenBlankFields() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("");
        request.setPassword("");

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
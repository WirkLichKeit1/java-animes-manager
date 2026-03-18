package com.animeapi.controller;

import com.animeapi.dto.request.AnimeRequest;
import com.animeapi.dto.response.AnimeResponse;
import com.animeapi.dto.response.PageResponse;
import com.animeapi.model.AnimeStatus;
import com.animeapi.service.AnimeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AnimeController.class)
class AnimeControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private AnimeService animeService;
    @MockBean private com.animeapi.security.JwtService jwtService;
    @MockBean private com.animeapi.security.UserDetailsServiceImpl userDetailsService;

    private AnimeResponse animeResponse;

    @BeforeEach
    void setUp() {
        animeResponse = new AnimeResponse();
        animeResponse.setId(1L);
        animeResponse.setTitle("Naruto");
        animeResponse.setGenre("Action");
        animeResponse.setStatus(AnimeStatus.COMPLETED);
    }

    @Test
    void findAll_ShouldReturn200_WithoutAuth() throws Exception {
        PageResponse<AnimeResponse> page = new PageResponse<>(List.of(animeResponse), 0, 20, 1, 1, true);
        when(animeService.findAll(any())).thenReturn(page);

        mockMvc.perform(get("/api/animes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Naruto"));
    }

    @Test
    void findById_ShouldReturn200_WithoutAuth() throws Exception {
        when(animeService.findById(1L)).thenReturn(animeResponse);

        mockMvc.perform(get("/api/animes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Naruto"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_ShouldReturn201_WhenAdmin() throws Exception {
        AnimeRequest request = new AnimeRequest();
        request.setTitle("One Piece");
        request.setGenre("Adventure");

        when(animeService.create(any(AnimeRequest.class))).thenReturn(animeResponse);

        mockMvc.perform(post("/api/animes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "USER")
    void create_ShouldReturn403_WhenUser() throws Exception {
        AnimeRequest request = new AnimeRequest();
        request.setTitle("One Piece");

        mockMvc.perform(post("/api/animes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_ShouldReturn200_WhenAdmin() throws Exception {
        AnimeRequest request = new AnimeRequest();
        request.setTitle("Naruto Shippuden");
        request.setGenre("Action");

        when(animeService.update(eq(1L), any(AnimeRequest.class))).thenReturn(animeResponse);

        mockMvc.perform(put("/api/animes/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_ShouldReturn204_WhenAdmin() throws Exception {
        mockMvc.perform(delete("/api/animes/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void findAllGenres_ShouldReturn200_WithoutAuth() throws Exception {
        when(animeService.findAllGenres()).thenReturn(List.of("Action", "Comedy"));

        mockMvc.perform(get("/api/animes/genres"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("Action"));
    }
}
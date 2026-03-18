package com.animeapi.service;

import com.animeapi.dto.request.AnimeRequest;
import com.animeapi.dto.response.AnimeResponse;
import com.animeapi.dto.response.PageResponse;
import com.animeapi.exception.ResourceNotFoundException;
import com.animeapi.model.Anime;
import com.animeapi.model.AnimeStatus;
import com.animeapi.repository.AnimeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnimeServiceTest {

    @Mock private AnimeRepository animeRepository;
    @Mock private StorageService storageService;

    @InjectMocks
    private AnimeService animeService;

    private Anime anime;

    @BeforeEach
    void setUp() {
        anime = Anime.builder()
                .id(1L)
                .title("Naruto")
                .genre("Action")
                .status(AnimeStatus.COMPLETED)
                .build();
    }

    @Test
    void findById_ShouldReturnAnime_WhenExists() {
        when(animeRepository.findById(1L)).thenReturn(Optional.of(anime));

        AnimeResponse response = animeService.findById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Naruto");
    }

    @Test
    void findById_ShouldThrow_WhenNotFound() {
        when(animeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> animeService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findAll_ShouldReturnPage() {
        Page<Anime> animePage = new PageImpl<>(List.of(anime));
        when(animeRepository.findAll(any(PageRequest.class))).thenReturn(animePage);

        PageResponse<AnimeResponse> response = animeService.findAll(PageRequest.of(0, 20));

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    void create_ShouldSaveAndReturnAnime() {
        AnimeRequest request = new AnimeRequest();
        request.setTitle("One Piece");
        request.setGenre("Adventure");
        request.setStatus(AnimeStatus.ONGOING);

        when(animeRepository.save(any(Anime.class))).thenReturn(anime);

        AnimeResponse response = animeService.create(request);

        verify(animeRepository).save(any(Anime.class));
        assertThat(response).isNotNull();
    }

    @Test
    void update_ShouldUpdateAndReturnAnime() {
        AnimeRequest request = new AnimeRequest();
        request.setTitle("Naruto Shippuden");
        request.setGenre("Action");

        when(animeRepository.findById(1L)).thenReturn(Optional.of(anime));
        when(animeRepository.save(any(Anime.class))).thenReturn(anime);

        AnimeResponse response = animeService.update(1L, request);

        verify(animeRepository).save(anime);
        assertThat(response).isNotNull();
    }

    @Test
    void update_ShouldThrow_WhenNotFound() {
        when(animeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> animeService.update(99L, new AnimeRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_ShouldDelete_WhenExists() {
        when(animeRepository.findById(1L)).thenReturn(Optional.of(anime));

        animeService.delete(1L);

        verify(animeRepository).delete(anime);
    }

    @Test
    void delete_ShouldThrow_WhenNotFound() {
        when(animeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> animeService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findAllGenres_ShouldReturnList() {
        when(animeRepository.findAllGenres()).thenReturn(List.of("Action", "Comedy", "Drama"));

        List<String> genres = animeService.findAllGenres();

        assertThat(genres).hasSize(3).contains("Action", "Comedy", "Drama");
    }
}
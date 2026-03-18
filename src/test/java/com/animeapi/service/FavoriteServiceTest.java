package com.animeapi.service;

import com.animeapi.dto.response.AnimeResponse;
import com.animeapi.dto.response.PageResponse;
import com.animeapi.exception.ResourceNotFoundException;
import com.animeapi.model.*;
import com.animeapi.repository.AnimeRepository;
import com.animeapi.repository.FavoriteRepository;
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
class FavoriteServiceTest {

    @Mock private FavoriteRepository favoriteRepository;
    @Mock private AnimeRepository animeRepository;
    @Mock private AnimeService animeService;

    @InjectMocks
    private FavoriteService favoriteService;

    private User user;
    private Anime anime;
    private Favorite favorite;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("testuser")
                .role(Role.USER)
                .build();

        anime = Anime.builder()
                .id(1L)
                .title("Naruto")
                .status(AnimeStatus.COMPLETED)
                .build();

        favorite = Favorite.builder()
                .id(1L)
                .user(user)
                .anime(anime)
                .build();
    }

    @Test
    void addFavorite_ShouldSave_WhenNotAlreadyFavorited() {
        when(favoriteRepository.existsByUserIdAndAnimeId(1L, 1L)).thenReturn(false);
        when(animeRepository.findById(1L)).thenReturn(Optional.of(anime));

        favoriteService.addFavorite(user, 1L);

        verify(favoriteRepository).save(any(Favorite.class));
    }

    @Test
    void addFavorite_ShouldThrow_WhenAlreadyFavorited() {
        when(favoriteRepository.existsByUserIdAndAnimeId(1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> favoriteService.addFavorite(user, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already in favorites");
    }

    @Test
    void addFavorite_ShouldThrow_WhenAnimeNotFound() {
        when(favoriteRepository.existsByUserIdAndAnimeId(1L, 99L)).thenReturn(false);
        when(animeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoriteService.addFavorite(user, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void removeFavorite_ShouldDelete_WhenExists() {
        when(favoriteRepository.existsByUserIdAndAnimeId(1L, 1L)).thenReturn(true);

        favoriteService.removeFavorite(user, 1L);

        verify(favoriteRepository).deleteByUserIdAndAnimeId(1L, 1L);
    }

    @Test
    void removeFavorite_ShouldThrow_WhenNotFound() {
        when(favoriteRepository.existsByUserIdAndAnimeId(1L, 99L)).thenReturn(false);

        assertThatThrownBy(() -> favoriteService.removeFavorite(user, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getFavorites_ShouldReturnPage() {
        Page<Favorite> favoritePage = new PageImpl<>(List.of(favorite));
        when(favoriteRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any())).thenReturn(favoritePage);
        when(animeService.toResponse(anime)).thenReturn(new AnimeResponse());

        PageResponse<AnimeResponse> response = favoriteService.getFavorites(user, PageRequest.of(0, 20));

        assertThat(response.getContent()).hasSize(1);
    }

    @Test
    void isFavorite_ShouldReturnTrue_WhenFavorited() {
        when(favoriteRepository.existsByUserIdAndAnimeId(1L, 1L)).thenReturn(true);

        assertThat(favoriteService.isFavorite(user, 1L)).isTrue();
    }

    @Test
    void isFavorite_ShouldReturnFalse_WhenNotFavorited() {
        when(favoriteRepository.existsByUserIdAndAnimeId(1L, 99L)).thenReturn(false);

        assertThat(favoriteService.isFavorite(user, 99L)).isFalse();
    }
}
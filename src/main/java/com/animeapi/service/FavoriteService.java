package com.animeapi.service;

import com.animeapi.dto.response.AnimeResponse;
import com.animeapi.dto.response.PageResponse;
import com.animeapi.exception.ResourceNotFoundException;
import com.animeapi.model.Anime;
import com.animeapi.model.Favorite;
import com.animeapi.model.User;
import com.animeapi.repository.AnimeRepository;
import com.animeapi.repository.FavoriteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final AnimeRepository animeRepository;
    private final AnimeService animeService;

    @Transactional
    public void addFavorite(User user, Long animeId) {
        if (favoriteRepository.existsByUserIdAndAnimeId(user.getId(), animeId)) {
            throw new IllegalArgumentException("Anime already in favorites");
        }

        Anime anime = animeRepository.findById(animeId)
                .orElseThrow(() -> new ResourceNotFoundException("Anime", animeId));

        Favorite favorite = Favorite.builder()
                .user(user)
                .anime(anime)
                .build();

        favoriteRepository.save(favorite);
        log.info("Anime {} added to favorites by user {}", animeId, user.getUsername());
    }

    @Transactional
    public void removeFavorite(User user, Long animeId) {
        if (!favoriteRepository.existsByUserIdAndAnimeId(user.getId(), animeId)) {
            throw new ResourceNotFoundException("Favorite not found");
        }

        favoriteRepository.deleteByUserIdAndAnimeId(user.getId(), animeId);
        log.info("Anime {} removed from favorites by user {}", animeId, user.getUsername());
    }

    @Transactional(readOnly = true)
    public PageResponse<AnimeResponse> getFavorites(User user, Pageable pageable) {
        Page<AnimeResponse> page = favoriteRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(favorite -> animeService.toResponse(favorite.getAnime()));
        return PageResponse.of(page);
    }

    @Transactional(readOnly = true)
    public boolean isFavorite(User user, Long animeId) {
        return favoriteRepository.existsByUserIdAndAnimeId(user.getId(), animeId);
    }
}
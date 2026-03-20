package com.animeapi.service;

import com.animeapi.dto.request.AnimeRequest;
import com.animeapi.dto.response.AnimeResponse;
import com.animeapi.dto.response.PageResponse;
import com.animeapi.exception.ResourceNotFoundException;
import com.animeapi.model.Anime;
import com.animeapi.model.AnimeStatus;
import com.animeapi.repository.AnimeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnimeService {

    private final AnimeRepository animeRepository;
    private final StorageService storageService;

    @Transactional(readOnly = true)
    @Cacheable(value = "animes", key = "#id")
    public AnimeResponse findById(Long id) {
        return toResponse(getAnimeOrThrow(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<AnimeResponse> search(String title, String genre, AnimeStatus status, Integer year, Pageable pageable) {
        Page<AnimeResponse> page = animeRepository
                .search(title, genre, status, year, pageable)
                .map(this::toResponse);
        return PageResponse.of(page);
    }

    @Transactional(readOnly = true)
    public PageResponse<AnimeResponse> findAll(Pageable pageable) {
        Page<AnimeResponse> page = animeRepository.findAll(pageable).map(this::toResponse);
        return PageResponse.of(page);
    }

    @Transactional(readOnly = true)
    public List<String> findAllGenres() {
        return animeRepository.findAllGenres();
    }

    @Transactional
    @CacheEvict(value = "animes", allEntries = true)
    public AnimeResponse create(AnimeRequest request) {
        Anime anime = Anime.builder()
                .title(request.getTitle())
                .originalTitle(request.getOriginalTitle())
                .synopsis(request.getSynopsis())
                .genre(request.getGenre())
                .studio(request.getStudio())
                .releaseYear(request.getReleaseYear())
                .status(request.getStatus() != null ? request.getStatus() : AnimeStatus.ONGOING)
                .rating(request.getRating())
                .build();

        animeRepository.save(anime);
        log.info("Anime created: {}", anime.getTitle());
        return toResponse(anime);
    }

    @Transactional
    @CacheEvict(value = "animes", key = "#id")
    public AnimeResponse update(Long id, AnimeRequest request) {
        Anime anime = getAnimeOrThrow(id);

        anime.setTitle(request.getTitle());
        anime.setOriginalTitle(request.getOriginalTitle());
        anime.setSynopsis(request.getSynopsis());
        anime.setGenre(request.getGenre());
        anime.setStudio(request.getStudio());
        anime.setReleaseYear(request.getReleaseYear());
        anime.setRating(request.getRating());
        if (request.getStatus() != null) {
            anime.setStatus(request.getStatus());
        }

        animeRepository.save(anime);
        log.info("Anime updated: {}", anime.getTitle());
        return toResponse(anime);
    }

    @Transactional
    @CacheEvict(value = "animes", key = "#id")
    public void uploadCover(Long id, MultipartFile file, boolean isBanner) {
        Anime anime = getAnimeOrThrow(id);
        String filename = storageService.store(file, "images");

        if (isBanner) {
            if (anime.getBannerImageUrl() != null) {
                storageService.delete(anime.getBannerImageUrl());
            }
            anime.setBannerImageUrl(filename);
        } else {
            if (anime.getCoverImageUrl() != null) {
                storageService.delete(anime.getCoverImageUrl());
            }
            anime.setCoverImageUrl(filename);
        }

        animeRepository.save(anime);
        log.info("Image uploaded for anime {}: {}", id, filename);
    }

    @Transactional
    @CacheEvict(value = "animes", key = "#id")
    public void delete(Long id) {
        Anime anime = getAnimeOrThrow(id);
        animeRepository.delete(anime);
        log.info("Anime deleted: {}", id);
    }

    private Anime getAnimeOrThrow(Long id) {
        return animeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Anime", id));
    }

    public AnimeResponse toResponse(Anime anime) {
        AnimeResponse response = new AnimeResponse();
        response.setId(anime.getId());
        response.setTitle(anime.getTitle());
        response.setOriginalTitle(anime.getOriginalTitle());
        response.setSynopsis(anime.getSynopsis());
        response.setGenre(anime.getGenre());
        response.setStudio(anime.getStudio());
        response.setReleaseYear(anime.getReleaseYear());
        response.setStatus(anime.getStatus());
        response.setCoverImageUrl(anime.getCoverImageUrl());
        response.setBannerImageUrl(anime.getBannerImageUrl());
        response.setRating(anime.getRating());
        response.setTotalEpisodes(anime.getEpisodeCount());
        response.setCreatedAt(anime.getCreatedAt());
        response.setUpdatedAt(anime.getUpdatedAt());
        return response;
    }
}
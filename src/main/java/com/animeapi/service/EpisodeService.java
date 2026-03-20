package com.animeapi.service;

import com.animeapi.dto.request.EpisodeRequest;
import com.animeapi.dto.response.EpisodeResponse;
import com.animeapi.dto.response.PageResponse;
import com.animeapi.exception.ResourceNotFoundException;
import com.animeapi.model.Anime;
import com.animeapi.model.Episode;
import com.animeapi.repository.AnimeRepository;
import com.animeapi.repository.EpisodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EpisodeService {

    private final EpisodeRepository episodeRepository;
    private final AnimeRepository animeRepository;
    private final StorageService storageService;

    @Transactional(readOnly = true)
    public List<EpisodeResponse> findByAnime(Long animeId) {
        return episodeRepository
                .findByAnimeIdAndPublishedTrueOrderBySeasonNumberAscEpisodeNumberAsc(animeId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<EpisodeResponse> findByAnimePaged(Long animeId, Pageable pageable) {
        Page<EpisodeResponse> page = episodeRepository
                .findByAnimeId(animeId, pageable)
                .map(this::toResponse);
        return PageResponse.of(page);
    }

    @Transactional(readOnly = true)
    public EpisodeResponse findById(Long id) {
        Episode episode = getEpisodeOrThrow(id);
        return toResponse(episode);
    }

    @Transactional
    public EpisodeResponse create(Long animeId, EpisodeRequest request) {
        Anime anime = animeRepository.findById(animeId)
                .orElseThrow(() -> new ResourceNotFoundException("Anime", animeId));

        if (episodeRepository.existsByAnimeIdAndSeasonNumberAndEpisodeNumber(
                animeId, request.getSeasonNumber(), request.getEpisodeNumber())) {
            throw new IllegalArgumentException(
                    "Episode S" + request.getSeasonNumber() + "E" + request.getEpisodeNumber() + " already exists");
        }

        Episode episode = Episode.builder()
                .anime(anime)
                .title(request.getTitle())
                .episodeNumber(request.getEpisodeNumber())
                .seasonNumber(request.getSeasonNumber())
                .synopsis(request.getSynopsis())
                .durationSeconds(request.getDurationSeconds())
                .published(request.isPublished())
                .build();

        episodeRepository.save(episode);
        log.info("Episode created: S{}E{} for anime {}", request.getSeasonNumber(), request.getEpisodeNumber(), animeId);
        return toResponse(episode);
    }

    @Transactional
    public EpisodeResponse update(Long id, EpisodeRequest request) {
        Episode episode = getEpisodeOrThrow(id);

        episode.setTitle(request.getTitle());
        episode.setSynopsis(request.getSynopsis());
        episode.setDurationSeconds(request.getDurationSeconds());
        episode.setPublished(request.isPublished());

        episodeRepository.save(episode);
        log.info("Episode updated: {}", id);
        return toResponse(episode);
    }

    @Transactional
    public void uploadThumbnail(Long id, MultipartFile file) {
        Episode episode = getEpisodeOrThrow(id);

        if (episode.getThumbnailUrl() != null) {
            storageService.delete(episode.getThumbnailUrl());
        }

        String filename = storageService.store(file, "images");
        episode.setThumbnailUrl(filename);
        episodeRepository.save(episode);
        log.info("Thumbnail uploaded for episode {}: {}", id, filename);
    }

    @Transactional
    public void delete(Long id) {
        Episode episode = getEpisodeOrThrow(id);

        if (episode.getVideoFilename() != null) {
            storageService.delete(episode.getVideoFilename());
        }
        if (episode.getThumbnailUrl() != null) {
            storageService.delete(episode.getThumbnailUrl());
        }

        episodeRepository.delete(episode);
        log.info("Episode deleted: {}", id);
    }

    @Transactional
    public void incrementViews(Long id) {
        episodeRepository.incrementViews(id);
    }

    private Episode getEpisodeOrThrow(Long id) {
        return episodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Episode", id));
    }

    public EpisodeResponse toResponse(Episode episode) {
        EpisodeResponse response = new EpisodeResponse();
        response.setId(episode.getId());
        response.setAnimeId(episode.getAnime().getId());
        response.setAnimeTitle(episode.getAnime().getTitle());
        response.setTitle(episode.getTitle());
        response.setEpisodeNumber(episode.getEpisodeNumber());
        response.setSeasonNumber(episode.getSeasonNumber());
        response.setSynopsis(episode.getSynopsis());
        response.setDurationSeconds(episode.getDurationSeconds());
        response.setThumbnailUrl(
            episode.getThumbnailUrl() != null ? storageService.getUrl(episode.getThumbnailUrl()) : null
        );
        response.setVideoStatus(episode.getVideoStatus());
        response.setViews(episode.getViews());
        response.setPublished(episode.isPublished());
        response.setCreatedAt(episode.getCreatedAt());
        response.setUpdatedAt(episode.getUpdatedAt());
        if (episode.getVideoFilename() != null) {
            response.setStreamUrl("/api/videos/stream/" + episode.getId());
        }
        return response;
    }
}
package com.animeapi.service;

import com.animeapi.dto.request.WatchProgressRequest;
import com.animeapi.dto.response.PageResponse;
import com.animeapi.dto.response.WatchHistoryResponse;
import com.animeapi.exception.ResourceNotFoundException;
import com.animeapi.model.Episode;
import com.animeapi.model.User;
import com.animeapi.model.WatchHistory;
import com.animeapi.repository.EpisodeRepository;
import com.animeapi.repository.WatchHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WatchHistoryService {

    private final WatchHistoryRepository watchHistoryRepository;
    private final EpisodeRepository episodeRepository;
    private final StorageService storageService;

    @Transactional
    public WatchHistoryResponse saveProgress(User user, WatchProgressRequest request) {
        // JOIN FETCH carrega Episode + Anime em uma única query,
        // garantindo que toResponse() funcione mesmo após o commit da transação.
        Episode episode = episodeRepository.findByIdWithAnime(request.getEpisodeId())
                .orElseThrow(() -> new ResourceNotFoundException("Episode", request.getEpisodeId()));

        WatchHistory history = watchHistoryRepository
                .findByUserIdAndEpisodeId(user.getId(), episode.getId())
                .orElse(WatchHistory.builder()
                        .user(user)
                        .episode(episode)
                        .build());

        history.setProgressSeconds(request.getProgressSeconds());
        history.setCompleted(request.isCompleted());
        history.setWatchedAt(java.time.LocalDateTime.now());

        watchHistoryRepository.save(history);
        episodeRepository.incrementViews(episode.getId());

        log.info("Progress saved for user {} on episode {}", user.getUsername(), episode.getId());
        return toResponse(history);
    }

    @Transactional(readOnly = true)
    public PageResponse<WatchHistoryResponse> getHistory(User user, Pageable pageable) {
        // JOIN FETCH carrega Episode + Anime junto — sem LazyInitializationException
        // no toResponse() mesmo fora de sessão ativa.
        Page<WatchHistoryResponse> page = watchHistoryRepository
                .findByUserIdWithEpisodeAndAnime(user.getId(), pageable)
                .map(this::toResponse);
        return PageResponse.of(page);
    }

    private WatchHistoryResponse toResponse(WatchHistory history) {
        WatchHistoryResponse response = new WatchHistoryResponse();
        response.setId(history.getId());
        response.setEpisodeId(history.getEpisode().getId());
        response.setEpisodeTitle(history.getEpisode().getTitle());
        response.setEpisodeNumber(history.getEpisode().getEpisodeNumber());
        response.setSeasonNumber(history.getEpisode().getSeasonNumber());
        response.setAnimeId(history.getEpisode().getAnime().getId());
        response.setAnimeTitle(history.getEpisode().getAnime().getTitle());

        String rawCoverUrl = history.getEpisode().getAnime().getCoverImageUrl();
        response.setAnimeCoverImageUrl(
            rawCoverUrl != null ? storageService.getUrl(rawCoverUrl) : null
        );

        response.setProgressSeconds(history.getProgressSeconds());
        response.setCompleted(history.isCompleted());
        response.setWatchedAt(history.getWatchedAt());
        return response;
    }
}
package com.animeapi.service;

import com.animeapi.dto.request.WatchProgressRequest;
import com.animeapi.dto.response.PageResponse;
import com.animeapi.dto.response.WatchHistoryResponse;
import com.animeapi.exception.ResourceNotFoundException;
import com.animeapi.model.*;
import com.animeapi.repository.EpisodeRepository;
import com.animeapi.repository.WatchHistoryRepository;
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
class WatchHistoryServiceTest {

    @Mock private WatchHistoryRepository watchHistoryRepository;
    @Mock private EpisodeRepository episodeRepository;
    @Mock private StorageService storageService;

    @InjectMocks
    private WatchHistoryService watchHistoryService;

    private User user;
    private Anime anime;
    private Episode episode;
    private WatchHistory watchHistory;

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
                .build();

        episode = Episode.builder()
                .id(1L)
                .anime(anime)
                .title("Episode 1")
                .episodeNumber(1)
                .seasonNumber(1)
                .views(0L)
                .build();

        watchHistory = WatchHistory.builder()
                .id(1L)
                .user(user)
                .episode(episode)
                .progressSeconds(300)
                .completed(false)
                .build();
    }

    @Test
    void saveProgress_ShouldCreateNewHistory_WhenNotExists() {
        WatchProgressRequest request = new WatchProgressRequest();
        request.setEpisodeId(1L);
        request.setProgressSeconds(300);
        request.setCompleted(false);
        when(episodeRepository.findByIdWithAnime(1L)).thenReturn(Optional.of(episode));
        when(watchHistoryRepository.findByUserIdAndEpisodeId(1L, 1L)).thenReturn(Optional.empty());
        when(watchHistoryRepository.save(any(WatchHistory.class))).thenReturn(watchHistory);

        WatchHistoryResponse response = watchHistoryService.saveProgress(user, request);

        verify(watchHistoryRepository).save(any(WatchHistory.class));
        verify(episodeRepository).incrementViews(1L);
        assertThat(response.getProgressSeconds()).isEqualTo(300);
    }

    @Test
    void saveProgress_ShouldUpdateExistingHistory_WhenExists() {
        WatchProgressRequest request = new WatchProgressRequest();
        request.setEpisodeId(1L);
        request.setProgressSeconds(600);
        request.setCompleted(true);

        when(episodeRepository.findByIdWithAnime(1L)).thenReturn(Optional.of(episode));
        when(watchHistoryRepository.findByUserIdAndEpisodeId(1L, 1L)).thenReturn(Optional.of(watchHistory));
        when(watchHistoryRepository.save(any(WatchHistory.class))).thenReturn(watchHistory);

        watchHistoryService.saveProgress(user, request);

        verify(watchHistoryRepository).save(watchHistory);
    }

    @Test
    void saveProgress_ShouldThrow_WhenEpisodeNotFound() {
        WatchProgressRequest request = new WatchProgressRequest();
        request.setEpisodeId(99L);
        request.setProgressSeconds(100);

        when(episodeRepository.findByIdWithAnime(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> watchHistoryService.saveProgress(user, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getHistory_ShouldReturnPagedHistory() {
        Page<WatchHistory> historyPage = new PageImpl<>(List.of(watchHistory));
        when(watchHistoryRepository.findByUserIdWithEpisodeAndAnime(eq(1L), any()))
                .thenReturn(historyPage);

        PageResponse<WatchHistoryResponse> response = watchHistoryService.getHistory(user, PageRequest.of(0, 20));

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
    }
}
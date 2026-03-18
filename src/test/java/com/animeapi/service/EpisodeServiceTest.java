package com.animeapi.service;

import com.animeapi.dto.request.EpisodeRequest;
import com.animeapi.dto.response.EpisodeResponse;
import com.animeapi.exception.ResourceNotFoundException;
import com.animeapi.model.Anime;
import com.animeapi.model.Episode;
import com.animeapi.model.VideoStatus;
import com.animeapi.repository.AnimeRepository;
import com.animeapi.repository.EpisodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EpisodeServiceTest {

    @Mock private EpisodeRepository episodeRepository;
    @Mock private AnimeRepository animeRepository;
    @Mock private StorageService storageService;

    @InjectMocks
    private EpisodeService episodeService;

    private Anime anime;
    private Episode episode;

    @BeforeEach
    void setUp() {
        anime = Anime.builder()
                .id(1L)
                .title("Naruto")
                .build();

        episode = Episode.builder()
                .id(1L)
                .anime(anime)
                .title("Enter: Naruto Uzumaki!")
                .episodeNumber(1)
                .seasonNumber(1)
                .videoStatus(VideoStatus.READY)
                .views(0L)
                .published(true)
                .build();
    }

    @Test
    void findByAnime_ShouldReturnPublishedEpisodes() {
        when(episodeRepository.findByAnimeIdAndPublishedTrueOrderBySeasonNumberAscEpisodeNumberAsc(1L))
                .thenReturn(List.of(episode));

        List<EpisodeResponse> result = episodeService.findByAnime(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Enter: Naruto Uzumaki!");
    }

    @Test
    void findById_ShouldReturnEpisode_WhenExists() {
        when(episodeRepository.findById(1L)).thenReturn(Optional.of(episode));

        EpisodeResponse response = episodeService.findById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEpisodeNumber()).isEqualTo(1);
    }

    @Test
    void findById_ShouldThrow_WhenNotFound() {
        when(episodeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> episodeService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_ShouldSaveEpisode_WhenValid() {
        EpisodeRequest request = new EpisodeRequest();
        request.setTitle("Episode 1");
        request.setEpisodeNumber(1);
        request.setSeasonNumber(1);

        when(animeRepository.findById(1L)).thenReturn(Optional.of(anime));
        when(episodeRepository.existsByAnimeIdAndSeasonNumberAndEpisodeNumber(1L, 1, 1)).thenReturn(false);
        when(episodeRepository.save(any(Episode.class))).thenReturn(episode);

        EpisodeResponse response = episodeService.create(1L, request);

        verify(episodeRepository).save(any(Episode.class));
        assertThat(response).isNotNull();
    }

    @Test
    void create_ShouldThrow_WhenEpisodeAlreadyExists() {
        EpisodeRequest request = new EpisodeRequest();
        request.setTitle("Episode 1");
        request.setEpisodeNumber(1);
        request.setSeasonNumber(1);

        when(animeRepository.findById(1L)).thenReturn(Optional.of(anime));
        when(episodeRepository.existsByAnimeIdAndSeasonNumberAndEpisodeNumber(1L, 1, 1)).thenReturn(true);

        assertThatThrownBy(() -> episodeService.create(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void create_ShouldThrow_WhenAnimeNotFound() {
        when(animeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> episodeService.create(99L, new EpisodeRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_ShouldDeleteEpisode_WhenExists() {
        when(episodeRepository.findById(1L)).thenReturn(Optional.of(episode));

        episodeService.delete(1L);

        verify(episodeRepository).delete(episode);
    }

    @Test
    void incrementViews_ShouldCallRepository() {
        episodeService.incrementViews(1L);

        verify(episodeRepository).incrementViews(1L);
    }
}
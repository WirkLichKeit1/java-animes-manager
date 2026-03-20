package com.animeapi.service;

import com.animeapi.exception.ResourceNotFoundException;
import com.animeapi.model.Episode;
import com.animeapi.model.VideoStatus;
import com.animeapi.repository.EpisodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço auxiliar separado do VideoService para corrigir o problema de
 * @Async + @Transactional no mesmo bean.
 *
 * Quando @Async e @Transactional estão no mesmo bean, a chamada interna
 * via "this.método()" não passa pelo proxy do Spring, então a transação
 * nunca é aberta. Ao separar em beans distintos, o proxy funciona corretamente.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoStatusUpdater {

    private final EpisodeRepository episodeRepository;

    @Transactional
    public void markReady(Long episodeId, String filename) {
        Episode episode = episodeRepository.findById(episodeId)
                .orElseThrow(() -> new ResourceNotFoundException("Episode", episodeId));
        episode.setVideoStatus(VideoStatus.READY);
        episodeRepository.save(episode);
        log.info("Video processed successfully for episode {}: {}", episodeId, filename);
    }

    @Transactional
    public void markError(Long episodeId) {
        episodeRepository.findById(episodeId).ifPresent(episode -> {
            episode.setVideoStatus(VideoStatus.ERROR);
            episodeRepository.save(episode);
            log.warn("Video marked as ERROR for episode {}", episodeId);
        });
    }
}
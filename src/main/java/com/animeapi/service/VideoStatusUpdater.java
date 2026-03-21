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
 * Serviço auxiliar separado do VideoService para que @Async + @Transactional
 * funcionem corretamente (proxies Spring distintos).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoStatusUpdater {

    private final EpisodeRepository episodeRepository;

    /**
     * Salva o filename do vídeo e marca o episódio como READY.
     * Chamado após o upload assíncrono para o Cloudinary ser concluído.
     */
    @Transactional
    public void markReadyWithFilename(Long episodeId, String filename) {
        Episode episode = episodeRepository.findById(episodeId)
                .orElseThrow(() -> new ResourceNotFoundException("Episode", episodeId));
        episode.setVideoFilename(filename);
        episode.setVideoStatus(VideoStatus.READY);
        episodeRepository.save(episode);
        log.info("Video ready for episode {}: {}", episodeId, filename);
    }

    /**
     * Mantido para compatibilidade — usado quando o upload falha
     * e não há filename para salvar.
     */
    @Transactional
    public void markReady(Long episodeId, String filename) {
        markReadyWithFilename(episodeId, filename);
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
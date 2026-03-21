package com.animeapi.repository;

import com.animeapi.model.Episode;
import com.animeapi.model.VideoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EpisodeRepository extends JpaRepository<Episode, Long> {

    List<Episode> findByAnimeIdAndPublishedTrueOrderBySeasonNumberAscEpisodeNumberAsc(Long animeId);

    Page<Episode> findByAnimeId(Long animeId, Pageable pageable);

    Optional<Episode> findByAnimeIdAndSeasonNumberAndEpisodeNumber(
        Long animeId, Integer season, Integer episode
    );

    boolean existsByAnimeIdAndSeasonNumberAndEpisodeNumber(
        Long animeId, Integer season, Integer episode
    );

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Episode e SET e.views = e.views + 1 WHERE e.id = :id")
    void incrementViews(@Param("id") Long id);

    List<Episode> findByVideoStatus(VideoStatus status);

    /**
     * Carrega o Episode junto com o Anime em uma única query (JOIN FETCH).
     *
     * Use este método em qualquer lugar que precise acessar episode.getAnime()
     * fora de uma sessão JPA ativa — especialmente em toResponse() nos services
     * WatchHistoryService e EpisodeService.
     *
     * Substitui o findById() padrão nesses contextos para evitar
     * LazyInitializationException ao acessar anime.getTitle(), anime.getId(), etc.
     */
    @Query("SELECT e FROM Episode e JOIN FETCH e.anime WHERE e.id = :id")
    Optional<Episode> findByIdWithAnime(@Param("id") Long id);
}
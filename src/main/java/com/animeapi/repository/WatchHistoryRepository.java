package com.animeapi.repository;

import com.animeapi.model.WatchHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WatchHistoryRepository extends JpaRepository<WatchHistory, Long> {

    /**
     * Carrega WatchHistory junto com Episode e Anime em uma única query.
     * Evita LazyInitializationException ao acessar episode.getAnime().getTitle()
     * fora de uma sessão JPA ativa (ex: em toResponse() após o commit).
     */
    @Query("""
        SELECT wh FROM WatchHistory wh
        JOIN FETCH wh.episode e
        JOIN FETCH e.anime
        WHERE wh.user.id = :userId AND wh.episode.id = :episodeId
    """)
    Optional<WatchHistory> findByUserIdAndEpisodeId(
        @Param("userId") Long userId,
        @Param("episodeId") Long episodeId
    );

    /**
     * Versão paginada com JOIN FETCH.
     *
     * IMPORTANTE: JOIN FETCH com paginação em Hibernate gera um WARN
     * "HHH90003004: firstResult/maxResults specified with collection fetch"
     * porque o Hibernate carrega tudo em memória e pagina em Java.
     * Para evitar isso, a query de contagem é separada via countQuery.
     *
     * A query principal usa DISTINCT para evitar duplicatas causadas pelo JOIN FETCH.
     */
    @Query(
        value = """
            SELECT DISTINCT wh FROM WatchHistory wh
            JOIN FETCH wh.episode e
            JOIN FETCH e.anime
            WHERE wh.user.id = :userId
            ORDER BY wh.watchedAt DESC
        """,
        countQuery = """
            SELECT COUNT(wh) FROM WatchHistory wh
            WHERE wh.user.id = :userId
        """
    )
    Page<WatchHistory> findByUserIdWithEpisodeAndAnime(
        @Param("userId") Long userId,
        Pageable pageable
    );
}
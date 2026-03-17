package com.animeapi.repository;

import com.animeapi.model.Anime;
import com.animeapi.model.AnimeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnimeRepository extends JpaRepository<Anime, Long> {
    Page<Anime> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Page<Anime> findByGenreIgnoreCase(String genre, Pageable pageable);

    Page<Anime> findByStatus(AnimeStatus status, Pageable pageable);

    @Query("""
        SELECT DISTINCT a.genre FROM Anime a
        WHERE a.genre IS NOT NULL,
        ORDER BY a.genre
    """)
    List<String> findAllGenres();

    @Query("""
        SELECT a FROM Anime a
        WHERE (:title IS NULL OR LOWER(a.title) LIKE LOWER(CONCAT('%', :title, '%')))
            AND (:genre IS NULL OR LOWER(a.genre) = LOWER(:genre))
            AND (:status IS NULL OR a.status = :status)
            AND (:year IS NULL OR a.releaseYear = :year)
    """)
    Page<Anime> search(
        @Param("title") String title,
        @Param("genre") String genre,
        @Param("status") AnimeStatus status,
        @Param("year") Integer year,
        Pageable pageable
    );
}
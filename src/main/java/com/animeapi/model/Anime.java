package com.animeapi.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "animes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Anime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "original_title", length = 255)
    private String originalTitle;

    @Column(columnDefinition = "TEXT")
    private String synopsis;

    @Column(length = 100)
    private String genre;

    @Column(length = 100)
    private String studio;

    @Column(name = "release_year")
    private Integer releaseYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private AnimeStatus status = AnimeStatus.ONGOING;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(name = "banner_image_url", length = 500)
    private String bannerImageUrl;

    @Column(precision = 3, scale = 1)
    private BigDecimal rating;

    @OneToMany(mappedBy = "anime", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("seasonNumber ASC, episodeNumber ASC")
    @Builder.Default
    private List<Episode> episodes = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
        @Builder.Default
        private LocalDateTime createdAt = LocalDateTime.now();

        @Column(name = "updated_at", nullable = false)
        @Builder.Default
        private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void onUodate() {
        this.updatedAt = LocalDateTime.now();
    }
}
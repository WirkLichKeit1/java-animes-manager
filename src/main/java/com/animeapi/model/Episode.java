package com.animeapi.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "episodes",
    uniqueConstraints = @UniqueConstraint(columnNames = {
        "anime_id",
        "season_number",
        "episode_number"
    })
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Episode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anime_id", nullable = false)
    private Anime anime;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "episode_number", nullable = false)
    private Integer episodeNumber;

    @Column(name = "season_number", nullable = false)
    @Builder.Default
    private Integer seasonNumber = 1;

    @Column(columnDefinition = "TEXT")
    private String synopsis;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "video_filename", length = 500)
    private String videoFilename;

    @Enumerated(EnumType.STRING)
    @Column(name = "video_status", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    @Builder.Default
    private VideoStatus videoStatus = VideoStatus.PROCESSING;

    @Column(nullable = false)
    @Builder.Default
    private Long views = 0L;

    @Column(nullable = false)
    @Builder.Default
    private boolean published = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
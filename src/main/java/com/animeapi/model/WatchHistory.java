package com.animeapi.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "watch_history",
    uniqueConstraints = @UniqueConstraint(columnNames = {
        "user_id",
        "episode_id"
    })
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WatchHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "episode_id", nullable = false)
    private Episode episode;

    @Column(name = "progress_seconds", nullable = false)
    @Builder.Default
    private Integer progressSeconds = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean completed = false;

    @Column(name = "watched_at", nullable = false)
    @Builder.Default
    private LocalDateTime watchedAt = LocalDateTime.now();
}
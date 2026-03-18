package com.animeapi.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WatchHistoryResponse {
    private Long id;
    private Long episodeId;
    private String episodeTitle;
    private Integer episodeNumber;
    private Integer seasonNumber;
    private Long animeId;
    private String animeTitle;
    private String animeCoverImageUrl;
    private Integer progressSeconds;
    private boolean completed;
    private LocalDateTime watchedAt;
}
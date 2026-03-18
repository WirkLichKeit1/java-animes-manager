package com.animeapi.dto.response;

import com.animeapi.model.VideoStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EpisodeResponse {
    private Long id;
    private Long animeId;
    private String animeTitle;
    private String title;
    private Integer episodeNumber;
    private Integer seasonNumber;
    private String synopsis;
    private Integer durationSeconds;
    private String thumbnailUrl;
    private String streamUrl;
    private VideoStatus videoStatus;
    private Long views;
    private boolean published;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
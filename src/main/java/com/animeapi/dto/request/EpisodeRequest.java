package com.animeapi.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class EpisodeRequest {
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @NotNull(message = "Episode number is required")
    @Min(value = 1, message = "Episode number must be at least 1")
    private Integer episodeNumber;

    @Min(value = 1, message = "Season number must be at least 1")
    private Integer seasonNumber = 1;

    private String synopsis;

    @Min(value = 1, message = "Duration must be at least 1 second")
    private Integer durationSeconds;

    private boolean published = false;
}
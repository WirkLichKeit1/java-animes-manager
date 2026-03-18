package com.animeapi.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WatchProgressRequest {
    @NotNull(message = "Episode ID is required")
    private Long episodeId;

    @NotNull(message = "Progress is required")
    @Min(value = 0, message = "Progress must be at least 0")
    private Integer progressSeconds;

    private boolean completed = false;
}
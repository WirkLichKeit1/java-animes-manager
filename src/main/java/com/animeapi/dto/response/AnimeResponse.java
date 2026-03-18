package com.animeapi.dto.response;

import com.animeapi.model.AnimeStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AnimeResponse {
    private Long id;
    private String title;
    private String originalTitle;
    private String synopsis;
    private String genre;
    private String studio;
    private Integer releaseYear;
    private AnimeStatus status;
    private String coverImageUrl;
    private String bannerImageUrl;
    private BigDecimal rating;
    private int totalEpisodes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
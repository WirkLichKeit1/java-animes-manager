package com.animeapi.dto.request;

import com.animeapi.model.AnimeStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AnimeRequest {
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @Size(max = 255, message = "Original title must not exceed 255 characters")
    private String originalTitle;

    private String synopsis;

    @Size(max = 100, message = "Genre must not exceed 100 characters")
    private String genre;

    @Size(max = 100, message = "Studio must not exceed 100 characters")
    private String studio;

    @Min(value = 1900, message = "Release year must be after 1900")
    @Max(value = 2100, message = "Release year must be before 2100")
    private Integer releaseYear;

    private AnimeStatus status;

    @DecimalMin(value = "0.0", message = "Rating must be at least 0.0")
    @DecimalMax(value = "10.0", message = "Rating must not exceed 10.0")
    private BigDecimal rating;
}
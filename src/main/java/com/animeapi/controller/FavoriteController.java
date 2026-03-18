package com.animeapi.controller;

import com.animeapi.dto.response.AnimeResponse;
import com.animeapi.dto.response.PageResponse;
import com.animeapi.model.User;
import com.animeapi.service.AuthService;
import com.animeapi.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final AuthService authService;

    @GetMapping
    public ResponseEntity<PageResponse<AnimeResponse>> getFavorites(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        User user = authService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(favoriteService.getFavorites(user,
                PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @PostMapping("/{animeId}")
    public ResponseEntity<Void> addFavorite(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long animeId
    ) {
        User user = authService.getCurrentUser(userDetails.getUsername());
        favoriteService.addFavorite(user, animeId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{animeId}")
    public ResponseEntity<Void> removeFavorite(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long animeId
    ) {
        User user = authService.getCurrentUser(userDetails.getUsername());
        favoriteService.removeFavorite(user, animeId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{animeId}/check")
    public ResponseEntity<Boolean> isFavorite(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long animeId
    ) {
        User user = authService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(favoriteService.isFavorite(user, animeId));
    }
}
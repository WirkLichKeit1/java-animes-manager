package com.animeapi.controller;

import com.animeapi.dto.request.WatchProgressRequest;
import com.animeapi.dto.response.PageResponse;
import com.animeapi.dto.response.WatchHistoryResponse;
import com.animeapi.model.User;
import com.animeapi.service.AuthService;
import com.animeapi.service.WatchHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class WatchHistoryController {

    private final WatchHistoryService watchHistoryService;
    private final AuthService authService;

    @PostMapping
    public ResponseEntity<WatchHistoryResponse> saveProgress(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid WatchProgressRequest request
    ) {
        User user = authService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(watchHistoryService.saveProgress(user, request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<WatchHistoryResponse>> getHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        User user = authService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(watchHistoryService.getHistory(user,
                PageRequest.of(page, size, Sort.by("watchedAt").descending())));
    }
}
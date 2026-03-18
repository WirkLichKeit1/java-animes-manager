package com.animeapi.controller;

import com.animeapi.service.EpisodeService;
import com.animeapi.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;
    private final EpisodeService episodeService;

    @GetMapping("/stream/{episodeId}")
    public ResponseEntity<byte[]> stream(
            @PathVariable Long episodeId,
            @RequestHeader(value = "Range", required = false) String rangeHeader
    ) {
        episodeService.incrementViews(episodeId);
        return videoService.streamVideo(episodeId, rangeHeader);
    }
}
package com.animeapi.controller;

import com.animeapi.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    @GetMapping("/stream/{episodeId}")
    public ResponseEntity<StreamingResponseBody> stream(
            @PathVariable Long episodeId,
            @RequestHeader(value = "Range", required = false) String rangeHeader
    ) {
        return videoService.streamVideo(episodeId, rangeHeader);
    }
}
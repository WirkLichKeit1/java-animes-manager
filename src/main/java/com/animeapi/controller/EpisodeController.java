package com.animeapi.controller;

import com.animeapi.dto.request.EpisodeRequest;
import com.animeapi.dto.request.VideoUploadConfirmRequest;
import com.animeapi.dto.response.EpisodeResponse;
import com.animeapi.dto.response.PageResponse;
import com.animeapi.dto.response.VideoUploadSignatureResponse;
import com.animeapi.service.EpisodeService;
import com.animeapi.service.VideoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/animes/{animeId}/episodes")
@RequiredArgsConstructor
public class EpisodeController {

    private final EpisodeService episodeService;
    private final VideoService videoService;

    @GetMapping
    public ResponseEntity<List<EpisodeResponse>> findByAnime(@PathVariable Long animeId) {
        return ResponseEntity.ok(episodeService.findByAnime(animeId));
    }

    @GetMapping("/paged")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<EpisodeResponse>> findByAnimePaged(
            @PathVariable Long animeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(episodeService.findByAnimePaged(animeId,
                PageRequest.of(page, size, Sort.by("seasonNumber", "episodeNumber").ascending())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EpisodeResponse> findById(
            @PathVariable Long animeId,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(episodeService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EpisodeResponse> create(
            @PathVariable Long animeId,
            @RequestBody @Valid EpisodeRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(episodeService.create(animeId, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EpisodeResponse> update(
            @PathVariable Long animeId,
            @PathVariable Long id,
            @RequestBody @Valid EpisodeRequest request
    ) {
        return ResponseEntity.ok(episodeService.update(id, request));
    }

    @PostMapping(value = "/{id}/thumbnail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> uploadThumbnail(
            @PathVariable Long animeId,
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        episodeService.uploadThumbnail(id, file);
        return ResponseEntity.noContent().build();
    }

    /**
     * Gera uma assinatura para upload direto do vídeo ao Cloudinary pelo frontend.
     * O arquivo nunca passa pelo servidor — o Render fica completamente fora do caminho.
     */
    @PostMapping("/{id}/video-signature")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VideoUploadSignatureResponse> getVideoUploadSignature(
            @PathVariable Long animeId,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(videoService.generateUploadSignature(id));
    }

    /**
     * Chamado pelo frontend após o upload direto ao Cloudinary ser concluído.
     * Salva o publicId e marca o vídeo como READY.
     */
    @PostMapping("/{id}/video-confirm")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> confirmVideoUpload(
            @PathVariable Long animeId,
            @PathVariable Long id,
            @RequestBody @Valid VideoUploadConfirmRequest request
    ) {
        videoService.confirmUpload(id, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable Long animeId,
            @PathVariable Long id
    ) {
        episodeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
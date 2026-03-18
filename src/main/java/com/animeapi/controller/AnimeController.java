package com.animeapi.controller;

import com.animeapi.dto.request.AnimeRequest;
import com.animeapi.dto.response.AnimeResponse;
import com.animeapi.dto.response.PageResponse;
import com.animeapi.model.AnimeStatus;
import com.animeapi.service.AnimeService;
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
@RequestMapping("/api/animes")
@RequiredArgsConstructor
public class AnimeController {

    private final AnimeService animeService;

    @GetMapping
    public ResponseEntity<PageResponse<AnimeResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        return ResponseEntity.ok(animeService.findAll(PageRequest.of(page, size, sort)));
    }

    @GetMapping("/search")
    public ResponseEntity<PageResponse<AnimeResponse>> search(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) AnimeStatus status,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(animeService.search(title, genre, status, year,
                PageRequest.of(page, size, Sort.by("title").ascending())));
    }

    @GetMapping("/genres")
    public ResponseEntity<List<String>> findAllGenres() {
        return ResponseEntity.ok(animeService.findAllGenres());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AnimeResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(animeService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AnimeResponse> create(@RequestBody @Valid AnimeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(animeService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AnimeResponse> update(
            @PathVariable Long id,
            @RequestBody @Valid AnimeRequest request
    ) {
        return ResponseEntity.ok(animeService.update(id, request));
    }

    @PostMapping(value = "/{id}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> uploadCover(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        animeService.uploadCover(id, file, false);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/banner", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> uploadBanner(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        animeService.uploadCover(id, file, true);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        animeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
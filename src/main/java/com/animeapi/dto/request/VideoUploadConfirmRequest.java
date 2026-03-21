package com.animeapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Enviado pelo frontend após o upload direto ao Cloudinary ser concluído.
 * O backend usa o publicId para salvar a referência no episódio e
 * marcar o vídeo como READY.
 */
@Data
public class VideoUploadConfirmRequest {
    @NotBlank(message = "publicId is required")
    private String publicId;
}
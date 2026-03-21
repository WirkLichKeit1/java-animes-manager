package com.animeapi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Resposta do endpoint de assinatura para upload direto ao Cloudinary.
 *
 * O frontend usa esses dados para fazer um POST diretamente para
 * https://api.cloudinary.com/v1_1/{cloudName}/video/upload
 * sem passar pelo servidor — o Render nunca recebe o binário do vídeo.
 */
@Data
@AllArgsConstructor
public class VideoUploadSignatureResponse {
    /** Assinatura HMAC-SHA256 dos parâmetros, gerada pelo backend com api_secret */
    private String signature;

    /** Unix timestamp usado na geração da assinatura (deve ser <= 1h) */
    private long timestamp;

    /** Cloudinary API key (pública, pode ser exposta ao frontend) */
    private String apiKey;

    /** Nome do cloud Cloudinary */
    private String cloudName;

    /**
     * public_id reservado para este upload.
     * O frontend deve enviar exatamente este valor ao Cloudinary para que
     * o backend saiba qual arquivo confirmar depois.
     */
    private String publicId;
}
package com.animeapi.service;

import com.animeapi.dto.response.VideoUploadSignatureResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Path;

public interface StorageService {

    /**
     * Salva um arquivo a partir de um MultipartFile e retorna a chave/path gerado.
     */
    String store(MultipartFile file, String directory);

    /**
     * Salva um arquivo a partir de um Path no disco (arquivo temporário).
     */
    String storeFromPath(Path tempFile, String directory, String originalFilename);

    /**
     * Gera uma assinatura para upload direto pelo cliente (frontend → Cloudinary).
     * Retorna os parâmetros necessários para o frontend fazer o POST diretamente
     * para a API do Cloudinary sem passar pelo servidor.
     *
     * Implementações de storage local podem lançar UnsupportedOperationException,
     * pois o upload direto só faz sentido com storage remoto.
     */
    VideoUploadSignatureResponse generateUploadSignature(String directory);

    /**
     * Retorna a URL pública de acesso ao arquivo.
     */
    String getUrl(String key);

    /**
     * Retorna um InputStream do arquivo para streaming.
     */
    InputStream load(String key);

    /**
     * Retorna o tamanho em bytes do arquivo.
     */
    long getFileSize(String key);

    /**
     * Deleta um arquivo.
     */
    void delete(String key);

    /**
     * Verifica se o arquivo existe.
     */
    boolean exists(String key);
}
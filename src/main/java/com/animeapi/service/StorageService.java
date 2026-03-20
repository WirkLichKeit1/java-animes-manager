package com.animeapi.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface StorageService {

    /**
     * Salva um arquivo e retorna a chave/path gerado.
     */
    String store(MultipartFile file, String directory);

    /**
     * Retorna a URL pública de acesso ao arquivo.
     * Para armazenamento local, retorna o path relativo (servido pelo backend).
     * Para Cloudinary, retorna a URL CDN completa.
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
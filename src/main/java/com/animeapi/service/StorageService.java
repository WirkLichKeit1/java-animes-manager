package com.animeapi.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface StorageService {
    /**
    * Salva um arquivo e retorna o nome do arquivo gerado.
    */
    String store(MultipartFile file, String directory);

    /**
    * Retorna um InputStream do arquivo para streaming.
    */
    InputStream load(String filename);

    /**
    * Retorna o tamanho em bytes do arquivo.
    */
    long getFileSize(String filename);

    /**
    * Deleta um arquivo.
    */
    void delete(String filename);

    /**
    * Verifica se o arquivo existe.
    */
    boolean exists(String filename);
}
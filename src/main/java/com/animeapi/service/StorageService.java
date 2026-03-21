package com.animeapi.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Path;

public interface StorageService {

    /**
     * Salva um arquivo a partir de um MultipartFile e retorna a chave/path gerado.
     * Para arquivos grandes, prefira storeFromPath() para evitar OOM.
     */
    String store(MultipartFile file, String directory);

    /**
     * Salva um arquivo a partir de um Path no disco (arquivo temporário).
     * Não carrega o arquivo inteiro na heap — usa stream diretamente.
     * Indicado para vídeos e outros arquivos grandes.
     *
     * @param tempFile        caminho do arquivo temporário no disco
     * @param directory       subdiretório de destino (ex: "videos", "images")
     * @param originalFilename nome original do arquivo (usado para determinar extensão/tipo)
     * @return chave/publicId do arquivo salvo
     */
    String storeFromPath(Path tempFile, String directory, String originalFilename);

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
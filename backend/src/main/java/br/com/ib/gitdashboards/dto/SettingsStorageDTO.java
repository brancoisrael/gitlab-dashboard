package br.com.ib.gitdashboards.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Contrato de API para leitura e escrita da configuração de armazenamento em tempo de execução.
 *
 * @param type       tipo de armazenamento: {@code "local"} ou {@code "s3"}
 * @param localPath  caminho do diretório local (usado quando {@code type=local})
 * @param endpoint   endpoint S3/MinIO
 * @param bucket     nome do bucket
 * @param region     região AWS/MinIO (padrão {@code us-east-1})
 * @param accessKey  access key S3/MinIO
 * @param secretKey  secret key S3/MinIO; {@code "***"} indica valor mascarado (não alterar)
 */
public record SettingsStorageDTO(
    String type,
    @JsonProperty("localPath")  String localPath,
    String endpoint,
    String bucket,
    String region,
    @JsonProperty("accessKey")  String accessKey,
    @JsonProperty("secretKey")  String secretKey
) {}

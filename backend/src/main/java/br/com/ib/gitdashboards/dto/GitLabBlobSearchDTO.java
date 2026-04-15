package br.com.ib.gitdashboards.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Representa um item retornado pela API de busca em blobs do GitLab.
 * Endpoint: {@code GET /api/v4/projects/{id}/search?scope=blobs}
 *
 * @param filename  nome do arquivo onde o termo foi encontrado
 * @param path      caminho completo do arquivo no repositório
 * @param ref       branch ou ref onde o arquivo reside
 * @param projectId ID do projeto ao qual o arquivo pertence
 */
public record GitLabBlobSearchDTO(
    String filename,
    String path,
    String ref,
    @JsonProperty("project_id") Long projectId
) {}

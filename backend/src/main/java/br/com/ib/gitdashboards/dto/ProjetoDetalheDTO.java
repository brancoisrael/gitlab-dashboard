package br.com.ib.gitdashboards.dto;

/**
 * Detalhes de um projeto GitLab para exibição no dashboard.
 *
 * @param nome            nome simples do projeto
 * @param webUrl          URL web do projeto no GitLab
 * @param criadoEm        data de criação em formato ISO 8601 (string)
 * @param ultimaAtividade data da última atividade em formato ISO 8601 (string)
 */
public record ProjetoDetalheDTO(
    String nome,
    String webUrl,
    String criadoEm,
    String ultimaAtividade
) {}

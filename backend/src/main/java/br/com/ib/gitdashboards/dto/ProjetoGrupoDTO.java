package br.com.ib.gitdashboards.dto;

/**
 * Resultado do agrupamento de projetos por namespace de primeiro nível.
 *
 * @param grupo nome do grupo extraído de {@code name_with_namespace} (parte antes do " / ")
 * @param count quantidade de projetos no grupo
 */
public record ProjetoGrupoDTO(String grupo, long count) {}

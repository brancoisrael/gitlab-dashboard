package br.com.ib.gitdashboards.dto;

/**
 * Resumo mínimo de um projeto GitLab lido do Parquet local.
 * Usado pelo {@code DependencySyncService} para iterar projetos sem re-consultar a API GitLab.
 *
 * @param id            ID numérico do projeto no GitLab
 * @param name          nome simples do projeto
 * @param defaultBranch branch padrão (pode ser {@code null}; usar {@code "HEAD"} como fallback)
 * @param webUrl        URL web do projeto no GitLab (ex.: {@code https://gitlab.com/group/repo})
 */
public record ProjetoResumoDTO(Long id, String name, String defaultBranch, String webUrl) {}

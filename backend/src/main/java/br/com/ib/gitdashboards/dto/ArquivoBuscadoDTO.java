package br.com.ib.gitdashboards.dto;

/**
 * Representa um arquivo encontrado durante a busca de conteúdo nos projetos GitLab.
 *
 * @param nomeArquivo nome do arquivo (ex.: {@code MyService.java})
 * @param urlArquivo  URL direta para o arquivo no GitLab (ex.: {@code https://gitlab.com/group/repo/-/blob/main/src/MyService.java})
 */
public record ArquivoBuscadoDTO(String nomeArquivo, String urlArquivo) {}

package br.com.ib.gitdashboards.dto;

import java.util.List;

/**
 * Agrupa os arquivos encontrados em um projeto durante a busca de conteúdo.
 *
 * @param nomeProjeto nome do projeto GitLab
 * @param arquivos    lista de arquivos que contêm o termo buscado com a extensão informada
 */
public record ProjetoBuscaDTO(String nomeProjeto, List<ArquivoBuscadoDTO> arquivos) {}

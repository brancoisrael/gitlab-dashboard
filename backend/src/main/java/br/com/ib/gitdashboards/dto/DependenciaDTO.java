package br.com.ib.gitdashboards.dto;

/**
 * Representa uma dependência extraída de um arquivo de gerenciamento (pom.xml, package.json, etc.).
 * Todos os campos estão presentes no schema Parquet independentemente da linguagem,
 * garantindo equidade entre arquivos gerados para diferentes ecossistemas.
 *
 * @param aplicacao     nome do projeto GitLab de origem
 * @param dependencia   nome da biblioteca/pacote
 * @param versao        versão declarada (pode ser {@code null} quando não especificada)
 * @param pacote        namespace, groupId ou escopo (pode ser {@code null} quando não aplicável)
 * @param gerenciador   ferramenta gerenciadora (ex.: {@code "maven"}, {@code "npm"}, {@code "pip"})
 * @param arquivoOrigem nome do arquivo de onde a dependência foi extraída (ex.: {@code "pom.xml"})
 */
public record DependenciaDTO(
    String aplicacao,
    String dependencia,
    String versao,
    String pacote,
    String gerenciador,
    String arquivoOrigem
) {}

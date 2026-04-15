package br.com.ib.gitdashboards.dto;

import java.util.List;

/**
 * Resultado de uma consulta ao arquivo Parquet de dependências.
 *
 * @param tipo  {@code "projeto"} ou {@code "dependencia"}
 * @param nome  nome do projeto ou da dependência consultada
 * @param lista quando {@code tipo="projeto"}: itens com as dependências do projeto e suas versões;
 *              quando {@code tipo="dependencia"}: itens com os projetos que utilizam a dependência
 *              e a versão declarada em cada um
 */
public record DependenciaConsultaDTO(String tipo, String nome, List<DependenciaItemDTO> lista) {}

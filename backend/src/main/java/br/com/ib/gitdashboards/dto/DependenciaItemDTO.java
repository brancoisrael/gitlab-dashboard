package br.com.ib.gitdashboards.dto;

/**
 * Item de uma lista de dependências ou projetos em uma consulta.
 *
 * @param nome   nome da dependência (quando {@code tipo="projeto"}) ou do projeto
 *               (quando {@code tipo="dependencia"})
 * @param versao versão da dependência declarada no projeto; {@code null} quando não informada
 */
public record DependenciaItemDTO(String nome, String versao) {}

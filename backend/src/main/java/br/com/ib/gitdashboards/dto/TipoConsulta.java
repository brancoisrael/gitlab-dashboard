package br.com.ib.gitdashboards.dto;

import java.util.Optional;

/**
 * Tipo de filtro para o endpoint de consulta de dependências.
 * <ul>
 *   <li>{@link #PROJETO} — filtra pelo nome do projeto, retorna suas dependências.</li>
 *   <li>{@link #DEPENDENCIA} — filtra pelo nome da dependência, retorna os projetos que a utilizam.</li>
 * </ul>
 */
public enum TipoConsulta {

    PROJETO,
    DEPENDENCIA;

    /**
     * Converte uma string vinda de query param para o enum, aceitando valores
     * em qualquer caixa e sem acento (ex.: {@code "projeto"}, {@code "DEPENDENCIA"}).
     *
     * @return {@link Optional#empty()} se o valor não corresponder a nenhuma constante
     */
    public static Optional<TipoConsulta> parse(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        return switch (value.trim().toLowerCase()) {
            case "projeto"               -> Optional.of(PROJETO);
            case "dependencia",
                 "dependência"           -> Optional.of(DEPENDENCIA);
            default                      -> Optional.empty();
        };
    }
}

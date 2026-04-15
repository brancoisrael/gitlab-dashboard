package br.com.ib.gitdashboards.parser;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.Optional;

/**
 * Localiza o {@link DependencyParser} adequado para um dado arquivo.
 *
 * <p>Todos os beans CDI que implementam {@link DependencyParser} são descobertos
 * automaticamente — nenhuma alteração aqui é necessária ao adicionar novos parsers.
 */
@ApplicationScoped
public class DependencyParserRegistry {

    private final Instance<DependencyParser> parsers;

    @Inject
    public DependencyParserRegistry(Instance<DependencyParser> parsers) {
        this.parsers = parsers;
    }

    /**
     * Retorna o parser capaz de processar o arquivo informado.
     *
     * @param nomeArquivo nome do arquivo (ex.: {@code pom.xml})
     * @return parser correspondente, ou {@link Optional#empty()} se não houver suporte
     */
    public Optional<DependencyParser> encontrar(String nomeArquivo) {
        return parsers.stream()
            .filter(p -> p.suporta(nomeArquivo))
            .findFirst();
    }
}

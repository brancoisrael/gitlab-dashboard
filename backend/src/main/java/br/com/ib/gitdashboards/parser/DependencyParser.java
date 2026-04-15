package br.com.ib.gitdashboards.parser;

import br.com.ib.gitdashboards.dto.DependenciaDTO;

import java.util.List;

/**
 * Contrato para extração de dependências de um arquivo de gerenciamento de pacotes.
 *
 * <p>Cada implementação cobre um ecossistema específico (Maven, npm, pip, etc.).
 * Para adicionar suporte a uma nova linguagem, basta criar uma classe anotada com
 * {@code @ApplicationScoped} que implemente esta interface — o
 * {@link DependencyParserRegistry} a descobrirá automaticamente via CDI.
 */
public interface DependencyParser {

    /**
     * Indica se este parser suporta o arquivo informado.
     *
     * @param nomeArquivo nome do arquivo (ex.: {@code pom.xml}, {@code package.json})
     * @return {@code true} se este parser consegue processar o arquivo
     */
    boolean suporta(String nomeArquivo);

    /**
     * Extrai as dependências do conteúdo do arquivo.
     * Nunca lança exceção — erros de parsing são logados e resultam em lista vazia ou parcial.
     *
     * @param nomeAplicacao nome do projeto GitLab de origem (usado para preencher {@code aplicacao})
     * @param conteudo      conteúdo textual do arquivo
     * @return lista de dependências encontradas; pode ser vazia, nunca {@code null}
     */
    List<DependenciaDTO> parsear(String nomeAplicacao, String conteudo);
}

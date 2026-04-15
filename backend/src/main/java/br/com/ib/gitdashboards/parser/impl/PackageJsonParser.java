package br.com.ib.gitdashboards.parser.impl;

import br.com.ib.gitdashboards.dto.DependenciaDTO;
import br.com.ib.gitdashboards.parser.DependencyParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Parser de dependências para arquivos npm/yarn {@code package.json}.
 * Processa as seções {@code dependencies} e {@code devDependencies}.
 */
@ApplicationScoped
public class PackageJsonParser implements DependencyParser {

    private static final Logger log = Logger.getLogger(PackageJsonParser.class);

    private final ObjectMapper mapper;

    @Inject
    public PackageJsonParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean suporta(String nomeArquivo) {
        return "package.json".equalsIgnoreCase(nomeArquivo);
    }

    @Override
    public List<DependenciaDTO> parsear(String nomeAplicacao, String conteudo) {
        var result = new ArrayList<DependenciaDTO>();
        try {
            var root = mapper.readTree(conteudo);
            extrairSecao(root, "dependencies",    nomeAplicacao, result);
            extrairSecao(root, "devDependencies", nomeAplicacao, result);
        } catch (Exception e) {
            log.warnf("Erro ao parsear package.json do projeto '%s': %s", nomeAplicacao, e.getMessage());
        }
        return result;
    }

    private void extrairSecao(JsonNode root, String secao, String nomeAplicacao, List<DependenciaDTO> result) {
        var node = root.get(secao);
        if (node == null || !node.isObject()) return;
        node.fields().forEachRemaining(entry ->
            result.add(new DependenciaDTO(
                nomeAplicacao, entry.getKey(), entry.getValue().asText(null), null, "npm", "package.json"
            ))
        );
    }
}

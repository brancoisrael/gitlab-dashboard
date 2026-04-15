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
 * Parser de dependências para arquivos PHP Composer {@code composer.json}.
 * Processa as seções {@code require} e {@code require-dev}.
 * O vendor (ex.: {@code laravel} em {@code laravel/framework}) é mapeado para {@code pacote}.
 */
@ApplicationScoped
public class ComposerJsonParser implements DependencyParser {

    private static final Logger log = Logger.getLogger(ComposerJsonParser.class);

    private final ObjectMapper mapper;

    @Inject
    public ComposerJsonParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean suporta(String nomeArquivo) {
        return "composer.json".equalsIgnoreCase(nomeArquivo);
    }

    @Override
    public List<DependenciaDTO> parsear(String nomeAplicacao, String conteudo) {
        var result = new ArrayList<DependenciaDTO>();
        try {
            var root = mapper.readTree(conteudo);
            extrairSecao(root, "require",     nomeAplicacao, result);
            extrairSecao(root, "require-dev", nomeAplicacao, result);
        } catch (Exception e) {
            log.warnf("Erro ao parsear composer.json do projeto '%s': %s", nomeAplicacao, e.getMessage());
        }
        return result;
    }

    private void extrairSecao(JsonNode root, String secao, String nomeAplicacao, List<DependenciaDTO> result) {
        var node = root.get(secao);
        if (node == null || !node.isObject()) return;
        node.fields().forEachRemaining(entry -> {
            var fullName  = entry.getKey();              // ex.: "laravel/framework" ou "php"
            var slashIdx  = fullName.lastIndexOf('/');
            var nome      = slashIdx >= 0 ? fullName.substring(slashIdx + 1) : fullName;
            var pacote    = slashIdx >= 0 ? fullName.substring(0, slashIdx) : null;
            var versao    = entry.getValue().asText(null);
            result.add(new DependenciaDTO(nomeAplicacao, nome, versao, pacote, "composer", "composer.json"));
        });
    }
}

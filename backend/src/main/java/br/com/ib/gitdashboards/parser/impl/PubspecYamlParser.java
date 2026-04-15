package br.com.ib.gitdashboards.parser.impl;

import br.com.ib.gitdashboards.dto.DependenciaDTO;
import br.com.ib.gitdashboards.parser.DependencyParser;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Parser de dependências para arquivos Flutter/Dart {@code pubspec.yaml}.
 * Processa as seções {@code dependencies} e {@code dev_dependencies}.
 * A versão é extraída quando declarada inline (ex.: {@code http: ^0.13.0}).
 * Dependências com sub-mapa (ex.: {@code sdk: flutter}) são registradas sem versão.
 */
@ApplicationScoped
public class PubspecYamlParser implements DependencyParser {

    private static final Logger log = Logger.getLogger(PubspecYamlParser.class);

    private static final Pattern SECTION_HEADER = Pattern.compile(
        "^(dependencies|dev_dependencies)\\s*:"
    );
    private static final Pattern DEP_LINE = Pattern.compile(
        "^  ([a-zA-Z0-9_-]+)\\s*:\\s*(.*)$"
    );

    @Override
    public boolean suporta(String nomeArquivo) {
        return "pubspec.yaml".equalsIgnoreCase(nomeArquivo)
            || "pubspec.yml".equalsIgnoreCase(nomeArquivo);
    }

    @Override
    public List<DependenciaDTO> parsear(String nomeAplicacao, String conteudo) {
        var result = new ArrayList<DependenciaDTO>();
        try {
            var emSecao = false;
            for (var linha : conteudo.lines().toList()) {
                if (SECTION_HEADER.matcher(linha).find()) {
                    emSecao = true;
                    continue;
                }
                if (emSecao) {
                    if (!linha.isEmpty() && !linha.startsWith(" ") && !linha.startsWith("\t")) {
                        emSecao = false;
                        continue;
                    }
                    var m = DEP_LINE.matcher(linha);
                    if (!m.matches()) continue;

                    var nome   = m.group(1);
                    var valor  = m.group(2).trim();
                    // Valor vazio ou que começa com '{' indica sub-mapa → sem versão inline
                    var versao = (!valor.isEmpty() && !valor.startsWith("{") && !valor.startsWith("#"))
                        ? valor : null;
                    result.add(new DependenciaDTO(nomeAplicacao, nome, versao, null, "pub", "pubspec.yaml"));
                }
            }
        } catch (Exception e) {
            log.warnf("Erro ao parsear pubspec.yaml do projeto '%s': %s", nomeAplicacao, e.getMessage());
        }
        return result;
    }
}

package br.com.ib.gitdashboards.parser.impl;

import br.com.ib.gitdashboards.dto.DependenciaDTO;
import br.com.ib.gitdashboards.parser.DependencyParser;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Parser de dependências para arquivos Python {@code pyproject.toml}.
 * Suporta dois formatos:
 * <ul>
 *   <li><b>Poetry:</b> seções {@code [tool.poetry.dependencies]} e {@code [tool.poetry.dev-dependencies]}</li>
 *   <li><b>PEP 621:</b> lista {@code dependencies} dentro de {@code [project]}</li>
 * </ul>
 */
@ApplicationScoped
public class PyprojectTomlParser implements DependencyParser {

    private static final Logger log = Logger.getLogger(PyprojectTomlParser.class);

    private static final Pattern POETRY_SECTION = Pattern.compile(
        "^\\[tool\\.poetry(?:\\.dev)?-?dependencies\\]"
    );
    private static final Pattern TOML_SECTION = Pattern.compile("^\\[");
    private static final Pattern POETRY_DEP   = Pattern.compile(
        "^([a-zA-Z0-9][a-zA-Z0-9._-]*)\\s*=\\s*[\"']?([^\"'\\s{,]+)"
    );
    private static final Pattern PEP621_BLOCK  = Pattern.compile(
        "\\[project\\].*?dependencies\\s*=\\s*\\[([^\\]]+)\\]", Pattern.DOTALL
    );
    private static final Pattern PEP621_DEP    = Pattern.compile(
        "\"([a-zA-Z0-9][a-zA-Z0-9._-]*)([><=!~][^\"]*)?\"" +
        "|'([a-zA-Z0-9][a-zA-Z0-9._-]*)([><=!~][^']*)?'"
    );

    @Override
    public boolean suporta(String nomeArquivo) {
        return "pyproject.toml".equalsIgnoreCase(nomeArquivo);
    }

    @Override
    public List<DependenciaDTO> parsear(String nomeAplicacao, String conteudo) {
        var result = new ArrayList<DependenciaDTO>();
        try {
            parsearPoetry(nomeAplicacao, conteudo, result);
            parsearPep621(nomeAplicacao, conteudo, result);
        } catch (Exception e) {
            log.warnf("Erro ao parsear pyproject.toml do projeto '%s': %s", nomeAplicacao, e.getMessage());
        }
        return result;
    }

    private void parsearPoetry(String nomeAplicacao, String conteudo, List<DependenciaDTO> result) {
        var emSecao = false;
        for (var linha : conteudo.lines().toList()) {
            var trimmed = linha.trim();
            if (POETRY_SECTION.matcher(trimmed).find()) {
                emSecao = true;
                continue;
            }
            if (emSecao && TOML_SECTION.matcher(trimmed).find()) {
                emSecao = false;
                continue;
            }
            if (emSecao && !trimmed.isEmpty() && !trimmed.startsWith("#")) {
                var m = POETRY_DEP.matcher(trimmed);
                if (m.find()) {
                    var versao = m.group(2);
                    result.add(new DependenciaDTO(
                        nomeAplicacao, m.group(1),
                        versao != null && !versao.isBlank() ? versao.trim() : null,
                        null, "poetry", "pyproject.toml"
                    ));
                }
            }
        }
    }

    private void parsearPep621(String nomeAplicacao, String conteudo, List<DependenciaDTO> result) {
        var m = PEP621_BLOCK.matcher(conteudo);
        if (!m.find()) return;
        var dm = PEP621_DEP.matcher(m.group(1));
        while (dm.find()) {
            var nome   = dm.group(1) != null ? dm.group(1) : dm.group(3);
            var versao = dm.group(2) != null ? dm.group(2).trim()
                       : dm.group(4) != null ? dm.group(4).trim() : null;
            result.add(new DependenciaDTO(
                nomeAplicacao, nome,
                versao != null && !versao.isBlank() ? versao : null,
                null, "pip", "pyproject.toml"
            ));
        }
    }
}

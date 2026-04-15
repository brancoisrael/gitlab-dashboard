package br.com.ib.gitdashboards.parser.impl;

import br.com.ib.gitdashboards.dto.DependenciaDTO;
import br.com.ib.gitdashboards.parser.DependencyParser;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Parser de dependências para arquivos Python {@code requirements.txt}.
 * Suporta os formatos mais comuns: {@code pacote==1.0}, {@code pacote>=1.0},
 * {@code pacote~=1.0}, {@code pacote[extra]>=1.0}.
 * Linhas em branco, comentários ({@code #}) e flags de opção ({@code -r}, {@code -c})
 * são ignorados.
 */
@ApplicationScoped
public class RequirementsTxtParser implements DependencyParser {

    private static final Logger log = Logger.getLogger(RequirementsTxtParser.class);

    private static final Pattern DEP_PATTERN = Pattern.compile(
        "^([a-zA-Z0-9]([a-zA-Z0-9._-]*[a-zA-Z0-9])?)(?:\\[[^\\]]*\\])?" +
        "\\s*([><=!~]{1,3}\\s*[^;\\s#,]+)?"
    );

    @Override
    public boolean suporta(String nomeArquivo) {
        return "requirements.txt".equalsIgnoreCase(nomeArquivo);
    }

    @Override
    public List<DependenciaDTO> parsear(String nomeAplicacao, String conteudo) {
        var result = new ArrayList<DependenciaDTO>();
        try {
            for (var linha : conteudo.lines().toList()) {
                var trimmed = linha.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("-")) continue;
                // Remove comentário inline
                var semComentario = trimmed.contains("#")
                    ? trimmed.substring(0, trimmed.indexOf('#')).trim()
                    : trimmed;
                var m = DEP_PATTERN.matcher(semComentario);
                if (m.find()) {
                    var nome   = m.group(1);
                    var versao = m.group(3) != null ? m.group(3).trim() : null;
                    result.add(new DependenciaDTO(nomeAplicacao, nome, versao, null, "pip", "requirements.txt"));
                }
            }
        } catch (Exception e) {
            log.warnf("Erro ao parsear requirements.txt do projeto '%s': %s", nomeAplicacao, e.getMessage());
        }
        return result;
    }
}

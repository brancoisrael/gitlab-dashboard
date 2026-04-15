package br.com.ib.gitdashboards.parser.impl;

import br.com.ib.gitdashboards.dto.DependenciaDTO;
import br.com.ib.gitdashboards.parser.DependencyParser;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Parser de dependências para arquivos Gradle ({@code build.gradle} e {@code build.gradle.kts}).
 * Suporta notação de string ({@code 'group:name:version'}) e notação de mapa
 * ({@code group: 'g', name: 'n', version: 'v'}).
 */
@ApplicationScoped
public class BuildGradleParser implements DependencyParser {

    private static final Logger log = Logger.getLogger(BuildGradleParser.class);

    /** Captura: grupo, artefato e versão opcional em notação de string. */
    private static final Pattern STRING_NOTATION = Pattern.compile(
        "(?:implementation|api|compile(?:Only)?|runtime(?:Only)?|" +
        "test(?:Implementation|CompileOnly|RuntimeOnly)|annotationProcessor|kapt|classpath)" +
        "\\s*[\\(\"']\\s*[\"']?([a-zA-Z0-9._-]+):([a-zA-Z0-9._-]+)(?::([^'\"\\)\\s]+))?[\"']?",
        Pattern.CASE_INSENSITIVE
    );

    /** Captura: group/name/version em notação de mapa Groovy/Kotlin. */
    private static final Pattern MAP_NOTATION = Pattern.compile(
        "group\\s*[=:]\\s*['\"]([^'\"]+)['\"]\\s*,\\s*name\\s*[=:]\\s*['\"]([^'\"]+)['\"]" +
        "(?:\\s*,\\s*version\\s*[=:]\\s*['\"]([^'\"]+)['\"])?",
        Pattern.CASE_INSENSITIVE
    );

    @Override
    public boolean suporta(String nomeArquivo) {
        return "build.gradle".equalsIgnoreCase(nomeArquivo)
            || "build.gradle.kts".equalsIgnoreCase(nomeArquivo);
    }

    @Override
    public List<DependenciaDTO> parsear(String nomeAplicacao, String conteudo) {
        var result = new ArrayList<DependenciaDTO>();
        try {
            var m1 = STRING_NOTATION.matcher(conteudo);
            while (m1.find()) {
                result.add(new DependenciaDTO(
                    nomeAplicacao, m1.group(2), m1.group(3), m1.group(1), "gradle", nomeArquivoOrigem(conteudo)
                ));
            }

            var m2 = MAP_NOTATION.matcher(conteudo);
            while (m2.find()) {
                result.add(new DependenciaDTO(
                    nomeAplicacao, m2.group(2), m2.group(3), m2.group(1), "gradle", nomeArquivoOrigem(conteudo)
                ));
            }
        } catch (Exception e) {
            log.warnf("Erro ao parsear build.gradle do projeto '%s': %s", nomeAplicacao, e.getMessage());
        }
        return result;
    }

    private String nomeArquivoOrigem(String conteudo) {
        // Heurística simples: Kotlin DSL usa funções com parênteses predominantemente
        return conteudo.contains("build.gradle.kts") ? "build.gradle.kts" : "build.gradle";
    }
}

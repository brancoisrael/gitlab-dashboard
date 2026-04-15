package br.com.ib.gitdashboards.parser.impl;

import br.com.ib.gitdashboards.dto.DependenciaDTO;
import br.com.ib.gitdashboards.parser.DependencyParser;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser de dependências para arquivos Maven {@code pom.xml}.
 * Extrai todas as entradas de {@code <dependencies>}, capturando
 * {@code groupId} (pacote), {@code artifactId} (dependência) e {@code version}.
 */
@ApplicationScoped
public class PomXmlParser implements DependencyParser {

    private static final Logger log = Logger.getLogger(PomXmlParser.class);

    @Override
    public boolean suporta(String nomeArquivo) {
        return "pom.xml".equalsIgnoreCase(nomeArquivo);
    }

    @Override
    public List<DependenciaDTO> parsear(String nomeAplicacao, String conteudo) {
        var result = new ArrayList<DependenciaDTO>();
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setNamespaceAware(false);
            var doc = factory.newDocumentBuilder()
                .parse(new InputSource(new StringReader(conteudo)));

            var deps = doc.getElementsByTagName("dependency");
            for (int i = 0; i < deps.getLength(); i++) {
                var dep = (Element) deps.item(i);
                var groupId    = textOf(dep, "groupId");
                var artifactId = textOf(dep, "artifactId");
                var version    = textOf(dep, "version");
                if (artifactId != null) {
                    result.add(new DependenciaDTO(nomeAplicacao, artifactId, version, groupId, "maven", "pom.xml"));
                }
            }
        } catch (Exception e) {
            log.warnf("Erro ao parsear pom.xml do projeto '%s': %s", nomeAplicacao, e.getMessage());
        }
        return result;
    }

    private String textOf(Element parent, String tag) {
        var nodes = parent.getElementsByTagName(tag);
        if (nodes.getLength() == 0) return null;
        var text = nodes.item(0).getTextContent();
        return text != null && !text.isBlank() ? text.trim() : null;
    }
}

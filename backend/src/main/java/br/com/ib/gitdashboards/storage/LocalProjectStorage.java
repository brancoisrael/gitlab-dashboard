package br.com.ib.gitdashboards.storage;

import br.com.ib.gitdashboards.config.RuntimeStorageConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

@ApplicationScoped
public class LocalProjectStorage implements ProjectStorageService {

    private static final Logger log = Logger.getLogger(LocalProjectStorage.class);

    private final RuntimeStorageConfig runtimeConfig;

    @Inject
    public LocalProjectStorage(RuntimeStorageConfig runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    @Override
    public Path criarArquivoTemporario() throws IOException {
        return Files.createTempFile("projetos-gitlab-", ".parquet.tmp");
    }

    @Override
    public Path obterArquivoParaLeitura(String nomeArquivo) {
        return Path.of(runtimeConfig.localPath()).resolve(nomeArquivo);
    }

    @Override
    public boolean arquivoDisponivel(String nomeArquivo) {
        return Files.exists(Path.of(runtimeConfig.localPath()).resolve(nomeArquivo));
    }

    @Override
    public List<String> listarArquivosDependencias() {
        var dir = Path.of(runtimeConfig.localPath());
        if (!Files.exists(dir)) return List.of();
        try (var stream = Files.list(dir)) {
            return stream
                .map(p -> p.getFileName().toString())
                .filter(n -> n.startsWith("dependencie_") && n.endsWith(".parquet"))
                .map(n -> n.substring("dependencie_".length(), n.length() - ".parquet".length())
                           .replace("_", "."))
                .sorted()
                .toList();
        } catch (IOException e) {
            log.warnf("Erro ao listar arquivos de dependências: %s", e.getMessage());
            return List.of();
        }
    }

    @Override
    public void confirmar(Path arquivoTemp, String nomeArquivo) throws IOException {
        var destino = Path.of(runtimeConfig.localPath()).resolve(nomeArquivo);
        Files.createDirectories(destino.getParent());
        Files.move(arquivoTemp, destino, StandardCopyOption.REPLACE_EXISTING);
        log.infof("Parquet salvo localmente em: %s", destino.toAbsolutePath());
    }
}

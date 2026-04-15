package br.com.ib.gitdashboards.config;

import br.com.ib.gitdashboards.dto.SettingsStorageDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuração de armazenamento em tempo de execução.
 *
 * <p>Na inicialização, lê {@code settings.config-file} se existir; caso contrário,
 * usa os valores do {@link StorageConfig} (application.yaml / variáveis de ambiente).
 * Ao salvar, persiste em {@code settings.config-file} e atualiza o estado em memória
 * imediatamente — sem necessidade de reiniciar a JVM.
 *
 * <p>A {@code secretKey} retornada ao cliente é mascarada como {@code "***"}.
 * Um POST com {@code secretKey="***"} preserva o valor existente.
 */
@ApplicationScoped
public class RuntimeStorageConfig {

    private static final Logger log = Logger.getLogger(RuntimeStorageConfig.class);
    static final String MASKED = "***";

    @Inject
    StorageConfig bootstrapConfig;

    @Inject
    ObjectMapper mapper;

    @Inject
    @ConfigProperty(name = "settings.config-file", defaultValue = "/tmp/gitlab-dashboard/settings.json")
    String settingsFilePath;

    /** Configuração ativa; substituída atomicamente ao salvar. */
    private volatile SettingsStorageDTO current;

    @PostConstruct
    void init() {
        var file = Path.of(settingsFilePath);
        if (Files.exists(file)) {
            try {
                current = mapper.readValue(file.toFile(), SettingsStorageDTO.class);
                log.infof("Configurações de armazenamento carregadas de: %s", file);
                return;
            } catch (IOException e) {
                log.warnf("Falha ao ler %s; usando defaults do application.yaml: %s",
                          file, e.getMessage());
            }
        }
        current = defaultsFromBootstrap();
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public String type()      { return current.type(); }
    public String localPath() { return current.localPath(); }
    public String endpoint()  { return current.endpoint(); }
    public String bucket()    { return current.bucket(); }
    public String region()    { return current.region(); }
    public String accessKey() { return current.accessKey(); }
    public String secretKey() { return current.secretKey(); }

    /**
     * Retorna o DTO atual com a secretKey mascarada para exibição no frontend.
     */
    public SettingsStorageDTO getForDisplay() {
        var c = current;
        var maskedSecret = (c.secretKey() != null && !c.secretKey().isBlank()) ? MASKED : "";
        return new SettingsStorageDTO(
            c.type(), c.localPath(), c.endpoint(),
            c.bucket(), c.region(), c.accessKey(), maskedSecret
        );
    }

    /**
     * Persiste a nova configuração em arquivo e atualiza o estado em memória.
     * Se {@code secretKey == "***"}, preserva o valor atual.
     */
    public void save(SettingsStorageDTO dto) throws IOException {
        var resolvedSecret = MASKED.equals(dto.secretKey()) ? current.secretKey() : dto.secretKey();
        var toSave = new SettingsStorageDTO(
            dto.type(), dto.localPath(), dto.endpoint(),
            dto.bucket(), dto.region(), dto.accessKey(), resolvedSecret
        );
        var file = Path.of(settingsFilePath);
        Files.createDirectories(file.getParent());
        mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), toSave);
        current = toSave;
        log.infof("Configurações de armazenamento salvas em: %s", file);
    }

    // ── Private ──────────────────────────────────────────────────────────────

    private SettingsStorageDTO defaultsFromBootstrap() {
        return new SettingsStorageDTO(
            bootstrapConfig.type(),
            bootstrapConfig.local().baseDir(),
            bootstrapConfig.s3().endpoint(),
            bootstrapConfig.s3().bucket(),
            bootstrapConfig.s3().region(),
            bootstrapConfig.s3().accessKey(),
            bootstrapConfig.s3().secretKey()
        );
    }
}

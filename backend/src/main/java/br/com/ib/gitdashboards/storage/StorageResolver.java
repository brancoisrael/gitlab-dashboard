package br.com.ib.gitdashboards.storage;

import br.com.ib.gitdashboards.config.RuntimeStorageConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Seleciona a implementação de {@link ProjectStorageService} ativa
 * com base em {@code RuntimeStorageConfig.type()} (local | s3).
 * O tipo é resolvido a cada chamada, de forma que alterações via API
 * de configurações entram em vigor imediatamente.
 */
@ApplicationScoped
public class StorageResolver {

    private final RuntimeStorageConfig runtimeConfig;
    private final LocalProjectStorage localStorage;
    private final S3ProjectStorage s3Storage;

    @Inject
    public StorageResolver(RuntimeStorageConfig runtimeConfig, LocalProjectStorage localStorage,
                           S3ProjectStorage s3Storage) {
        this.runtimeConfig = runtimeConfig;
        this.localStorage = localStorage;
        this.s3Storage = s3Storage;
    }

    public ProjectStorageService resolve() {
        return "s3".equalsIgnoreCase(runtimeConfig.type()) ? s3Storage : localStorage;
    }
}

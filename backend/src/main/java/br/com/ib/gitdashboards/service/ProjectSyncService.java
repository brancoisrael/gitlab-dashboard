package br.com.ib.gitdashboards.service;

import br.com.ib.gitdashboards.config.GitLabConfig;
import br.com.ib.gitdashboards.dto.GitLabProjectDTO;
import br.com.ib.gitdashboards.storage.ParquetProjectWriter;
import br.com.ib.gitdashboards.storage.ProjectStorageService;
import br.com.ib.gitdashboards.storage.StorageResolver;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Orquestra a sincronização assíncrona de projetos GitLab para Parquet.
 *
 * <p>Fluxo:
 * <ol>
 *   <li>Cria arquivo temporário local via {@link ProjectStorageService}.</li>
 *   <li>Pagina a API do GitLab via {@link GitLabService} (com retry + circuit breaker).</li>
 *   <li>Escreve cada registro incrementalmente no arquivo Parquet — sem bufferizar tudo em memória.</li>
 *   <li>Ao esgotar as páginas, confirma o arquivo no destino (local ou S3).</li>
 *   <li>Em caso de falha, limpa o arquivo temporário e registra o erro.</li>
 * </ol>
 *
 * <p>Execuções concorrentes são prevenidas por {@link #emExecucao}.
 */
@ApplicationScoped
public class ProjectSyncService {

    private static final Logger log = Logger.getLogger(ProjectSyncService.class);

    private final GitLabService gitLabService;
    private final StorageResolver storageResolver;
    private final ParquetProjectWriter parquetWriter;
    private final GitLabConfig gitLabConfig;
    private final SyncEventService syncEventService;

    private final AtomicBoolean emExecucao = new AtomicBoolean(false);

    @Inject
    public ProjectSyncService(GitLabService gitLabService, StorageResolver storageResolver,
                              ParquetProjectWriter parquetWriter, GitLabConfig gitLabConfig,
                              SyncEventService syncEventService) {
        this.gitLabService = gitLabService;
        this.storageResolver = storageResolver;
        this.parquetWriter = parquetWriter;
        this.gitLabConfig = gitLabConfig;
        this.syncEventService = syncEventService;
    }

    public void executar() {
        if (!emExecucao.compareAndSet(false, true)) {
            log.warn("Sincronização de projetos já está em andamento — requisição ignorada");
            return;
        }

        Path arquivoTemp = null;
        try {
            var storage = storageResolver.resolve();
            arquivoTemp = storage.criarArquivoTemporario();
            var nomeArquivo = "projetos.parquet";

            var total = sincronizarPaginas(arquivoTemp);

            storage.confirmar(arquivoTemp, nomeArquivo);
            arquivoTemp = null; // storage assumiu o arquivo; não deletar no finally
            log.infof("Sincronização concluída: %d projetos → '%s'", total, nomeArquivo);
            syncEventService.emitirConcluido();

        } catch (Exception e) {
            log.errorf(e, "Falha na sincronização de projetos GitLab");
            syncEventService.emitirErro();
        } finally {
            emExecucao.set(false);
            deletarSeExistir(arquivoTemp);
        }
    }

    /**
     * Percorre todas as páginas da API e escreve cada registro no Parquet de forma incremental.
     *
     * @return total de registros escritos
     */
    private int sincronizarPaginas(Path arquivoTemp) throws IOException {
        var porPagina = gitLabConfig.api().perPageDefault();
        var pagina = 1;
        var totalRegistros = 0;

        try (var writer = parquetWriter.abrirEscritor(arquivoTemp)) {
            List<GitLabProjectDTO> projetos;
            do {
                projetos = gitLabService.listarProjetos(pagina, porPagina);
                for (var projeto : projetos) {
                    writer.write(parquetWriter.toRecord(projeto));
                    totalRegistros++;
                }
                log.debugf("Página %d: %d projetos (acumulado: %d)", pagina, projetos.size(), totalRegistros);
                pagina++;
            } while (projetos.size() == porPagina);
        }

        return totalRegistros;
    }

    private void deletarSeExistir(Path arquivo) {
        if (arquivo == null) return;
        try {
            Files.deleteIfExists(arquivo);
        } catch (IOException _) {
            log.warnf("Não foi possível deletar arquivo temporário: %s", arquivo);
        }
    }
}

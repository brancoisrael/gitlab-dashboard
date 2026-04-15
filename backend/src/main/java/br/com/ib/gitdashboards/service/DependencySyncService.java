package br.com.ib.gitdashboards.service;

import br.com.ib.gitdashboards.dto.DependenciaDTO;
import br.com.ib.gitdashboards.parser.DependencyParserRegistry;
import br.com.ib.gitdashboards.storage.ParquetDependencyWriter;
import br.com.ib.gitdashboards.storage.StorageResolver;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

/**
 * Orquestra a catalogação assíncrona de dependências projeto a projeto.
 *
 * <p>Lê a lista de projetos do {@code projetos.parquet} e processa cada um em paralelo
 * usando virtual threads. O grau de paralelismo é limitado por um {@link Semaphore} com
 * {@code dependencies.sync.max-threads} permissões, evitando sobrecarga na API GitLab.
 *
 * <p>Execuções concorrentes para o <em>mesmo arquivo</em> são silenciosamente ignoradas.
 * Execuções para arquivos diferentes podem correr em paralelo.
 *
 * <p>Ao finalizar (com sucesso ou erro), emite um evento SSE via {@link DependencySyncEventService}.
 */
@ApplicationScoped
public class DependencySyncService {

    private static final Logger log = Logger.getLogger(DependencySyncService.class);

    private final Set<String> emExecucao = ConcurrentHashMap.newKeySet();

    private final ParquetQueryService parquetQueryService;
    private final GitLabService gitLabService;
    private final DependencyParserRegistry parserRegistry;
    private final ParquetDependencyWriter parquetWriter;
    private final StorageResolver storageResolver;
    private final DependencySyncEventService eventService;
    private final int maxThreads;

    @Inject
    public DependencySyncService(
        ParquetQueryService parquetQueryService,
        GitLabService gitLabService,
        DependencyParserRegistry parserRegistry,
        ParquetDependencyWriter parquetWriter,
        StorageResolver storageResolver,
        DependencySyncEventService eventService,
        @ConfigProperty(name = "dependencies.sync.max-threads", defaultValue = "10") int maxThreads
    ) {
        this.parquetQueryService = parquetQueryService;
        this.gitLabService       = gitLabService;
        this.parserRegistry      = parserRegistry;
        this.parquetWriter       = parquetWriter;
        this.storageResolver     = storageResolver;
        this.eventService        = eventService;
        this.maxThreads          = maxThreads;
    }

    /**
     * Inicia a sincronização em thread virtual.
     * Retorna {@code false} se já houver uma sincronização em andamento para o mesmo arquivo.
     *
     * @param nomeArquivo arquivo de dependências a buscar (ex.: {@code pom.xml})
     * @return {@code true} se a execução foi iniciada; {@code false} se já estava em andamento
     */
    public boolean iniciar(String nomeArquivo) {
        if (!emExecucao.add(nomeArquivo)) {
            log.warnf("Sincronização de dependências para '%s' já está em andamento — ignorada", nomeArquivo);
            return false;
        }
        Thread.ofVirtual().start(() -> executar(nomeArquivo));
        return true;
    }

    private void executar(String nomeArquivo) {
        log.infof("Iniciando catalogação paralela de '%s' (%d threads simultâneas)", nomeArquivo, maxThreads);
        Path arquivoTemp = null;
        try {
            var storage     = storageResolver.resolve();
            var projetos    = parquetQueryService.listarTodos();
            var parserOpt   = parserRegistry.encontrar(nomeArquivo);
            var nomeParquet = gerarNomeParquet(nomeArquivo);

            if (parserOpt.isEmpty()) {
                log.warnf("Nenhum parser disponível para '%s' — sincronização encerrada", nomeArquivo);
                eventService.emitirErro(nomeArquivo);
                return;
            }

            var parser   = parserOpt.get();
            arquivoTemp  = storage.criarArquivoTemporario();

            // Fila thread-safe: cada virtual thread produtora deposita suas deps aqui
            var coletado = new ConcurrentLinkedQueue<DependenciaDTO>();
            var semaforo = new Semaphore(maxThreads);
            var latch    = new CountDownLatch(projetos.size());

            for (var projeto : projetos) {
                semaforo.acquire();  // bloqueia (virtual thread) quando maxThreads estão ativos
                Thread.ofVirtual().start(() -> {
                    try {
                        var branch   = "master";
                        var conteudo = gitLabService.tentarObterArquivo(projeto.id(), nomeArquivo, branch);
                        if (conteudo.isPresent()) {
                            coletado.addAll(parser.parsear(projeto.name(), conteudo.get()));
                        }
                    } catch (Exception e) {
                        log.warnf("Projeto '%s' ignorado: %s", projeto.name(), e.getMessage());
                    } finally {
                        semaforo.release();
                        latch.countDown();
                    }
                });
            }

            latch.await();  // aguarda todos os produtores finalizarem

            // Escrita sequencial — AvroParquetWriter não é thread-safe
            int totalDeps = 0;
            try (var writer = parquetWriter.abrirEscritor(arquivoTemp)) {
                for (var dep : coletado) {
                    writer.write(parquetWriter.toRecord(dep));
                    totalDeps++;
                }
            }

            storage.confirmar(arquivoTemp, nomeParquet);
            arquivoTemp = null;
            log.infof("Catalogação de '%s' concluída: %d dependências em '%s'", nomeArquivo, totalDeps, nomeParquet);
            eventService.emitirConcluido(nomeArquivo);

        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            log.errorf("Catalogação de '%s' interrompida", nomeArquivo);
            eventService.emitirErro(nomeArquivo);
        } catch (Exception e) {
            log.errorf(e, "Erro ao catalogar dependências para '%s'", nomeArquivo);
            eventService.emitirErro(nomeArquivo);
        } finally {
            emExecucao.remove(nomeArquivo);
            deletarSeExistir(arquivoTemp);
        }
    }

    /**
     * Converte o nome do arquivo de origem no nome do Parquet de destino.
     * Regra: {@code dependencie_} + nome com {@code .} substituído por {@code _} + {@code .parquet}.
     * Exemplo: {@code pom.xml} → {@code dependencie_pom_xml.parquet}
     */
    public static String gerarNomeParquet(String nomeArquivo) {
        return "dependencie_" + nomeArquivo.replace(".", "_") + ".parquet";
    }

    private void deletarSeExistir(Path arquivo) {
        if (arquivo == null) return;
        try {
            Files.deleteIfExists(arquivo);
        } catch (Exception e) {
            log.warnf("Não foi possível deletar arquivo temporário '%s': %s", arquivo, e.getMessage());
        }
    }
}

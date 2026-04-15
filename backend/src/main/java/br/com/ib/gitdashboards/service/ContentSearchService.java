package br.com.ib.gitdashboards.service;

import br.com.ib.gitdashboards.dto.ArquivoBuscadoDTO;
import br.com.ib.gitdashboards.dto.ProjetoBuscaDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

/**
 * Busca conteúdo em todos os projetos GitLab sincronizados de forma concorrente e assíncrona.
 *
 * <p>Cada chamada a {@link #iniciar} gera um ID único, dispara a busca em segundo plano
 * com virtual threads (limitadas por {@code content.search.max-threads}) e armazena os
 * resultados em memória até que o cliente os consuma via {@link #obterResultado}.
 *
 * <p>Ao finalizar, emite um evento SSE via {@link ContentSearchEventService} no formato
 * {@code "concluido:<id>"} ou {@code "erro:<id>"}.
 */
@ApplicationScoped
public class ContentSearchService {

    private static final Logger log = Logger.getLogger(ContentSearchService.class);

    private final ConcurrentHashMap<String, List<ProjetoBuscaDTO>> resultados = new ConcurrentHashMap<>();

    private final ParquetQueryService parquetQueryService;
    private final GitLabService gitLabService;
    private final ContentSearchEventService eventService;
    private final int maxThreads;

    @Inject
    public ContentSearchService(
        ParquetQueryService parquetQueryService,
        GitLabService gitLabService,
        ContentSearchEventService eventService,
        @ConfigProperty(name = "content.search.max-threads", defaultValue = "10") int maxThreads
    ) {
        this.parquetQueryService = parquetQueryService;
        this.gitLabService       = gitLabService;
        this.eventService        = eventService;
        this.maxThreads          = maxThreads;
    }

    /**
     * Inicia a busca de conteúdo em segundo plano.
     *
     * @param extensao extensão dos arquivos a considerar (ex.: {@code ".java"})
     * @param texto    termo a buscar no conteúdo dos arquivos
     * @return ID único da busca; use-o para correlacionar o evento SSE e buscar os resultados
     */
    public String iniciar(String extensao, String texto) {
        var id = UUID.randomUUID().toString();
        Thread.ofVirtual().start(() -> executar(id, extensao, texto));
        return id;
    }

    /**
     * Retorna os resultados de uma busca já concluída e os remove do armazenamento interno.
     *
     * @param id ID da busca retornado por {@link #iniciar}
     * @return resultados da busca; {@link Optional#empty()} se o ID não existir ou a busca ainda não terminou
     */
    public Optional<List<ProjetoBuscaDTO>> obterResultado(String id) {
        return Optional.ofNullable(resultados.remove(id));
    }

    private void executar(String id, String extensao, String texto) {
        log.infof("Busca [%s] iniciada: extensao='%s' texto='%s' (%d threads)", id, extensao, texto, maxThreads);
        try {
            var projetos   = parquetQueryService.listarTodos();
            var semaforo   = new Semaphore(maxThreads);
            var latch      = new CountDownLatch(projetos.size());
            var coletados  = new ConcurrentLinkedQueue<ProjetoBuscaDTO>();

            for (var projeto : projetos) {
                semaforo.acquire();
                Thread.ofVirtual().start(() -> {
                    try {
                        var branch  = projeto.defaultBranch() != null ? projeto.defaultBranch() : "HEAD";
                        var blobs   = gitLabService.buscarArquivos(projeto.id(), branch, texto, extensao);
                        if (!blobs.isEmpty()) {
                            var webUrl   = projeto.webUrl() != null ? projeto.webUrl() : "";
                            var arquivos = blobs.stream()
                                .map(b -> new ArquivoBuscadoDTO(
                                    b.filename(),
                                    webUrl + "/-/blob/" + b.ref() + "/" + b.path()
                                ))
                                .toList();
                            coletados.add(new ProjetoBuscaDTO(projeto.name(), arquivos));
                        }
                    } catch (Exception e) {
                        log.warnf("Projeto '%s' ignorado na busca [%s]: %s", projeto.name(), id, e.getMessage());
                    } finally {
                        semaforo.release();
                        latch.countDown();
                    }
                });
            }

            latch.await();

            var resultado = coletados.stream()
                .sorted(Comparator.comparing(ProjetoBuscaDTO::nomeProjeto, String.CASE_INSENSITIVE_ORDER))
                .toList();

            resultados.put(id, resultado);
            log.infof("Busca [%s] concluída: %d projetos com resultados", id, resultado.size());
            eventService.emitirConcluido(id);

        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            log.errorf("Busca [%s] interrompida", id);
            eventService.emitirErro(id);
        } catch (Exception e) {
            log.errorf(e, "Erro na busca [%s]", id);
            eventService.emitirErro(id);
        }
    }
}

package br.com.ib.gitdashboards.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Canal de eventos de sincronização via SSE.
 *
 * <p>Publica um evento único quando a sincronização termina (com sucesso ou erro).
 * Clientes SSE conectados recebem o evento e encerram a conexão.
 */
@ApplicationScoped
public class SyncEventService {

    private final BroadcastProcessor<String> processor = BroadcastProcessor.create();

    /** Chamado pelo {@link ProjectSyncService} ao concluir com sucesso. */
    public void emitirConcluido() {
        processor.onNext("concluido");
    }

    /** Chamado pelo {@link ProjectSyncService} em caso de falha. */
    public void emitirErro() {
        processor.onNext("erro");
    }

    /** Stream de eventos consumido pelo endpoint SSE. */
    public Multi<String> eventos() {
        return processor;
    }
}

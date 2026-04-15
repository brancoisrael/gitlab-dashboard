package br.com.ib.gitdashboards.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Canal de eventos de catalogação de dependências via SSE.
 *
 * <p>Emite um evento quando a catalogação de um arquivo termina (com sucesso ou erro).
 * O formato do evento é {@code "concluido:<nomeArquivo>"} ou {@code "erro:<nomeArquivo>"},
 * permitindo ao cliente filtrar pelo arquivo de interesse.
 */
@ApplicationScoped
public class DependencySyncEventService {

    private final BroadcastProcessor<String> processor = BroadcastProcessor.create();

    /** Chamado pelo {@link DependencySyncService} ao concluir com sucesso. */
    public void emitirConcluido(String nomeArquivo) {
        processor.onNext("concluido:" + nomeArquivo);
    }

    /** Chamado pelo {@link DependencySyncService} em caso de falha. */
    public void emitirErro(String nomeArquivo) {
        processor.onNext("erro:" + nomeArquivo);
    }

    /** Stream de eventos consumido pelo endpoint SSE. */
    public Multi<String> eventos() {
        return processor;
    }
}

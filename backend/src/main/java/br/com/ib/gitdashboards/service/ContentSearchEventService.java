package br.com.ib.gitdashboards.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Canal de eventos de busca de conteúdo via SSE.
 *
 * <p>Emite um evento quando a busca termina (com sucesso ou erro).
 * O formato do evento é {@code "concluido:<id>"} ou {@code "erro:<id>"},
 * permitindo ao cliente filtrar pelo ID da busca retornado no POST.
 */
@ApplicationScoped
public class ContentSearchEventService {

    private final BroadcastProcessor<String> processor = BroadcastProcessor.create();

    /** Chamado pelo {@link ContentSearchService} ao concluir com sucesso. */
    public void emitirConcluido(String id) {
        processor.onNext("concluido:" + id);
    }

    /** Chamado pelo {@link ContentSearchService} em caso de falha. */
    public void emitirErro(String id) {
        processor.onNext("erro:" + id);
    }

    /** Stream de eventos consumido pelo endpoint SSE. */
    public Multi<String> eventos() {
        return processor;
    }
}

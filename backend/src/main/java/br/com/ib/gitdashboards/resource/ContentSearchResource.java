package br.com.ib.gitdashboards.resource;

import br.com.ib.gitdashboards.i18n.MessageService;
import br.com.ib.gitdashboards.service.ContentSearchEventService;
import br.com.ib.gitdashboards.service.ContentSearchService;
import br.com.ib.gitdashboards.storage.StorageResolver;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestStreamElementType;

import java.util.Map;

/**
 * Endpoints de busca de conteúdo em arquivos dos projetos GitLab.
 * Contexto: {@code /api/busca}
 *
 * <p>Fluxo de uso:
 * <ol>
 *   <li>{@code POST /api/busca?extensao=.java&texto=HttpStatus.NOT_FOUND} — inicia a busca em segundo
 *       plano e retorna um ID único.</li>
 *   <li>{@code GET /api/busca/eventos} — conectar ao stream SSE e aguardar o evento
 *       {@code "concluido:<id>"} ou {@code "erro:<id>"}.</li>
 *   <li>{@code GET /api/busca/resultado/{id}} — buscar os resultados após o evento de conclusão.</li>
 * </ol>
 */
@Path("/busca")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Busca de Conteúdo", description = "Busca texto em arquivos dos projetos GitLab sincronizados")
public class ContentSearchResource {

    private final ContentSearchService searchService;
    private final ContentSearchEventService eventService;
    private final StorageResolver storageResolver;
    private final MessageService messages;

    @Inject
    public ContentSearchResource(
        ContentSearchService searchService,
        ContentSearchEventService eventService,
        StorageResolver storageResolver,
        MessageService messages
    ) {
        this.searchService   = searchService;
        this.eventService    = eventService;
        this.storageResolver = storageResolver;
        this.messages        = messages;
    }

    /**
     * Inicia a busca de conteúdo em segundo plano.
     *
     * <p>Percorre todos os projetos do {@code projetos.parquet} de forma concorrente via virtual threads,
     * consultando a API de busca de blobs do GitLab em cada projeto com filtragem pela extensão informada.
     * Retorna imediatamente com o ID da busca; conecte-se ao stream SSE para saber quando terminar.
     */
    @POST
    @Operation(
        summary = "Iniciar busca de conteúdo nos projetos",
        description = """
            Dispara a busca em segundo plano e retorna um ID único.
            Conecte-se a GET /api/busca/eventos para receber o evento de conclusão,
            então busque os resultados em GET /api/busca/resultado/{id}.
            Requer que a sincronização de projetos tenha sido executada ao menos uma vez.
            """
    )
    @APIResponse(responseCode = "200", description = "Busca iniciada — retorna o ID para acompanhamento")
    @APIResponse(responseCode = "400", description = "Parâmetros 'extensao' ou 'texto' ausentes")
    @APIResponse(responseCode = "409", description = "Projetos ainda não sincronizados — execute POST /api/projetos/sincronizar primeiro")
    public Response iniciar(
        @Parameter(description = "Extensão dos arquivos a considerar (ex.: .java, .py)", required = true)
        @QueryParam("extensao") String extensao,

        @Parameter(description = "Texto a buscar no conteúdo dos arquivos", required = true)
        @QueryParam("texto") String texto,

        @HeaderParam("Accept-Language") String acceptLanguage
    ) {
        if (extensao == null || extensao.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("erro", messages.get("busca.extensao.obrigatoria", acceptLanguage)))
                .build();
        }
        if (texto == null || texto.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("erro", messages.get("busca.texto.obrigatorio", acceptLanguage)))
                .build();
        }
        if (!storageResolver.resolve().arquivoDisponivel()) {
            return Response.status(Response.Status.CONFLICT)
                .entity(Map.of("erro", messages.get("busca.projetos.nao.sincronizados", acceptLanguage)))
                .build();
        }

        var id = searchService.iniciar(extensao.trim(), texto.trim());
        return Response.ok(Map.of("id", id)).build();
    }

    /**
     * Stream SSE de eventos de busca de conteúdo.
     *
     * <p>Emite {@code "concluido:<id>"} quando a busca termina com sucesso ou
     * {@code "erro:<id>"} em caso de falha. O cliente deve filtrar pelo ID retornado no POST.
     */
    @GET
    @Path("/eventos")
    @Produces("text/event-stream")
    @RestStreamElementType("text/plain")
    @Operation(
        summary = "Stream SSE de eventos de busca",
        description = "Emite 'concluido:<id>' quando a busca termina com sucesso ou 'erro:<id>' em caso de falha."
    )
    @APIResponse(responseCode = "200", description = "Stream SSE ativo")
    public Multi<String> eventos() {
        return eventService.eventos();
    }

    /**
     * Retorna os resultados de uma busca concluída e os remove do armazenamento interno.
     *
     * <p>Deve ser chamado após receber o evento {@code "concluido:<id>"} no stream SSE.
     * O resultado é removido após a primeira leitura — chamadas subsequentes retornam 404.
     */
    @GET
    @Path("/resultado/{id}")
    @Operation(
        summary = "Obter resultado da busca",
        description = """
            Retorna a lista de projetos com os arquivos encontrados para o ID informado.
            Cada projeto contém o nome e a lista de arquivos com nome e URL direta no GitLab.
            O resultado é removido após a primeira leitura.
            """
    )
    @APIResponse(responseCode = "200", description = "Resultado retornado com sucesso")
    @APIResponse(responseCode = "404", description = "ID não encontrado — busca ainda em andamento, não iniciada, já consumida ou encerrada com erro")
    public Response obterResultado(
        @Parameter(description = "ID da busca retornado pelo POST /api/busca", required = true)
        @PathParam("id") String id,

        @HeaderParam("Accept-Language") String acceptLanguage
    ) {
        return searchService.obterResultado(id)
            .map(resultado -> Response.ok(resultado).build())
            .orElse(Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("erro", messages.get("busca.resultado.nao.encontrado", acceptLanguage)))
                .build());
    }
}

package br.com.ib.gitdashboards.resource;

import br.com.ib.gitdashboards.config.GitLabConfig;
import br.com.ib.gitdashboards.dto.GitLabProjectDTO;
import br.com.ib.gitdashboards.dto.ProjetoGrupoDTO;
import br.com.ib.gitdashboards.i18n.MessageService;
import br.com.ib.gitdashboards.service.GitLabService;
import br.com.ib.gitdashboards.service.ParquetQueryService;
import br.com.ib.gitdashboards.service.ProjectSyncService;
import br.com.ib.gitdashboards.service.SyncEventService;
import br.com.ib.gitdashboards.storage.StorageResolver;
import io.smallrye.mutiny.Multi;
import org.jboss.resteasy.reactive.RestStreamElementType;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import br.com.ib.gitdashboards.dto.ProjetoDetalheDTO;
import jakarta.ws.rs.PathParam;

@Path("/projetos")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Projetos", description = "Consulta e sincronização de projetos do GitLab")
public class GitLabResource {

    @Inject
    GitLabService service;

    @Inject
    GitLabConfig config;

    @Inject
    ProjectSyncService syncService;

    @Inject
    ParquetQueryService queryService;

    @Inject
    SyncEventService syncEventService;

    @Inject
    StorageResolver storageResolver;

    @Inject
    MessageService messages;

    @GET
    @Operation(
        summary = "Lista projetos do GitLab",
        description = "Retorna os projetos acessíveis pelo token configurado, ordenados por última atividade."
    )
    @APIResponse(responseCode = "200", description = "Lista de projetos retornada com sucesso")
    @APIResponse(responseCode = "401", description = "Token inválido ou não configurado")
    @APIResponse(responseCode = "502", description = "Erro ao comunicar com o GitLab")
    public List<GitLabProjectDTO> listarProjetos(
        @Parameter(description = "Número da página (inicia em 1)")
        @QueryParam("pagina") @DefaultValue("1") int pagina,

        @Parameter(description = "Quantidade de itens por página")
        @QueryParam("porPagina") int porPagina
    ) {
        var tamanho = porPagina > 0 ? porPagina : config.api().perPageDefault();
        return service.listarProjetos(pagina, tamanho);
    }

    @GET
    @Path("/grupos")
    @Operation(
        summary = "Agrupa projetos por namespace de primeiro nível",
        description = """
            Lê o arquivo Parquet local e agrupa os projetos pelo nome extraído de
            name_with_namespace (parte antes do primeiro " / ").
            Retorna lista ordenada por contagem decrescente.
            Requer que a sincronização tenha sido executada ao menos uma vez.
            """
    )
    @APIResponse(responseCode = "200", description = "Agrupamento retornado com sucesso")
    @APIResponse(responseCode = "202", description = "Arquivo ainda não existe — sincronização iniciada automaticamente")
    @APIResponse(responseCode = "500", description = "Erro ao ler o arquivo Parquet")
    public Response listarGrupos(@HeaderParam("Accept-Language") String acceptLanguage) {
        if (!storageResolver.resolve().arquivoDisponivel()) {
            Thread.ofVirtual().start(syncService::executar);
            return Response.accepted(Map.of("mensagem", messages.get("sincronizacao.iniciada.automaticamente", acceptLanguage))).build();
        }
        try {
            var grupos = queryService.contarPorGrupo();
            return Response.ok(grupos).build();
        } catch (IOException _) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("erro", messages.get("erro.leitura.parquet", acceptLanguage)))
                .build();
        }
    }

    @GET
    @Path("/grupos/{grupo}")
    @Operation(
        summary = "Lista projetos de um grupo",
        description = "Retorna os projetos que pertencem ao grupo informado, ordenados por nome."
    )
    @APIResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @APIResponse(responseCode = "500", description = "Erro ao ler o arquivo Parquet")
    public Response listarProjetosPorGrupo(
        @Parameter(description = "Nome do grupo") @PathParam("grupo") String grupo,
        @HeaderParam("Accept-Language") String acceptLanguage
    ) {
        try {
            var projetos = queryService.listarPorGrupo(grupo);
            return Response.ok(projetos).build();
        } catch (IOException _) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("erro", messages.get("erro.leitura.parquet", acceptLanguage)))
                .build();
        }
    }

    @GET
    @Path("/sincronizar/eventos")
    @Produces("text/event-stream")
    @RestStreamElementType("text/plain")
    @Operation(
        summary = "Stream SSE de eventos de sincronização",
        description = "Emite um único evento ('concluido' ou 'erro') quando a sincronização termina e encerra a conexão."
    )
    @APIResponse(responseCode = "200", description = "Stream SSE ativo")
    public Multi<String> eventosSincronizacao() {
        return syncEventService.eventos();
    }

    @POST
    @Path("/sincronizar")
    @Operation(
        summary = "Inicia sincronização assíncrona para Parquet",
        description = """
            Dispara a leitura paginada de todos os projetos do GitLab e persiste o resultado
            em formato Parquet no destino configurado (local ou S3/MinIO).
            A operação ocorre em segundo plano; esta chamada retorna 200 imediatamente.
            Execuções concorrentes são ignoradas — a sincronização em andamento continua.
            """
    )
    @APIResponse(responseCode = "200", description = "Sincronização iniciada em segundo plano")
    public Response sincronizar(@HeaderParam("Accept-Language") String acceptLanguage) {
        Thread.ofVirtual().start(syncService::executar);
        return Response.ok(Map.of("mensagem", messages.get("sincronizacao.iniciada.background", acceptLanguage))).build();
    }
}

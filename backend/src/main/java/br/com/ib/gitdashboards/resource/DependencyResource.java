package br.com.ib.gitdashboards.resource;

import br.com.ib.gitdashboards.dto.TipoConsulta;
import br.com.ib.gitdashboards.i18n.MessageService;
import br.com.ib.gitdashboards.service.DependencySyncEventService;
import br.com.ib.gitdashboards.service.DependencySyncService;
import br.com.ib.gitdashboards.service.ParquetQueryService;
import br.com.ib.gitdashboards.storage.StorageResolver;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
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
import org.jboss.resteasy.reactive.RestStreamElementType;

import java.io.IOException;

/**
 * Endpoints REST para catalogação e consulta de dependências de projetos GitLab.
 * Contexto: {@code /api/dependencias}
 */
@Path("/dependencias")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Dependências", description = "Catalogação de dependências por arquivo de gerenciamento")
public class DependencyResource {

    private final DependencySyncService syncService;
    private final DependencySyncEventService syncEventService;
    private final ParquetQueryService queryService;
    private final StorageResolver storageResolver;
    private final MessageService messages;

    @Inject
    public DependencyResource(
        DependencySyncService syncService,
        DependencySyncEventService syncEventService,
        ParquetQueryService queryService,
        StorageResolver storageResolver,
        MessageService messages
    ) {
        this.syncService      = syncService;
        this.syncEventService = syncEventService;
        this.queryService     = queryService;
        this.storageResolver  = storageResolver;
        this.messages         = messages;
    }

    /**
     * Dispara a catalogação assíncrona de dependências buscando o arquivo informado
     * em todos os projetos GitLab já sincronizados.
     */
    @POST
    @Path("/sincronizar")
    @Operation(
        summary = "Catalogar dependências",
        description = "Percorre todos os projetos sincronizados, busca o arquivo informado e " +
                      "persiste as dependências em um arquivo Parquet nomeado conforme a regra " +
                      "'dependencie_<arquivo>.parquet' (ex.: pom.xml → dependencie_pom_xml.parquet)."
    )
    @APIResponse(responseCode = "200", description = "Catalogação iniciada em segundo plano")
    @APIResponse(responseCode = "400", description = "Parâmetro 'arquivo' ausente ou vazio")
    @APIResponse(responseCode = "409", description = "Projetos ainda não sincronizados — execute /projetos/sincronizar primeiro")
    public Response sincronizar(
        @Parameter(description = "Nome do arquivo de dependências (ex.: pom.xml)", required = true)
        @QueryParam("arquivo") String nomeArquivo,
        @HeaderParam("Accept-Language") String acceptLanguage
    ) {
        if (nomeArquivo == null || nomeArquivo.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(messages.get("dependencias.arquivo.obrigatorio", acceptLanguage))
                .build();
        }

        if (!storageResolver.resolve().arquivoDisponivel()) {
            return Response.status(Response.Status.CONFLICT)
                .entity(messages.get("dependencias.projetos.nao.sincronizados", acceptLanguage))
                .build();
        }

        syncService.iniciar(nomeArquivo.trim());
        return Response.ok(messages.get("sincronizacao.iniciada.background", acceptLanguage)).build();
    }

    /**
     * Consulta o Parquet de dependências de um arquivo.
     *
     * <p>Combinações válidas:
     * <ul>
     *   <li>Só {@code arquivo} → todos os projetos com suas dependências.</li>
     *   <li>{@code arquivo} + {@code tipo=projeto} + {@code nome} → dependências do projeto.</li>
     *   <li>{@code arquivo} + {@code tipo=dependencia} + {@code nome} → projetos que usam a dependência.</li>
     * </ul>
     * {@code tipo} e {@code nome} devem ser informados juntos ou omitidos juntos.
     */
    @GET
    @Path("/consultar")
    @Operation(
        summary = "Consultar dependências catalogadas",
        description = "Lê o Parquet gerado para o arquivo informado e retorna agrupamentos por projeto " +
                      "ou por dependência. Sem tipo/nome retorna todos os projetos com suas dependências."
    )
    @APIResponse(responseCode = "200", description = "Consulta realizada com sucesso")
    @APIResponse(responseCode = "400", description = "Parâmetros inválidos ou incompletos")
    @APIResponse(responseCode = "404", description = "Arquivo ainda não catalogado — execute POST /sincronizar primeiro")
    @APIResponse(responseCode = "500", description = "Erro ao ler o arquivo Parquet")
    public Response consultar(
        @Parameter(description = "Nome do arquivo de dependências (ex.: pom.xml)", required = true)
        @QueryParam("arquivo") String arquivo,

        @Parameter(description = "Tipo de filtro: 'projeto' ou 'dependencia'")
        @QueryParam("tipo") String tipoParam,

        @Parameter(description = "Nome do projeto ou dependência a consultar (obrigatório quando 'tipo' é informado)")
        @QueryParam("nome") String nome,

        @Parameter(description = "Filtro parcial no campo pacote (ex.: 'br.com'). Independente dos demais filtros.")
        @QueryParam("pacote") String pacote,

        @HeaderParam("Accept-Language") String acceptLanguage
    ) {
        if (arquivo == null || arquivo.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(messages.get("dependencias.arquivo.obrigatorio", acceptLanguage))
                .build();
        }

        boolean temTipo = tipoParam != null && !tipoParam.isBlank();
        boolean temNome = nome != null && !nome.isBlank();

        if (temNome && !temTipo) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(messages.get("dependencias.consulta.nome.requer.tipo", acceptLanguage))
                .build();
        }

        TipoConsulta tipo = null;
        if (temTipo) {
            var tipoOpt = TipoConsulta.parse(tipoParam.trim());
            if (tipoOpt.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(messages.get("dependencias.consulta.tipo.invalido", acceptLanguage))
                    .build();
            }
            tipo = tipoOpt.get();
        }

        var nomeParquet = "dependencie_" + arquivo.trim().replace(".", "_") + ".parquet";
        if (!storageResolver.resolve().arquivoDisponivel(nomeParquet)) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(messages.get("dependencias.consulta.arquivo.nao.catalogado", acceptLanguage, arquivo.trim()))
                .build();
        }

        try {
            var pacoteFiltro = (pacote != null && !pacote.isBlank()) ? pacote.trim() : null;
            var resultado = queryService.consultarDependencias(arquivo.trim(), tipo, temNome ? nome.trim() : null, pacoteFiltro);
            return Response.ok(resultado).build();
        } catch (IOException _) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(messages.get("erro.leitura.parquet", acceptLanguage))
                .build();
        }
    }

    /**
     * Lista os arquivos de dependências já catalogados no storage.
     *
     * @return lista ordenada de nomes de arquivo (ex.: {@code ["package.json", "pom.xml"]})
     */
    @GET
    @Path("/arquivos")
    @Operation(
        summary = "Listar arquivos de dependências catalogados",
        description = "Retorna a lista de arquivos de dependências que já foram catalogados " +
                      "(possuem Parquet correspondente no storage)."
    )
    @APIResponse(responseCode = "200", description = "Lista de arquivos catalogados")
    public Response listarArquivos() {
        return Response.ok(storageResolver.resolve().listarArquivosDependencias()).build();
    }

    @GET
    @Path("/sincronizar/eventos")
    @Produces("text/event-stream")
    @RestStreamElementType("text/plain")
    @Operation(
        summary = "Stream SSE de eventos de catalogação de dependências",
        description = "Emite um evento quando a catalogação termina. " +
                      "Formato: 'concluido:<arquivo>' ou 'erro:<arquivo>' (ex.: 'concluido:pom.xml')."
    )
    @APIResponse(responseCode = "200", description = "Stream SSE ativo")
    public Multi<String> eventosSincronizacao() {
        return syncEventService.eventos();
    }
}

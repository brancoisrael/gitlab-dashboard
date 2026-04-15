package br.com.ib.gitdashboards.client;

import br.com.ib.gitdashboards.dto.GitLabBlobSearchDTO;
import br.com.ib.gitdashboards.dto.GitLabProjectDTO;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import java.util.List;

@RegisterRestClient(configKey = "gitlab-api")
@Path("/api/v4")
@Produces(MediaType.APPLICATION_JSON)
public interface GitLabClient {

    @GET
    @Path("/projects")
    List<GitLabProjectDTO> listarProjetos(
        @HeaderParam("PRIVATE-TOKEN") String token,
        @QueryParam("page") int pagina,
        @QueryParam("per_page") int porPagina,
        @QueryParam("order_by") String ordenarPor,
        @QueryParam("sort") String ordem,
        @QueryParam("membership") boolean apenasComMembership,
        @QueryParam("simple") boolean simple
    );

    /**
     * Retorna o conteúdo bruto de um arquivo no repositório de um projeto.
     * Retorna 404 se o arquivo não existir no branch informado.
     *
     * @param token     Personal Access Token do GitLab
     * @param projectId ID numérico do projeto
     * @param filePath  caminho do arquivo (ex.: {@code pom.xml})
     * @param ref       branch, tag ou commit (ex.: {@code main}, {@code HEAD})
     */
    @GET
    @Path("/projects/{projectId}/repository/files/{filePath}/raw")
    @Produces(MediaType.WILDCARD)
    String obterConteudoArquivo(
        @HeaderParam("PRIVATE-TOKEN") String token,
        @PathParam("projectId") Long projectId,
        @PathParam("filePath") String filePath,
        @QueryParam("ref") String ref
    );

    /**
     * Busca arquivos cujo conteúdo corresponde ao termo informado (scope=blobs).
     * Retorna lista paginada; quando vazia, indica fim da paginação.
     *
     * @param token     Personal Access Token do GitLab
     * @param projectId ID numérico do projeto
     * @param scope     escopo da busca — usar {@code "blobs"} para busca de conteúdo
     * @param termo     texto a buscar nos arquivos
     * @param ref       branch, tag ou commit (ex.: {@code main}, {@code HEAD})
     * @param porPagina quantidade de resultados por página (máx. 100)
     * @param pagina    número da página (inicia em 1)
     */
    @GET
    @Path("/projects/{projectId}/search")
    List<GitLabBlobSearchDTO> buscarConteudo(
        @HeaderParam("PRIVATE-TOKEN") String token,
        @PathParam("projectId") Long projectId,
        @QueryParam("scope") String scope,
        @QueryParam("search") String termo,
        @QueryParam("ref") String ref,
        @QueryParam("per_page") int porPagina,
        @QueryParam("page") int pagina
    );
}

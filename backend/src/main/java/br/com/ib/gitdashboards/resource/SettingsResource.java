package br.com.ib.gitdashboards.resource;

import br.com.ib.gitdashboards.config.RuntimeStorageConfig;
import br.com.ib.gitdashboards.dto.SettingsStorageDTO;
import br.com.ib.gitdashboards.i18n.MessageService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.io.IOException;
import java.util.Map;

@Path("/settings")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Configurações", description = "Configuração de armazenamento em tempo de execução")
public class SettingsResource {

    private final RuntimeStorageConfig runtimeConfig;
    private final MessageService messages;

    @Inject
    public SettingsResource(RuntimeStorageConfig runtimeConfig, MessageService messages) {
        this.runtimeConfig = runtimeConfig;
        this.messages = messages;
    }

    @GET
    @Path("/storage")
    @Operation(
        summary = "Retorna a configuração de armazenamento atual",
        description = "A secretKey é retornada mascarada como '***' quando não está vazia."
    )
    @APIResponse(responseCode = "200", description = "Configuração retornada com sucesso")
    public SettingsStorageDTO getStorage() {
        return runtimeConfig.getForDisplay();
    }

    @POST
    @Path("/storage")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Salva a configuração de armazenamento",
        description = """
            Persiste em settings.config-file e aplica imediatamente.
            Enviar secretKey='***' preserva o valor atual sem alteração.
            """
    )
    @APIResponse(responseCode = "200", description = "Configuração salva com sucesso")
    @APIResponse(responseCode = "500", description = "Erro ao persistir a configuração")
    public Response saveStorage(
        SettingsStorageDTO dto,
        @HeaderParam("Accept-Language") String acceptLanguage
    ) {
        try {
            runtimeConfig.save(dto);
            return Response.ok(Map.of("mensagem", messages.get("configuracoes.salvas", acceptLanguage))).build();
        } catch (IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("erro", messages.get("erro.salvar.configuracoes", acceptLanguage)))
                .build();
        }
    }
}

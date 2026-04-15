package br.com.ib.gitdashboards.exception;

import br.com.ib.gitdashboards.i18n.MessageService;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<WebApplicationException> {

    private final MessageService messages;

    @Context
    HttpHeaders httpHeaders;

    @Inject
    public GlobalExceptionMapper(MessageService messages) {
        this.messages = messages;
    }

    @Override
    public Response toResponse(WebApplicationException ex) {
        var status = ex.getResponse().getStatus();
        var acceptLanguage = httpHeaders.getHeaderString(HttpHeaders.ACCEPT_LANGUAGE);
        var mensagem = messages.get(chavePorStatus(status), acceptLanguage);
        return Response.status(status)
            .type(MediaType.APPLICATION_JSON)
            .entity(new ErroDTO(mensagem, ex.getMessage(), status))
            .build();
    }

    private String chavePorStatus(int status) {
        return switch (status) {
            case 401 -> "erro.gitlab.token.invalido";
            case 403 -> "erro.gitlab.acesso.negado";
            case 404 -> "erro.gitlab.nao.encontrado";
            case 429 -> "erro.gitlab.limite.requisicoes";
            default -> "erro.gitlab.comunicacao";
        };
    }

    public record ErroDTO(String erro, String detalhe, int status) {}
}

package br.com.ib.gitdashboards.service;

import br.com.ib.gitdashboards.client.GitLabClient;
import br.com.ib.gitdashboards.config.GitLabConfig;
import br.com.ib.gitdashboards.dto.GitLabBlobSearchDTO;
import br.com.ib.gitdashboards.dto.GitLabProjectDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class GitLabService {

    private static final Logger log = Logger.getLogger(GitLabService.class);

    private final GitLabConfig config;
    private final GitLabClient gitLabClient;

    @Inject
    public GitLabService(GitLabConfig config, @RestClient GitLabClient gitLabClient) {
        this.config = config;
        this.gitLabClient = gitLabClient;
    }

    /**
     * Lista projetos com paginação.
     *
     * <p><b>Retry:</b> até 3 tentativas com backoff de 2 s em erros 5xx e falhas de conexão.
     * Erros 4xx (ex.: 401, 403, 404) abortam imediatamente sem retry.
     *
     * <p><b>Circuit Breaker:</b> abre após 50 % de falhas em 10 requisições consecutivas;
     * tenta recuperação após 30 s. Apenas erros 5xx e falhas de conexão contam como falha.
     */
    @Retry(
        maxRetries = 3,
        delay = 2,
        delayUnit = ChronoUnit.SECONDS,
        retryOn = {ServerErrorException.class, ProcessingException.class}
    )
    @CircuitBreaker(
        requestVolumeThreshold = 10,
        failureRatio = 0.5,
        delay = 30,
        delayUnit = ChronoUnit.SECONDS,
        successThreshold = 2,
        failOn = {ServerErrorException.class, ProcessingException.class}
    )
    public List<GitLabProjectDTO> listarProjetos(int pagina, int porPagina) {
        var limite = Math.min(porPagina, config.api().pagination().maxPerPage());
        return gitLabClient.listarProjetos(
            config.token(),
            pagina,
            limite,
            "last_activity_at",
            "desc",
            false,
            true
        );
    }

    /**
     * Tenta obter o conteúdo bruto de um arquivo no repositório do projeto.
     * Retorna {@link Optional#empty()} se o arquivo não existir (404) sem gerar erro.
     * Erros 5xx e falhas de conexão são retentados até 3 vezes com backoff de 2 s.
     *
     * @param projectId ID numérico do projeto GitLab
     * @param filePath  nome ou caminho do arquivo (ex.: {@code pom.xml})
     * @param branch    branch de referência; {@code null} usa {@code "HEAD"}
     */
    @Retry(
        maxRetries = 3,
        delay = 2,
        delayUnit = ChronoUnit.SECONDS,
        retryOn = {ServerErrorException.class, ProcessingException.class}
    )
    public Optional<String> tentarObterArquivo(Long projectId, String filePath, String branch) {
        try {
            var conteudo = gitLabClient.obterConteudoArquivo(
                config.token(),
                projectId,
                filePath,
                branch != null ? branch : "HEAD"
            );
            return Optional.of(conteudo);
        } catch (NotFoundException _) {
            return Optional.empty();
        }
    }

    /**
     * Busca arquivos cujo conteúdo contenha {@code termo} e cuja extensão termine com {@code extensao},
     * percorrendo todas as páginas disponíveis da API de busca do GitLab.
     *
     * <p>Erros 4xx (projeto inacessível, busca não disponível) são tratados como resultado vazio.
     * Erros 5xx abortam silenciosamente com o que já foi coletado até o momento.
     * Resultados duplicados (mesmo caminho de arquivo) são descartados.
     *
     * @param projectId ID numérico do projeto GitLab
     * @param branch    branch de referência; {@code null} usa {@code "HEAD"}
     * @param termo     texto a buscar no conteúdo dos arquivos
     * @param extensao  extensão dos arquivos a retornar (ex.: {@code ".java"}); {@code null} retorna todos
     * @return lista de blobs únicos que correspondem aos critérios; vazia se nenhum encontrado
     */
    public List<GitLabBlobSearchDTO> buscarArquivos(Long projectId, String branch, String termo, String extensao) {
        var ref = branch != null ? branch : "HEAD";
        var coletados = new LinkedHashMap<String, GitLabBlobSearchDTO>();
        int pagina = 1;
        try {
            while (true) {
                var resultado = gitLabClient.buscarConteudo(config.token(), projectId, "blobs", termo, ref, 100, pagina);
                if (resultado.isEmpty()) break;
                resultado.stream()
                    .filter(b -> extensao == null || extensao.isBlank()
                        || b.filename().toLowerCase().endsWith(extensao.toLowerCase()))
                    .forEach(b -> coletados.putIfAbsent(b.path(), b));
                pagina++;
            }
        } catch (WebApplicationException e) {
            var status = e.getResponse().getStatus();
            if (status >= 500) {
                log.warnf("Busca interrompida no projeto %d (página %d) — status %d", projectId, pagina, status);
            }
            // 4xx: projeto sem acesso ou sem suporte a blobs — retorna o que foi coletado até agora
        } catch (ProcessingException e) {
            log.warnf("Falha de conexão ao buscar no projeto %d: %s", projectId, e.getMessage());
        }
        return List.copyOf(coletados.values());
    }
}

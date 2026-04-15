package br.com.ib.gitdashboards.storage;

import br.com.ib.gitdashboards.config.RuntimeStorageConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@ApplicationScoped
public class S3ProjectStorage implements ProjectStorageService {

    private static final Logger log = Logger.getLogger(S3ProjectStorage.class);
    private static final String EXT_PARQUET = ".parquet";

    private final RuntimeStorageConfig runtimeConfig;

    @Inject
    public S3ProjectStorage(RuntimeStorageConfig runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    @Override
    public Path criarArquivoTemporario() throws IOException {
        return Files.createTempFile("projetos-gitlab-", ".parquet.tmp");
    }

    @Override
    public Path obterArquivoParaLeitura(String nomeArquivo) throws IOException {
        var temp = Files.createTempFile("parquet-leitura-", EXT_PARQUET);
        try (var s3 = buildClient()) {
            s3.getObject(
                GetObjectRequest.builder()
                    .bucket(runtimeConfig.bucket())
                    .key(nomeArquivo)
                    .build(),
                temp
            );
        }
        return temp;
    }

    @Override
    public boolean arquivoDisponivel(String nomeArquivo) {
        try (var s3 = buildClient()) {
            s3.headObject(HeadObjectRequest.builder()
                .bucket(runtimeConfig.bucket())
                .key(nomeArquivo)
                .build());
            return true;
        } catch (NoSuchKeyException _) {
            return false;
        }
    }

    @Override
    public void confirmar(Path arquivoTemp, String nomeArquivo) throws IOException {
        try (var s3 = buildClient()) {
            s3.putObject(
                PutObjectRequest.builder()
                    .bucket(runtimeConfig.bucket())
                    .key(nomeArquivo)
                    .build(),
                arquivoTemp
            );
            log.infof("Parquet enviado para S3: s3://%s/%s", runtimeConfig.bucket(), nomeArquivo);
        } finally {
            Files.deleteIfExists(arquivoTemp);
        }
    }

    @Override
    public List<String> listarArquivosDependencias() {
        try (var s3 = buildClient()) {
            return s3.listObjectsV2(ListObjectsV2Request.builder()
                        .bucket(runtimeConfig.bucket())
                        .prefix("dependencie_")
                        .build())
                    .contents().stream()
                    .map(obj -> obj.key())
                    .filter(key -> key.endsWith(EXT_PARQUET))
                    .map(key -> key.substring("dependencie_".length(), key.length() - EXT_PARQUET.length())
                                   .replace("_", "."))
                    .sorted()
                    .toList();
        } catch (Exception e) {
            log.warnf("Erro ao listar arquivos de dependências no S3: %s", e.getMessage());
            return List.of();
        }
    }

    private S3Client buildClient() {
        return S3Client.builder()
            .endpointOverride(URI.create(runtimeConfig.endpoint()))
            .region(Region.of(runtimeConfig.region()))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(runtimeConfig.accessKey(), runtimeConfig.secretKey())
            ))
            .forcePathStyle(true)
            .httpClient(UrlConnectionHttpClient.create())
            .build();
    }
}

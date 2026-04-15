package br.com.ib.gitdashboards.storage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Abstração de persistência para arquivos Parquet.
 * Implementações disponíveis: {@link LocalProjectStorage} e {@link S3ProjectStorage}.
 * A seleção é feita por {@link StorageResolver} com base em {@code storage.type}.
 */
public interface ProjectStorageService {

    /**
     * Cria um arquivo temporário local para escrita incremental do Parquet.
     * O chamador é responsável por invocar {@link #confirmar} ao final ou
     * deletar o arquivo em caso de falha.
     */
    Path criarArquivoTemporario() throws IOException;

    /**
     * Confirma o arquivo Parquet no destino final (diretório local ou bucket S3).
     * Após esta chamada, o arquivo temporário não existe mais.
     *
     * @param arquivoTemp arquivo temporário gerado por {@link #criarArquivoTemporario}
     * @param nomeArquivo nome de destino (ex.: {@code projetos.parquet})
     */
    void confirmar(Path arquivoTemp, String nomeArquivo) throws IOException;

    /**
     * Retorna um {@link Path} local apontando para o arquivo Parquet com o nome informado.
     * Em implementações remotas (S3), faz download para arquivo temporário.
     *
     * @param nomeArquivo nome do arquivo no storage (ex.: {@code projetos.parquet})
     */
    Path obterArquivoParaLeitura(String nomeArquivo) throws IOException;

    /**
     * Verifica se o arquivo Parquet com o nome informado existe e está disponível para leitura.
     *
     * @param nomeArquivo nome do arquivo no storage
     * @return {@code true} se o arquivo existe e pode ser lido
     */
    boolean arquivoDisponivel(String nomeArquivo);

    /** Atalho para {@code obterArquivoParaLeitura("projetos.parquet")}. */
    default Path obterArquivoParaLeitura() throws IOException {
        return obterArquivoParaLeitura("projetos.parquet");
    }

    /** Atalho para {@code arquivoDisponivel("projetos.parquet")}. */
    default boolean arquivoDisponivel() {
        return arquivoDisponivel("projetos.parquet");
    }

    /**
     * Lista os arquivos de dependências já catalogados no storage.
     *
     * <p>Varre os arquivos Parquet com prefixo {@code dependencie_} e converte de volta
     * ao nome original (ex.: {@code dependencie_pom_xml.parquet} → {@code pom.xml}).
     *
     * @return lista ordenada de nomes originais de arquivo; vazia se não houver nenhum
     */
    List<String> listarArquivosDependencias();
}

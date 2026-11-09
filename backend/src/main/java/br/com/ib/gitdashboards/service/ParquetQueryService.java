package br.com.ib.gitdashboards.service;

import br.com.ib.gitdashboards.dto.DependenciaConsultaDTO;
import br.com.ib.gitdashboards.dto.DependenciaItemDTO;
import br.com.ib.gitdashboards.dto.ProjetoDetalheDTO;
import br.com.ib.gitdashboards.dto.ProjetoGrupoDTO;
import br.com.ib.gitdashboards.dto.ProjetoResumoDTO;
import br.com.ib.gitdashboards.dto.TipoConsulta;
import br.com.ib.gitdashboards.storage.LocalInputFile;
import br.com.ib.gitdashboards.storage.StorageResolver;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.parquet.avro.AvroParquetReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lê o arquivo Parquet de projetos e produz agrupamentos analíticos.
 */
@ApplicationScoped
public class ParquetQueryService {

    private final StorageResolver storageResolver;

    @Inject
    public ParquetQueryService(StorageResolver storageResolver) {
        this.storageResolver = storageResolver;
    }

    /**
     * Agrupa projetos pelo nome de primeiro nível de {@code name_with_namespace}.
     * A regra é: tudo que vier antes do primeiro " / " é o grupo.
     * Ex.: "ContaPag / repo-x" → grupo "ContaPag".
     *
     * @return lista ordenada por {@code count} decrescente
     */
    public List<ProjetoGrupoDTO> contarPorGrupo() throws IOException {
        var arquivo = storageResolver.resolve().obterArquivoParaLeitura();

        var contagem = new HashMap<String, Long>();
        try (var reader = AvroParquetReader.<GenericRecord>builder(new LocalInputFile(arquivo))
                .withDataModel(GenericData.get())
                .build()) {
            GenericRecord registro;
            while ((registro = reader.read()) != null) {
                var valor = registro.get("name_with_namespace");
                var grupo = extrairGrupo(valor != null ? valor.toString() : null);
                contagem.merge(grupo, 1L, Long::sum);
            }
        }

        return contagem.entrySet().stream()
            .map(e -> new ProjetoGrupoDTO(e.getKey(), e.getValue()))
            .sorted(Comparator.comparingLong(ProjetoGrupoDTO::count).reversed())
            .toList();
    }

    /**
     * Retorna os projetos que pertencem ao grupo informado, ordenados por nome.
     * O grupo é extraído do campo {@code name_with_namespace} da mesma forma que
     * {@link #contarPorGrupo()}.
     *
     * @param grupo nome do grupo (ex.: "ContaPag")
     * @return lista de detalhes dos projetos do grupo, ordenada por nome
     */
    public List<ProjetoDetalheDTO> listarPorGrupo(String grupo) throws IOException {
        var arquivo = storageResolver.resolve().obterArquivoParaLeitura();
        var projetos = new ArrayList<ProjetoDetalheDTO>();

        try (var reader = AvroParquetReader.<GenericRecord>builder(new LocalInputFile(arquivo))
                .withDataModel(GenericData.get())
                .build()) {
            GenericRecord registro;
            while ((registro = reader.read()) != null) {
                var nameWithNs    = registro.get("name_with_namespace");
                var grupoRegistro = extrairGrupo(nameWithNs != null ? nameWithNs.toString() : null);
                var nome          = valorCampo(registro, "name");
                if (grupo.equals(grupoRegistro) && nome != null) {
                    projetos.add(new ProjetoDetalheDTO(
                        nome,
                        valorCampo(registro, "web_url"),
                        valorCampo(registro, "created_at"),
                        valorCampo(registro, "last_activity_at")
                    ));
                }
            }
        }

        projetos.sort(Comparator.comparing(ProjetoDetalheDTO::nome, String.CASE_INSENSITIVE_ORDER));
        return projetos;
    }

    /**
     * Retorna todos os projetos armazenados no Parquet como resumos leves.
     * Usado pelo {@code DependencySyncService} para iterar projetos sem re-consultar a API GitLab.
     *
     * @return lista de resumos; projetos sem {@code id} ou {@code name} são descartados
     */
    public List<ProjetoResumoDTO> listarTodos() throws IOException {
        var arquivo = storageResolver.resolve().obterArquivoParaLeitura();
        var projetos = new ArrayList<ProjetoResumoDTO>();

        try (var reader = AvroParquetReader.<GenericRecord>builder(new LocalInputFile(arquivo))
                .withDataModel(GenericData.get())
                .build()) {
            GenericRecord registro;
            while ((registro = reader.read()) != null) {
                var idVal     = registro.get("id");
                var nameVal   = registro.get("name");
                var branchVal = registro.get("default_branch");
                var urlVal    = registro.get("web_url");
                if (idVal == null || nameVal == null) continue;
                projetos.add(new ProjetoResumoDTO(
                    (Long) idVal,
                    nameVal.toString(),
                    branchVal != null ? branchVal.toString() : null,
                    urlVal    != null ? urlVal.toString()    : null
                ));
            }
        }

        return projetos;
    }

    /**
     * Consulta o arquivo Parquet de dependências gerado para {@code nomeArquivo}.
     *
     * <p>Comportamento por combinação de parâmetros:
     * <ul>
     *   <li>{@code tipo=PROJETO, nome=X} → retorna um item com as dependências do projeto X.</li>
     *   <li>{@code tipo=DEPENDENCIA, nome=X} → retorna um item com os projetos que usam a dependência X.</li>
     *   <li>{@code tipo=null, nome=null} → retorna todos os projetos com suas dependências.</li>
     * </ul>
     *
     * @param nomeArquivo arquivo de origem (ex.: {@code pom.xml}); o Parquet é resolvido pela regra
     *                    {@code dependencie_<arquivo>.parquet}
     * @param tipo        filtro de tipo; {@code null} retorna todos os projetos
     * @param nome        valor a filtrar; {@code null} quando {@code tipo} também for {@code null}
     * @return lista de resultados; vazia se nenhum registro corresponder
     */
    /**
     * Consulta o arquivo Parquet de dependências gerado para {@code nomeArquivo}.
     *
     * <p>Comportamento por combinação de parâmetros:
     * <ul>
     *   <li>{@code tipo=PROJETO, nome=X} → retorna um item com as dependências do projeto X.</li>
     *   <li>{@code tipo=DEPENDENCIA, nome=X} → retorna um item com os projetos que usam a dependência X.</li>
     *   <li>{@code tipo=null, nome=null} → retorna todos os projetos com suas dependências.</li>
     * </ul>
     * O filtro {@code pacote}, quando informado, é aplicado como busca parcial case-insensitive
     * no campo {@code pacote} do Parquet, independentemente dos outros filtros.
     *
     * @param nomeArquivo arquivo de origem (ex.: {@code pom.xml})
     * @param tipo        filtro de tipo; {@code null} retorna todos os projetos
     * @param nome        valor a filtrar; {@code null} quando {@code tipo} também for {@code null}
     * @param pacote      substring a buscar no campo pacote (ex.: {@code "br.com"}); {@code null} ignora o filtro
     * @return lista de resultados; vazia se nenhum registro corresponder
     */
    public List<DependenciaConsultaDTO> consultarDependencias(
        String nomeArquivo, TipoConsulta tipo, String nome, String pacote
    ) throws IOException {
        var nomeParquet = "dependencie_" + nomeArquivo.replace(".", "_") + ".parquet";
        var path = storageResolver.resolve().obterArquivoParaLeitura(nomeParquet);

        boolean porDependencia = tipo == TipoConsulta.DEPENDENCIA;
        var pacoteLower = pacote != null ? pacote.toLowerCase() : null;
        var grupos = new LinkedHashMap<String, List<DependenciaItemDTO>>();
        var urlsProjetos = urlsPorNomeProjeto();

        try (var reader = AvroParquetReader.<GenericRecord>builder(new LocalInputFile(path))
                .withDataModel(GenericData.get())
                .build()) {
            GenericRecord reg;
            while ((reg = reader.read()) != null) {
                var aplicacao   = valorCampo(reg, "aplicacao");
                var dependencia = valorCampo(reg, "dependencia");
                var versao      = valorCampo(reg, "versao");
                var pacoteVal   = valorCampo(reg, "pacote");

                var chave = porDependencia ? dependencia : aplicacao;
                var valor = porDependencia ? aplicacao   : dependencia;

                if (chave != null && valor != null
                    && (nome == null || chave.equals(nome))
                    && (pacoteLower == null || (pacoteVal != null && pacoteVal.toLowerCase().contains(pacoteLower)))) {
                    grupos.computeIfAbsent(chave, chaveNova -> new ArrayList<>())
                          .add(new DependenciaItemDTO(valor, versao, porDependencia ? urlsProjetos.get(valor) : null));
                }
            }
        }

        var tipoStr = porDependencia ? "dependencia" : "projeto";
        return grupos.entrySet().stream()
            .map(e -> new DependenciaConsultaDTO(
                tipoStr, e.getKey(), porDependencia ? null : urlsProjetos.get(e.getKey()), e.getValue()))
            .toList();
    }

    /** Mapa nome do projeto → URL no GitLab; em caso de nomes repetidos prevalece o primeiro. */
    private Map<String, String> urlsPorNomeProjeto() throws IOException {
        var urls = new HashMap<String, String>();
        for (var projeto : listarTodos()) {
            if (projeto.webUrl() != null) urls.putIfAbsent(projeto.name(), projeto.webUrl());
        }
        return urls;
    }

    private String valorCampo(GenericRecord record, String campo) {
        var val = record.get(campo);
        return val != null ? val.toString() : null;
    }

    private String extrairGrupo(String nameWithNamespace) {
        if (nameWithNamespace == null || nameWithNamespace.isBlank()) return "Sem grupo";
        var idx = nameWithNamespace.indexOf(" / ");
        return idx >= 0 ? nameWithNamespace.substring(0, idx).trim() : nameWithNamespace.trim();
    }
}

package br.com.ib.gitdashboards.storage;

import br.com.ib.gitdashboards.dto.DependenciaDTO;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Fábrica e mapeador de registros Parquet para dependências de projetos.
 * Todas as colunas do schema são anuláveis para garantir equidade entre
 * arquivos gerados para diferentes linguagens/gerenciadores.
 */
@ApplicationScoped
public class ParquetDependencyWriter {

    private static final String AVRO_SCHEMA_JSON = """
        {
          "type": "record",
          "name": "Dependencia",
          "namespace": "br.com.ib.gitdashboards",
          "fields": [
            {"name": "aplicacao",      "type": ["null", "string"], "default": null},
            {"name": "dependencia",    "type": ["null", "string"], "default": null},
            {"name": "versao",         "type": ["null", "string"], "default": null},
            {"name": "pacote",         "type": ["null", "string"], "default": null},
            {"name": "gerenciador",    "type": ["null", "string"], "default": null},
            {"name": "arquivo_origem", "type": ["null", "string"], "default": null}
          ]
        }
        """;

    static final Schema SCHEMA = new Schema.Parser().parse(AVRO_SCHEMA_JSON);

    /**
     * Abre um {@link ParquetWriter} apontado para {@code arquivo}.
     * O chamador é responsável por fechar o writer (use try-with-resources).
     */
    public ParquetWriter<GenericRecord> abrirEscritor(Path arquivo) throws IOException {
        return AvroParquetWriter.<GenericRecord>builder(new LocalOutputFile(arquivo))
            .withSchema(SCHEMA)
            .withCompressionCodec(CompressionCodecName.SNAPPY)
            .withPageSize(ParquetWriter.DEFAULT_PAGE_SIZE)
            .withRowGroupSize((long) ParquetWriter.DEFAULT_BLOCK_SIZE)
            .build();
    }

    public GenericRecord toRecord(DependenciaDTO dto) {
        var registro = new GenericData.Record(SCHEMA);
        registro.put("aplicacao",      dto.aplicacao());
        registro.put("dependencia",    dto.dependencia());
        registro.put("versao",         dto.versao());
        registro.put("pacote",         dto.pacote());
        registro.put("gerenciador",    dto.gerenciador());
        registro.put("arquivo_origem", dto.arquivoOrigem());
        return registro;
    }
}

package br.com.ib.gitdashboards.storage;

import br.com.ib.gitdashboards.dto.GitLabProjectDTO;
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
 * Fábrica e mapeador de registros Parquet para projetos GitLab.
 * Usa {@link LocalOutputFile} para escrever diretamente em {@link Path} local
 * sem dependência do Hadoop runtime.
 */
@ApplicationScoped
public class ParquetProjectWriter {

    private static final String AVRO_SCHEMA_JSON = """
        {
          "type": "record",
          "name": "GitLabProject",
          "namespace": "br.com.ib.gitdashboards",
          "fields": [
            {"name": "id",                  "type": ["null", "long"],   "default": null},
            {"name": "name",                "type": ["null", "string"], "default": null},
            {"name": "name_with_namespace", "type": ["null", "string"], "default": null},
            {"name": "path",                "type": ["null", "string"], "default": null},
            {"name": "path_with_namespace", "type": ["null", "string"], "default": null},
            {"name": "description",         "type": ["null", "string"], "default": null},
            {"name": "web_url",             "type": ["null", "string"], "default": null},
            {"name": "visibility",          "type": ["null", "string"], "default": null},
            {"name": "default_branch",      "type": ["null", "string"], "default": null},
            {"name": "star_count",          "type": ["null", "int"],    "default": null},
            {"name": "forks_count",         "type": ["null", "int"],    "default": null},
            {"name": "open_issues_count",   "type": ["null", "int"],    "default": null},
            {"name": "created_at",          "type": ["null", "string"], "default": null},
            {"name": "last_activity_at",    "type": ["null", "string"], "default": null}
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

    public GenericRecord toRecord(GitLabProjectDTO dto) {
        var projeto = new GenericData.Record(SCHEMA);
        projeto.put("id",                  dto.id());
        projeto.put("name",                dto.name());
        projeto.put("name_with_namespace", dto.nameWithNamespace());
        projeto.put("path",                dto.path());
        projeto.put("path_with_namespace", dto.pathWithNamespace());
        projeto.put("description",         dto.description());
        projeto.put("web_url",             dto.webUrl());
        projeto.put("visibility",          dto.visibility());
        projeto.put("default_branch",      dto.defaultBranch());
        projeto.put("star_count",          dto.starCount());
        projeto.put("forks_count",         dto.forksCount());
        projeto.put("open_issues_count",   dto.openIssuesCount());
        projeto.put("created_at",          dto.createdAt() != null ? dto.createdAt().toString() : null);
        projeto.put("last_activity_at",    dto.lastActivityAt() != null ? dto.lastActivityAt().toString() : null);
        return projeto;
    }
}

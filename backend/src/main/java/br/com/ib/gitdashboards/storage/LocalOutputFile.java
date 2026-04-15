package br.com.ib.gitdashboards.storage;

import org.apache.parquet.io.OutputFile;
import org.apache.parquet.io.PositionOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * Implementação de {@link OutputFile} para {@link java.nio.file.Path} local,
 * sem dependência do Hadoop runtime. Usada pelo {@link ParquetProjectWriter}
 * para escrever arquivos Parquet diretamente no sistema de arquivos local.
 */
public class LocalOutputFile implements OutputFile {

    private final Path path;

    public LocalOutputFile(Path path) {
        this.path = path;
    }

    @Override
    public PositionOutputStream create(long blockSizeHint) throws IOException {
        return new TrackingOutputStream(
            Files.newOutputStream(path, CREATE, WRITE, TRUNCATE_EXISTING)
        );
    }

    @Override
    public PositionOutputStream createOrOverwrite(long blockSizeHint) throws IOException {
        return create(blockSizeHint);
    }

    @Override
    public boolean supportsBlockSize() {
        return false;
    }

    @Override
    public long defaultBlockSize() {
        return 0L;
    }

    private static class TrackingOutputStream extends PositionOutputStream {

        private final OutputStream out;
        private long pos = 0;

        TrackingOutputStream(OutputStream out) {
            this.out = out;
        }

        @Override
        public long getPos() {
            return pos;
        }

        @Override
        public void write(int b) throws IOException {
            out.write(b);
            pos++;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
            pos += len;
        }

        @Override
        public void flush() throws IOException {
            out.flush();
        }

        @Override
        public void close() throws IOException {
            out.close();
        }
    }
}

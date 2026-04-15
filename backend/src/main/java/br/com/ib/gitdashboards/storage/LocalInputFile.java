package br.com.ib.gitdashboards.storage;

import org.apache.parquet.io.InputFile;
import org.apache.parquet.io.SeekableInputStream;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Implementação de {@link InputFile} para {@link Path} local,
 * sem dependência do Hadoop runtime. Usada pelo {@link ParquetQueryService}
 * para ler arquivos Parquet diretamente do sistema de arquivos local.
 */
public class LocalInputFile implements InputFile {

    private final Path path;

    public LocalInputFile(Path path) {
        this.path = path;
    }

    @Override
    public long getLength() throws IOException {
        return Files.size(path);
    }

    @Override
    @SuppressWarnings("java:S2095") // channel é fechado no close() do SeekableInputStream retornado
    public SeekableInputStream newStream() throws IOException {
        var channel = Files.newByteChannel(path);
        return new SeekableInputStream() {

            @Override
            public void seek(long newPos) throws IOException {
                channel.position(newPos);
            }

            @Override
            public long getPos() throws IOException {
                return channel.position();
            }

            @Override
            public int read() throws IOException {
                var buf = ByteBuffer.allocate(1);
                var n = channel.read(buf);
                return n == -1 ? -1 : (buf.get(0) & 0xFF);
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                return channel.read(ByteBuffer.wrap(b, off, len));
            }

            @Override
            public void readFully(byte[] bytes) throws IOException {
                readFully(bytes, 0, bytes.length);
            }

            @Override
            public void readFully(byte[] bytes, int start, int len) throws IOException {
                var totalRead = 0;
                while (totalRead < len) {
                    var n = channel.read(ByteBuffer.wrap(bytes, start + totalRead, len - totalRead));
                    if (n == -1) throw new EOFException();
                    totalRead += n;
                }
            }

            @Override
            public int read(ByteBuffer buf) throws IOException {
                return channel.read(buf);
            }

            @Override
            public void readFully(ByteBuffer buf) throws IOException {
                while (buf.hasRemaining()) {
                    var n = channel.read(buf);
                    if (n == -1) throw new EOFException();
                }
            }

            @Override
            public void close() throws IOException {
                channel.close();
            }
        };
    }
}

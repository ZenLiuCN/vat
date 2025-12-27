package vat.api.utils;

import io.vertx.core.buffer.Buffer;
import lombok.EqualsAndHashCode;

import java.io.IOException;
import java.io.OutputStream;
@SuppressWarnings("unused")
@EqualsAndHashCode(callSuper = true)
public class BufferOutputStream extends OutputStream {
    public final Buffer buf;

    public BufferOutputStream() {
        this.buf = Buffer.buffer();
    }

    public BufferOutputStream(int initialCapacity) {
        this.buf = Buffer.buffer(initialCapacity);
    }

    public BufferOutputStream(Buffer buf) {
        this.buf = buf;
    }

    @Override
    public void write(int i) {
        buf.appendByte((byte) i);
    }

    @Override
    public void write(byte[] b) {
        buf.appendBytes(b);
    }

    @Override
    public void write(byte[] b, int off, int len) {
        buf.appendBytes(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        super.flush();
    }

    @Override
    public void close() throws IOException {
        super.close();
    }
}

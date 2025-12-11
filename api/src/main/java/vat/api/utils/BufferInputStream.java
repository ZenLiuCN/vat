package vat.api.utils;

import io.vertx.core.buffer.Buffer;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;

@EqualsAndHashCode(callSuper = true)
public class BufferInputStream extends InputStream {
    public final Buffer buf;
    protected AtomicInteger position = new AtomicInteger(0);

    public BufferInputStream(Buffer buf) {
        this.buf = buf;
    }

    public BufferInputStream() {
        this.buf = Buffer.buffer();
    }

    public BufferInputStream(int initialCapacity) {
        this.buf = Buffer.buffer(initialCapacity);
    }

    @Override
    public long transferTo(OutputStream out) throws IOException {
        if (out instanceof BufferOutputStream o) {
            buf.writeToBuffer(o.buf);
            return buf.length();
        }
        return super.transferTo(out);
    }

    @Override
    public int read() {
        if (position.get() >= buf.length()) {
            return -1;
        }
        return buf.getByte(position.incrementAndGet()) & 0xff;
    }

    @Override
    public int available() {
        return buf.length() - position.get();
    }

    @Override
    public byte[] readAllBytes() {
        checkClosed();
        try {
            return buf.getBytes(position.get(), buf.length());
        } finally {
            position.addAndGet(buf.length());
        }
    }

    @Override
    public int read(byte[] b, int offset, int length) {
        checkClosed();
        if (position.get() >= buf.length()) {
            return -1;
        }
        int bytesToRead = Math.min(length, buf.length() - position.get());
        byte[] bytes = buf.getBytes(position.get(), position.addAndGet(bytesToRead));
        System.arraycopy(bytes, 0, b, offset, bytesToRead);
        return bytesToRead;
    }

    @Override
    public int read(byte[] b) {
        return read(b, 0, b.length);
    }

    @Override
    public void reset() {
        checkClosed();
        position.set(0);
    }

    @Override
    public byte[] readNBytes(int len) {
        checkClosed();
        return buf.getBytes(position.get(), position.addAndGet(len));
    }


    @Override
    public long skip(long n) throws IOException {
        checkClosed();
        if (n <= 0) {
            return 0;
        }
        var p = position.get();
        try {
            if (n < buf.length() - p) {
                p += (int) n;
                return n;
            } else {
                p = buf.length();
                return buf.length() - p;
            }
        } finally {
            position.set(p);
        }
    }

    @Override
    public void skipNBytes(long n) throws IOException {
        checkClosed();
        if (n <= 0) {
            return;
        }
        var p = position.get();
        if (n < buf.length() - p) {
            p += (int) n;
            position.set(p);
        } else {
            p = buf.length();
            throw new EOFException();
        }
    }

    @Override
    public void close() {
        position.set(-2);
    }

    protected boolean isEOF() {
        checkClosed();
        return available() <= 0;
    }

    @SneakyThrows
    protected void checkClosed() {
        if (position.get()== - 2) throw new IllegalStateException("already closed");
    }
}

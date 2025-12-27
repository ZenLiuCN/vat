package vat.api.utils;


import io.vertx.core.buffer.Buffer;

import java.util.concurrent.atomic.AtomicInteger;
@SuppressWarnings("unused")
public record BufferRandomAccessWriter(
        Buffer buf,
        AtomicInteger pos
) implements AutoCloseable {

    void checkClosed() {
        if (pos.get() == -2) throw new IllegalStateException("already closed");
    }

    public Buffer buf() {
        return buf;
    }

    public BufferRandomAccessWriter(Buffer buf) {
        this(buf, new AtomicInteger(0));
    }


    public void write(int b) {
        checkClosed();
        buf.setByte(pos.getAndIncrement(), (byte) b);
    }


    public void write(byte[] b) {
        checkClosed();
        buf.setBytes(pos.getAndAdd(b.length), b);
    }


    public void write(byte[] b, int offset, int length) {
        checkClosed();
        buf.setBytes(pos.getAndAdd(length), b, offset, length);
    }


    public void clear() {
        checkClosed();
        pos.set(0);
    }

    @Override
    public void close() {
        pos.set(-2);
    }
}

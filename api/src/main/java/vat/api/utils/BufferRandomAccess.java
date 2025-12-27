package vat.api.utils;


import io.vertx.core.buffer.Buffer;

import java.util.concurrent.atomic.AtomicInteger;
@SuppressWarnings("unused")
public record BufferRandomAccess(
        Buffer buf,
        AtomicInteger pos
) implements AutoCloseable {
    public BufferRandomAccess(Buffer buf) {
        this(buf, new AtomicInteger(0));
    }
    //region Reader
    public int read() {
        if (isEOF()) return -1;
        return buf.getByte(pos.getAndIncrement());
    }


    public int available() {
        checkClosed();
        return buf.length() - pos.get();
    }


    public int peek() {
        checkClosed();
        return buf.getByte(pos.get());
    }


    public int read(byte[] b, int offset, int length) {
        checkClosed();
        if (isEOF()) return -1;
        var next = pos.get();
        var max = buf.length();
        var available = max - next;
        if (length >= available) {
            buf.getBytes(next, max, b, offset);
            pos.set(max);
            return available;
        }
        buf.getBytes(next, next + length, b, offset);
        pos.set(next + length);
        return length;
    }


    public long getPosition() {
        return pos.get();
    }


    public void seek(long position) {
        checkClosed();
        pos.set((int) position);
    }


    public long length() {
        return buf.length();
    }

    public void skip(int length) {
        checkClosed();
        pos.addAndGet(length);
    }

    public int read(byte[] b) {
        return read(b, 0, b.length);
    }

    public void rewind(int bytes) {
        checkClosed();
        pos.addAndGet(-bytes);
    }

    public boolean isClosed() {
        return pos.get() == -2;
    }


    public boolean isEOF() {
        checkClosed();
        return available() == 0;
    }
    //endregion
    public void close() {
        pos.set(-2);
    }
    void checkClosed() {
        if (pos.get() == -2) throw new IllegalStateException("already closed");
    }
    //region Writer
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
    //endregion

}

package vat.api.utils;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.internal.buffer.BufferInternal;
import lombok.EqualsAndHashCode;
import org.jspecify.annotations.Nullable;


import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

@SuppressWarnings("unused")
@EqualsAndHashCode(callSuper = false, exclude = "pos")
public class BufferReader extends Reader {
    public final Buffer buf;
    protected volatile int pos;

    public Buffer buf() {
        return buf;
    }

    public BufferReader(Buffer buf) {
        this.buf = buf;
    }

    protected final static Lazy<CharsetDecoder> decoder = Lazy.of(() -> StandardCharsets.UTF_8.newDecoder()
                                                                                              .onMalformedInput(
                                                                                                      CodingErrorAction.REPLACE)
                                                                                              .onUnmappableCharacter(
                                                                                                      CodingErrorAction.REPLACE));

    public @Nullable Buffer slice(int length) {
        checkClosed();
        if (isEOF()) return null;
        var next = pos;
        var max = buf.length();
        var available = max - next;
        if (available < length) {
            pos = (max);
            return buf.slice(next, length);
        }
        pos = (next + length);
        return buf.slice(next, next + length);
    }

    @Override
    public int read(char[] b, int offset, int length) throws IOException {
        checkClosed();
        if (isEOF()) return -1;
        var byteLength = length * 4;
        var buf = slice(byteLength);
        if (buf == null) return -1;
        var charBuffer = CharBuffer.wrap(b, offset, length);
        var dec = decoder.get();
        dec.reset();
        var result = dec.decode(((BufferInternal) buf).getByteBuf().nioBuffer(), charBuffer, false);
        if (result.isError()) {
            result.throwException();
        }
        return charBuffer.position() - offset;
    }

    public boolean isClosed() {
        return pos == -2;
    }


    public boolean isEOF() {
        checkClosed();
        return available() == 0;
    }

    public int available() {
        checkClosed();
        return buf.length() - pos;
    }

    @Override
    public void close() {
        pos = -2;
    }

    protected void checkClosed() {
        if (pos == -2) throw new IllegalStateException("already closed");
    }

}

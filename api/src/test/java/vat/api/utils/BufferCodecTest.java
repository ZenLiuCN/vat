package vat.api.utils;

import io.vertx.core.buffer.Buffer;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BufferCodecTest {
    @Test
    void base94(){
        var src="d指定中文∞收拾东西哦";
        var enc= BufferCodec.Chars.BASE94.encode(Buffer.buffer(src.getBytes(StandardCharsets.UTF_8)));
        System.out.println(enc);
        var dec=BufferCodec.Chars.BASE94.decode(enc);
        assertEquals(src, dec.toString());
    }
    @Test
    void base32(){
        var src="d指定中文∞收拾东西哦";
        var enc= BufferCodec.Chars.BASE32.encode(Buffer.buffer(src.getBytes(StandardCharsets.UTF_8)));
        System.out.println(enc);
        var dec=BufferCodec.Chars.BASE32.decode(enc);
        assertEquals(src, dec.toString());
    }
}

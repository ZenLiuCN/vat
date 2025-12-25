package vat.api.utils;

import io.vertx.core.buffer.Buffer;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

///  manul proto buf encoder and decoder
///
/// @author Zen.Liu
/// @since 2025-12-25

public interface Proto {
    Buffer buffer();

    enum WireType {
        VAR_INT(0), I64(1), LEN(2), S_GRP(3), E_GRP(4), I32(5);
        final int val;

        WireType(int val) {this.val = val;}
        public static WireType from(int val) {
            for (WireType t : values()) if (t.val == val) return t;
           throw new IllegalStateException("unknown wireType "+val);
        }
        public static WireType fromItem(Object item) {
            if (item instanceof Integer || item instanceof Long || item instanceof Boolean) return VAR_INT;
            if (item instanceof Float) return I32;
            if (item instanceof Double) return I64;
            return LEN;
        }
    }

    default Proto tag(int field, WireType kind) {
        return varInt(((long) field << 3) | kind.val);
    }

    default Proto varInt(long v) {
        var b = buffer();
        while ((v & ~0x7F) != 0) {
            b.appendByte((byte) ((v & 0x7F) | 0x80));
            v >>>= 7;
        }
        b.appendByte((byte) v);
        return this;
    }

    default Proto int32(int field, int v) {
        return tag(field, WireType.VAR_INT).varInt(v);
    }

    default Proto int64(int field, long v) {
        return tag(field, WireType.VAR_INT).varInt(v);
    }

    default Proto uint32(int field, int v) {
        return tag(field, WireType.VAR_INT).varInt(Integer.toUnsignedLong(v));
    }

    default Proto uint64(int field, long v) {
        return tag(field, WireType.VAR_INT).varInt(v); // long is already 64-bit
    }

    default Proto bool(int field, boolean v) {
        return tag(field, WireType.VAR_INT).varInt(v ? 1 : 0);
    }

    default Proto sInt32(int field, int v) {
        return tag(field, WireType.VAR_INT).varInt(((long) v << 1) ^ (v >> 31));
    }

    default Proto sInt64(int field, long v) {
        return tag(field, WireType.VAR_INT).varInt((v << 1) ^ (v >> 63));
    }

    default Proto fixed32(int field, int v) {
        tag(field, WireType.I32);
        buffer().appendIntLE(v);
        return this;
    }

    default Proto sFixed32(int field, int v) {
        return fixed32(field, v);
    }

    default Proto float32(int field, float v) {
        return fixed32(field, Float.floatToRawIntBits(v));
    }

    default Proto fixed64(int field, long v) {
        tag(field, WireType.I64);
        buffer().appendLongLE(v);
        return this;
    }

    default Proto sFixed64(int field, long v) {
        return fixed64(field, v);
    }

    default Proto float64(int field, double v) {
        return fixed64(field, Double.doubleToRawLongBits(v));
    }

    default Proto bytes(int field, byte[] v) {
        tag(field, WireType.LEN);
        varInt(v.length);
        buffer().appendBytes(v);
        return this;
    }

    default Proto string(int field, String v) {
        byte[] data = v.getBytes(StandardCharsets.UTF_8);
        tag(field, WireType.LEN);
        varInt(data.length);
        buffer().appendBytes(data);
        return this;
    }

    default <T> Proto repeated(int field, Iterable<T> items, BiConsumer<Proto, T> encoder) {
        for (T item : items) {
            tag(field, WireType.fromItem(item));
            encoder.accept(this, item);
        }
        return this;
    }
    default <T> Proto packed(int field, Iterable<T> items, BiConsumer<Proto, T> encoder) {
        return message(field, p -> {
            for (T item : items) {
                encoder.accept(p, item);
            }
        });
    }
    default <T> Proto optional(T value, BiConsumer<Proto, T> encoder) {
        if (value != null) {
            encoder.accept(this, value);
        }
        return this;
    }
    default Proto message(int field, Consumer<Proto> builder) {
        Proto nested = Proto.of();
        builder.accept(nested);
        tag(field, WireType.LEN);
        varInt(nested.buffer().length());
        buffer().appendBuffer(nested.buffer());
        return this;
    }

    record proto(Buffer buffer) implements Proto {}

    static Proto of() {
        return new proto(Buffer.buffer());
    }

    static Proto of(Buffer buffer) {
        return new proto(buffer);
    }
    record Tag(int fieldId, WireType wireType) {}
    interface Reader extends Proto {
        int pos();

        void pos(int newPos);
        default boolean ended() {
            return pos() >= buffer().length();
        }
        default long varInt() {
            long value = 0; int shift = 0;
            while (shift < 64&&!ended()) {
                byte b = buffer().getByte(pos());
                pos(pos() + 1);
                value |= (long) (b & 0x7F) << shift;
                if ((b & 0x80) == 0) return value;
                shift += 7;
            }
            throw new IllegalStateException("Malformed VarInt");
        }

        default Tag tag() {
            if (ended()) return null;
            long tag = varInt();
            return new Tag((int) (tag >>> 3), WireType.from((int) (tag & 0x07)));
        }

        default int int32() {return (int) varInt();}

        default long int64() {return varInt();}

        default boolean bool() {return varInt() != 0;}

        default int sInt32() {
            int n = (int) varInt();
            return (n >>> 1) ^ -(n & 1);
        }

        default long sInt64() {
            long n = varInt();
            return (n >>> 1) ^ -(n & 1);
        }

        default int fixed32() {
            int v = buffer().getIntLE(pos());
            pos(pos() + 4);
            return v;
        }

        default float float32() {
            return Float.intBitsToFloat(fixed32());
        }

        default long fixed64() {
            long v = buffer().getLongLE(pos());
            pos(pos() + 8);
            return v;
        }

        default double float64() {
            return Double.longBitsToDouble(fixed64());
        }

        default byte[] bytes() {
            int len = (int) varInt();
            byte[] data = buffer().getBytes(pos(), pos() + len);
            pos(pos() + len);
            return data;
        }

        default String string() {
            var len = (int) varInt();
            var s = buffer().getString(pos(), pos() + len,"UTF-8");
            pos(pos() + len);
            return s;
        }

        default void skipField(WireType type) {
            switch (type) {
                case VAR_INT -> varInt();
                case I64 -> pos(pos() + 8);
                case LEN -> pos(pos() + (int) varInt());
                case I32 -> pos(pos() + 4);
                default ->throw new RuntimeException("Unsupported wire type: " + type);
            }
            if (pos() > buffer().length()) throw new IllegalStateException("Skipped past buffer end");
        }

        default void nested(Consumer<Proto> consumer) {
            int len = (int) varInt();
            int limit = pos() + len;
            while (pos() < limit) {
                consumer.accept(this);
            }

        }

        default <T> void packed(Consumer<Proto> itemReader) {
            int len = (int) varInt();
            int limit = pos() + len;
            while (pos() < limit) {
                itemReader.accept(this);
            }
        }
        interface Handler{
           void accept(int field,WireType type,Reader reader);
        }
        default void parse(Handler hd){
            while (!ended()) {
                Tag tag = tag();
                if (tag == null) break;
                hd.accept(tag.fieldId(), tag.wireType(),this);
            }
        }
        default <T> T map(java.util.function.Supplier<T> factory, BiConsumer<T, Reader> fieldMapper) {
            T instance = factory.get();
            while (!ended()) {
                fieldMapper.accept(instance, this);
            }
            return instance;
        }
        default <T> Optional<T> find(int targetFieldId, Function<Reader, T> extractor) {
            while (!ended()) {
                Tag tag = tag();
                if (tag == null) break;
                if (tag.fieldId() == targetFieldId) {
                    return Optional.ofNullable(extractor.apply(this));
                }
                skipField(tag.wireType());
            }
            return Optional.empty();
        }
        record Field(int id, WireType type, Reader reader) {
            public int asInt() { return reader.int32(); }
            public String asString() { return reader.string(); }
        }
        default java.util.stream.Stream<Field> stream() {
            Iterable<Field> iterable = () -> new java.util.Iterator<>() {
                Tag nextTag = tag();
                @Override public boolean hasNext() { return nextTag != null; }
                @Override public Field next() {
                    Field f = new Field(nextTag.fieldId(), nextTag.wireType(), Reader.this);
                    nextTag = tag();
                    return f;
                }
            };
            return java.util.stream.StreamSupport.stream(iterable.spliterator(), false);
        }
        default void fields(BiConsumer<Tag, Reader> action) {
            while (!ended()) {
                Tag tag = tag();
                int startPos = pos();
                action.accept(tag, this);
                if (pos() == startPos) {
                    skipField(tag.wireType());
                }
            }
        }
        @EqualsAndHashCode(onlyExplicitlyIncluded = true)
        @Getter
        @Accessors(fluent = true)
        class reader implements Reader {
            @EqualsAndHashCode.Include
            protected final Buffer buffer;
            protected int pos;

            public reader(Buffer buffer) {this.buffer = buffer;}

            public reader(Buffer buffer, int pos) {this.buffer = buffer; this.pos = pos;}

            @Override
            public void pos(int newPos) {
                this.pos = newPos;
            }
        }

        static Reader of(Buffer buffer) {
            return new reader(buffer);
        }

        static Reader of(Buffer buffer, int pos) {
            return new reader(buffer, pos);
        }
    }
}

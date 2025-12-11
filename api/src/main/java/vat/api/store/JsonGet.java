package vat.api.store;

import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.data.Numeric;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Zen.Liu
 * @since 2025-10-21
 */
@Getter
@Accessors(fluent = true)
sealed public abstract class JsonGet<T, S extends JsonGet<T, S>> implements Expr<T> permits
        JsonGet.JsonBoolean,
        JsonGet.JsonInteger,
        JsonGet.JsonLong,
        JsonGet.JsonNumeric,
        JsonGet.JsonString        ,
        Value.JsonArrayValue.JsonGetJsonArray,
        Value.JsonObjectValue.JsonGetJsonObject {

    public record Path(Type target, Object path) {
    }

    @Override
    public void _render(Renderer renderer, Writer w) {
        w.expr(this);
        renderer.jsonPath(w,root,path);
    }
    final  ValueReader<T> reader;
    final JsonValue<?> root;
    final List<Path> path;

    protected JsonGet(ValueReader<T> reader, JsonValue<?> root) {
        this.reader = reader;
        this.root = root;
        this.path = new ArrayList<>();

    }

    protected JsonGet(ValueReader<T> reader, JsonGet<?, ?> root) {
        this.reader = reader;
        this.root = root.root;
        this.path = new ArrayList<>(root.path);
    }

    protected abstract S self();

    public S at(Type type, Object path) {
        this.path.add(new Path(type, path));
        return self();
    }

    public enum Type {
        OBJECT, ARRAY, STRING, BOOLEAN, NUMERIC, INTEGER, LONG
    }

    public static final class JsonBoolean extends JsonGet<Boolean, JsonBoolean> implements BooleanValue {

        public JsonBoolean(JsonGet<?, ?> root) {
            super(Row::getBoolean, root);
        }

        public JsonBoolean(JsonValue<?> root) {
            super(Row::getBoolean, root);
        }

        @Override
        protected JsonBoolean self() {
            return this;
        }
    }

    public static final class JsonString extends JsonGet<String, JsonString> implements StringValue {

        public JsonString(JsonGet<?, ?> root) {
            super(Row::getString, root);
        }

        public JsonString(JsonValue<?> root) {
            super(Row::getString, root);
        }

        @Override
        protected JsonString self() {
            return this;
        }
    }

    public static final class JsonNumeric extends JsonGet<Numeric, JsonNumeric> implements NumericValue {

        public JsonNumeric(JsonGet<?, ?> root) {
            super(Row::getNumeric, root);
        }

        public JsonNumeric(JsonValue<?> root) {
            super(Row::getNumeric, root);
        }

        @Override
        protected JsonNumeric self() {
            return this;
        }
    }

    public static final class JsonInteger extends JsonGet<Integer, JsonInteger> implements IntegerValue {

        public JsonInteger(JsonGet<?, ?> root) {
            super(Row::getInteger, root);
        }

        public JsonInteger(JsonValue<?> root) {
            super(Row::getInteger, root);
        }

        @Override
        protected JsonInteger self() {
            return this;
        }
    }

    public static final class JsonLong extends JsonGet<Long, JsonLong> implements LongValue {

        public JsonLong(JsonGet<?, ?> root) {
            super(Row::getLong, root);
        }

        public JsonLong(JsonValue<?> root) {
            super(Row::getLong, root);
        }

        @Override
        protected JsonLong self() {
            return this;
        }
    }
}

package vat.codegen.utils;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.data.Numeric;
import lombok.With;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.lambda.function.Consumer3;
import org.jooq.lambda.function.Function3;
import vat.api.Data;
import vat.api.DomainError;
import vat.api.Entity;
import vat.api.implement.Codec;
import vat.api.implement.CommonCodec;
import vat.api.meta.Enhance;
import vat.api.store.Field;
import vat.api.utils.Buf;
import vat.api.utils.Fn;
import vat.api.utils.ITimes;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

///
/// @author Zen.Liu
/// @since 2025-10-28


@SuppressWarnings({"SwitchStatementWithTooFewBranches", "DuplicatedCode"})
public interface CodecBuilder {
    interface JsonCodecBuilder {
        //region json codec
        interface Writer {
            void write(Context ctx, CodeBlock.Builder cb, CharSequence receiver, CharSequence property, CharSequence value,
                       boolean index, boolean variable);
        }

        /**
         * writer add an expression `JsonObject|JsonArray=>void` to write spec value into json source.
         *
         * @param ctx      the context
         * @param cb       code block builder
         * @param receiver receiver name
         * @param property property (key or index)
         * @param value    value name or value
         * @param index    array model, property is an index
         * @param variable property (key or index) is variable
         */
        void write(Context ctx, CodeBlock.Builder cb, CharSequence receiver, CharSequence property, CharSequence value,
                   boolean index, boolean variable);

        interface Reader {
            void read(Context ctx, CodeBlock.Builder cb, CharSequence receiver, CharSequence property, boolean index,
                      boolean variable);
        }

        /**
         * read add an expression `JsonObject|JsonArray=>T` to read spec value for a json source
         *
         * @param ctx      the context
         * @param cb       code block builder
         * @param receiver receiver which hold the source data (object or array)
         * @param property property (key or index)
         * @param index    array model, property is an index
         * @param variable property (key or index) is variable
         */
        void read(Context ctx, CodeBlock.Builder cb, CharSequence receiver, CharSequence property, boolean index,
                  boolean variable);

        default void read(Context ctx, CodeBlock.Builder cb, CharSequence receiver, CharSequence property, boolean index) {
            read(ctx, cb, receiver, property, index, false);
        }

        default void write(Context ctx, CodeBlock.Builder cb, CharSequence receiver, CharSequence property,
                           CharSequence value, boolean index) {
            write(ctx, cb, receiver, property, value, index, false);
        }

        enum BuilderType {
            PRIMITIVE,
            ANY,
            DATA,
            LONG_STRING,
            STRING_MAP,
            OBJECT,
            MAP,
            ARRAY,
            LIST,
            TUPLE,
            ENUM,
        }

        BuilderType type();

        boolean nullable();

        record Builder(
                BuilderType type,
                boolean nullable,
                @With Writer w,
                @With Reader r
        ) implements JsonCodecBuilder {
            @Override
            public void write(Context ctx, CodeBlock.Builder cb, CharSequence receiver, CharSequence property,
                              CharSequence value, boolean index, boolean variable) {
                Objects.requireNonNull(w, "writer is null").write(ctx, cb, receiver, property, value, index, variable);
            }

            @Override
            public void read(Context ctx, CodeBlock.Builder cb, CharSequence receiver, CharSequence property, boolean index,
                             boolean variable) {
                Objects.requireNonNull(r, "reader is null").read(ctx, cb, receiver, property, index, variable);
            }

        }

        //region PRIMITIVE
        Builder BOOLEAN = new Builder(BuilderType.PRIMITIVE, false,
                (u, c, r, p, v, i, vn) ->
                {
                    if (i)
                        c.add("$L.set($L,$L)", r, p, v);
                    else if (vn)
                        c.add("$L.put($L,$L)", r, p, v);
                    else
                        c.add("$L.put($S,$L)", r, p, v);
                },
                (u, c, r, p, i, vn) ->
                {
                    if (i || vn)
                        c.add("$T.prim($L.getBoolean($L),$S)", Codec.class, r, p);
                    else
                        c.add("$T.prim($L.getBoolean($S),$S)", Codec.class, r, p);
                }
        );
        Builder BYTE = BOOLEAN.withR((u, c, r, p, i, vn) ->
        {
            if (i || vn)
                c.add("$T.prim($L.getNumber($L),$S).byteValue()", Codec.class, r, p);
            else
                c.add("$T.prim($L.getNumber($S),$S).byteValue()", Codec.class, r, p);
        });
        Builder SHORT = BOOLEAN.withR((u, c, r, p, i, vn) ->
        {
            if (i || vn)
                c.add("$T.prim($L.getNumber($L),$S).shortValue()", Codec.class, r, p);
            else
                c.add("$T.prim($L.getNumber($S),$S).shortValue()", Codec.class, r, p);
        });
        Builder INT = BOOLEAN.withR((u, c, r, p, i, vn) ->
        {
            if (i || vn)
                c.add("$T.prim($L.getNumber($L),$S).intValue()", Codec.class, r, p);
            else
                c.add("$T.prim($L.getNumber($S),$S).intValue()", Codec.class, r, p);
        });
        Builder CHAR = BOOLEAN.withR((u, c, r, p, i, vn) ->
        {
            if (i || vn)
                c.add("(char)$T.prim($L.getNumber($L),$S).intValue()", Codec.class, r, p);
            else
                c.add("(char)$T.prim($L.getNumber($S),$S).intValue()", Codec.class, r, p);
        });
        Builder LONG = BOOLEAN.withR((u, c, r, p, i, vn) ->
        {
            if (i || vn)
                c.add("$T.prim($L.getNumber($L),$S).longValue()", Codec.class, r, p);
            else
                c.add("$T.prim($L.getNumber($S),$S).longValue()", Codec.class, r, p);
        });
        Builder FLOAT = BOOLEAN.withR((u, c, r, p, i, vn) ->
        {
            if (i || vn)
                c.add("$T.prim($L.getNumber($L),$S).floatValue()", Codec.class, r, p);
            else
                c.add("$T.prim($L.getNumber($S),$S).floatValue()", Codec.class, r, p);
        });
        Builder DOUBLE = BOOLEAN.withR((u, c, r, p, i, vn) ->
        {
            if (i || vn)
                c.add("$T.prim($L.getNumber($L),$S).doubleValue()", Codec.class, r, p);
            else
                c.add("$T.prim($L.getNumber($S),$S).doubleValue()", Codec.class, r, p);
        });
        //endregion
        //region SPECIAL
        JsonCodecBuilder ANY = new Builder(BuilderType.ANY, true,
                (u, c, r, n, v, i, vn) ->
                {
                    if (i)
                        c.add("$L.set($L,$T.jsonAny($L))", r, n, Codec.class, v);
                    else if (vn)
                        c.add("$L.put($L,$T.jsonAny($L))", r, n, Codec.class, v);
                    else
                        c.add("$L.put($S,$T.jsonAny($L))", r, n, Codec.class, v);
                },
                (u, c, n, v, i, vn) ->
                {
                    if (i || vn)
                        c.add("$L.getValue($L)", v, n);
                    else
                        c.add("$L.getValue($S)", v, n);
                });
        /// ANY extends DATA
        Function<TypeName, JsonCodecBuilder> DATA = (type) -> new Builder(BuilderType.DATA, true,
                //interface store instance name as $type
                (u, c, r, n, v, i, vn) ->
                {
                    if (i)
                        c.add("$L.set($L,$L==null?null:$L.asJson().put($S,$L.getClass().getName()))",
                                r, n, v, v, "$type", v);
                    else if (vn)
                        c.add("$L.put($L,$L==null?null:$L.asJson().put($S,$L.getClass().getName()))",
                                r, n, v, v, "$type", v);
                    else
                        c.add("$L.put($S,$L==null?null:$L.asJson().put($S,$L.getClass().getName()))",
                                r, n, v, v, "$type", v);
                }
                ,
                type == null
                        ? (u, c, n, v, i, vn) ->
                {
                    if (i || vn)
                        c.add("$T.data($L.getJsonObject($L),null)",
                                Codec.class, n, v);
                    else
                        c.add("$T.data($L.getJsonObject($S),null)",
                                Codec.class, n, v);
                }
                        : (u, c, n, v, i, vn) ->
                {
                    if (i || vn)
                        c.add("$T.data($L.getJsonObject($L),$T.class)",
                                Codec.class, n, v, type);
                    else
                        c.add("$T.data($L.getJsonObject($S),$T.class)",
                                Codec.class, n, v, type);
                });
        BiFunction<TypeName, String, JsonCodecBuilder> DATA_ENHANCED = (codec, field) -> new Builder(BuilderType.DATA, true,
                //interface store instance name as $type
                (u, c, r, n, v, i, vn) ->
                {
                    if (i)
                        c.add("$L.set($L,$L==null?null:$L.asJson())",
                                r, n, v, v);
                    else if (vn)
                        c.add("$L.put($L,$L==null?null:$L.asJson())",
                                r, n, v, v);
                    else
                        c.add("$L.put($S,$L==null?null:$L.asJson())",
                                r, n, v, v);
                }
                ,
                (u, c, n, v, i, vn) ->
                {
                    if (i || vn)
                        c.add("$T.$L.get($L.getJsonObject($L))",
                                codec, field, n, v);
                    else
                        c.add("$T.$L($L.getJsonObject($S))",
                                codec, field, n, v);
                });
        /// LONG as STRING
        JsonCodecBuilder LONG_STRING = new Builder(BuilderType.LONG_STRING, false,
                (u, c, r, n, v, i, vn) ->
                {
                    if (i)
                        c.add("$L.set($L,$T.toString($L))", r, n, v, v, Long.class, v);
                    else if (vn)
                        c.add("$L.put($L,$T.toString($L))", r, n, v, v, Long.class, v);
                    else
                        c.add("$L.put($S,$T.toString($L))", r, n, v, v, Long.class, v);
                },
                (u, c, r, p, i, vn) ->
                {
                    if (i || vn)
                        c.add("$T.parseLong($T.prim($L.getString($L),$S))", Long.class,
                                Codec.class, r, p, p);
                    else
                        c.add("$T.parseLong($T.prim($L.getString($S),$S))", Long.class,
                                Codec.class, r, p, p);
                });
        //endregion
        //region ARRAY
        JsonCodecBuilder BYTE_ARRAY = new Builder(BuilderType.ARRAY, true,
                (u, c, r, p, v, i, vn) -> {
                    if (i)
                        c.add("$L.set($L,$L)", r, p, v);
                    else if (vn)
                        c.add("$L.put($L,$L)", r, p, v);
                    else
                        c.add("$L.put($S,$L)", r, p, v);
                },
                (u, c, r, p, i, vn) -> {
                    if (i || vn)
                        c.add("$L.getBinary($L)", r, p);
                    else
                        c.add("$L.getBinary($S)", r, p);
                });
        JsonCodecBuilder CHAR_ARRAY = new Builder(BuilderType.ARRAY, true,
                (u, c, r, p, v, i, vn) -> {
                    if (i)
                        c.add("$L.set($L,$T.charArray($L))", r, p, Codec.class, v);
                    else if (vn)
                        c.add("$L.put($L,$T.charArray($L))", r, p, Codec.class, v);
                    else
                        c.add("$L.put($S,$T.charArray($L))", r, p, Codec.class, v);
                },
                (u, c, r, p, i, vn) -> {
                    if (i || vn)
                        c.add("$T.charArray($L.getString($L))", Codec.class, r, p);
                    else
                        c.add("$T.charArray($L.getString($S))", Codec.class, r, p);
                });
        JsonCodecBuilder BOOLEAN_ARRAY = new Builder(BuilderType.ARRAY, true,
                (u, c, r, p, v, i, vn) -> {
                    if (i)
                        c.add("$L.set($L,$T.booleanArray($L))", r, p, Codec.class, v);
                    else if (vn)
                        c.add("$L.put($L,$T.booleanArray($L))", r, p, Codec.class, v);
                    else
                        c.add("$L.put($S,$T.booleanArray($L))", r, p, Codec.class, v);
                },
                (u, c, r, p, i, vn) -> {
                    if (i || vn)
                        c.add("$T.booleanArray($L.getBinary($L))", Codec.class, r, p);
                    else
                        c.add("$T.booleanArray($L.getBinary($S))", Codec.class, r, p);
                });
        JsonCodecBuilder SHORT_ARRAY = new Builder(BuilderType.ARRAY, true,
                (u, c, r, p, v, i, vn) -> {
                    if (i)
                        c.add("$L.set($L,$T.shortArray($L))", r, p, Codec.class, v);
                    else if (vn)
                        c.add("$L.put($L,$T.shortArray($L))", r, p, Codec.class, v);
                    else
                        c.add("$L.put($S,$T.shortArray($L))", r, p, Codec.class, v);
                },
                (u, c, r, p, i, vn) -> {
                    if (i || vn)
                        c.add("$T.shortArray($L.getJsonArray($L))", Codec.class, r, p);
                    else
                        c.add("$T.shortArray($L.getJsonArray($S))", Codec.class, r, p);
                });
        JsonCodecBuilder INT_ARRAY = new Builder(BuilderType.ARRAY, true,
                (u, c, r, p, v, i, vn) -> {
                    if (i)
                        c.add("$L.set($L,$T.intArray($L))", r, p, Codec.class, v);
                    else if (vn)
                        c.add("$L.put($L,$T.intArray($L))", r, p, Codec.class, v);
                    else
                        c.add("$L.put($S,$T.intArray($L))", r, p, Codec.class, v);
                },
                (u, c, r, p, i, vn) -> {
                    if (i || vn)
                        c.add("$T.intArray($L.getJsonArray($L))", Codec.class, r, p);
                    else
                        c.add("$T.intArray($L.getJsonArray($S))", Codec.class, r, p);
                });
        JsonCodecBuilder LONG_ARRAY = new Builder(BuilderType.ARRAY, true,
                (u, c, r, p, v, i, vn) -> {
                    if (i)
                        c.add("$L.set($L,$T.longArray($L))", r, p, Codec.class, v);
                    else if (vn)
                        c.add("$L.put($L,$T.longArray($L))", r, p, Codec.class, v);
                    else
                        c.add("$L.put($S,$T.longArray($L))", r, p, Codec.class, v);
                },
                (u, c, r, p, i, vn) -> {
                    if (i || vn)
                        c.add("$T.longArray($L.getJsonArray($L))", Codec.class, r, p);
                    else
                        c.add("$T.longArray($L.getJsonArray($S))", Codec.class, r, p);
                });
        JsonCodecBuilder FLOAT_ARRAY = new Builder(BuilderType.ARRAY, true,
                (u, c, r, p, v, i, vn) -> {
                    if (i)
                        c.add("$L.set($L,$T.floatArray($L))", r, p, Codec.class, v);
                    else if (vn)
                        c.add("$L.put($L,$T.floatArray($L))", r, p, Codec.class, v);
                    else
                        c.add("$L.put($S,$T.floatArray($L))", r, p, Codec.class, v);
                },
                (u, c, r, p, i, vn) -> {
                    if (i || vn)
                        c.add("$T.floatArray($L.getJsonArray($L))", Codec.class, r, p);
                    else
                        c.add("$T.floatArray($L.getJsonArray($S))", Codec.class, r, p);
                });
        JsonCodecBuilder DOUBLE_ARRAY = new Builder(BuilderType.ARRAY, true,
                (u, c, r, p, v, i, vn) -> {
                    if (i)
                        c.add("$L.set($L,$T.doubleArray($L))", r, p, Codec.class, v);
                    else if (vn)
                        c.add("$L.put($L,$T.doubleArray($L))", r, p, Codec.class, v);
                    else
                        c.add("$L.put($S,$T.doubleArray($L))", r, p, Codec.class, v);
                },
                (u, c, r, p, i, vn) -> {
                    if (i || vn)
                        c.add("$T.doubleArray($L.getJsonArray($L))", Codec.class, r, p);
                    else
                        c.add("$T.doubleArray($L.getJsonArray($S))", Codec.class, r, p);
                });

        BiFunction<JsonCodecBuilder, TypeMirror, JsonCodecBuilder> GENERIC_ARRAY = (e, m) -> new Builder(BuilderType.ARRAY, true,
                (u, c, r, p, v, i, vn) -> {
                    var cb = CodeBlock.builder();
                    e.write(u, cb, "r_" + p,
                            "p_" + p, "v_" + p, true,
                            true);
                    if (i)
                        c.add("$L.set($L,$T.generic($L,(p_$L,r_$L,v_$L)->$L))",
                                r, p, Codec.class, v,
                                p, p, p, cb.build());
                    else if (vn)
                        c.add("$L.put($L,$T.generic($L,(p_$L,r_$L,v_$L)->$L))",
                                r, p, Codec.class, v,
                                p, p, p, cb.build());
                    else
                        c.add("$L.put($S,$T.generic($L,(p_$L,r_$L,v_$L)->$L))",
                                r, p, Codec.class, v,
                                p, p, p, cb.build());
                },
                (u, c, r, p, i, vn) -> {
                    var cb = CodeBlock.builder();
                    e.read(u, cb, "r_" + p, "p_" + p,
                            true, true);
                    if (i || vn)
                        c.add("$T.generic($L.getJsonArray($L),$T.class,(p_$L,r_$L)->$L)",
                                Codec.class, r, p,
                                TypeName.get(m), p, p,
                                cb.build());
                    else
                        c.add("$T.generic($L.getJsonArray($S),$T.class,(p_$L,r_$L)->$L)",
                                Codec.class, r, p,
                                TypeName.get(m), p, p,
                                cb.build());
                });
        //endregion
        //region CONTAINER
        BiFunction<JsonCodecBuilder, ClassName, JsonCodecBuilder> GENERIC_COLLECTION = (e, m) -> new Builder(BuilderType.LIST, true,
                (u, c, r, p, v, i, vn) -> {
                    var cb = CodeBlock.builder();
                    e.write(u, cb, "r_" + p,
                            "p_" + p, "v_" + p,
                            true);
                    if (i)
                        c.add("$L.set($L,$T.list($L,(p_$L,r_$L,v_$L)->$L))",
                                r, p, Codec.class,
                                v, p, p, p,
                                cb.build());
                    else if (vn)
                        c.add("$L.put($L,$T.list($L,(p_$L,r_$L,v_$L)->$L))",
                                r, p, Codec.class,
                                v, p, p, p,
                                cb.build());
                    else
                        c.add("$L.put($S,$T.list($L,(p_$L,r_$L,v_$L)->$L))",
                                r, p, Codec.class,
                                v, p, p, p,
                                cb.build());
                },
                (u, c, r, p, i, vn) -> {
                    var cb = CodeBlock.builder();
                    e.read(u, cb, "r_" + p,
                            "p_" + p, true);
                    if (i || vn)
                        c.add("$T.list($L.getJsonArray($L),$T::new,(p_$L,r_$L)->$L)",
                                Codec.class, r, p,
                                m, p, p,
                                cb.build());
                    else
                        c.add("$T.list($L.getJsonArray($S),$T::new,(p_$L,r_$L)->$L)",
                                Codec.class, r, p,
                                m, p, p,
                                cb.build());
                });
        Function3<JsonCodecBuilder, JsonCodecBuilder, ClassName, JsonCodecBuilder> GENERIC_MAP = (ka, va, m) -> new Builder(BuilderType.LIST, true,
                (u, c, r, p, v, i, vn) -> {
                    var kb = CodeBlock.builder();
                    ka.write(u, kb,
                            "r_" + p,
                            "p_" + p,
                            "v_" + p,
                            true);
                    var vb = CodeBlock.builder();
                    va.write(u, vb,
                            "r_" + p,
                            "p_" + p,
                            "v_" + p,
                            true);
                    if (i)
                        c.add("$L.set($L,$T.map($L,(p_$L,r_$L,v_$L)->$L),(p_$L,r_$L,v_$L)->$L))",
                                r, p,
                                Codec.class,
                                v, p, p, p,
                                kb.build(),
                                p, p, p,
                                vb.build());
                    else if (vn)
                        c.add("$L.set($L,$T.map($L,(p_$L,r_$L,v_$L)->$L),(p_$L,r_$L,v_$L)->$L))",
                                r, p,
                                Codec.class,
                                v, p, p, p,
                                kb.build(),
                                p, p, p,
                                vb.build());
                    else
                        c.add("$L.set($S,$T.map($L,(p_$L,r_$L,v_$L)->$L),(p_$L,r_$L,v_$L)->$L))",
                                r, p,
                                Codec.class,
                                v, p, p, p,
                                kb.build(),
                                p, p, p,
                                vb.build());
                },
                (u, c, r, p, i, vn) -> {
                    var kb = CodeBlock.builder();
                    ka.read(u, kb,
                            "r_" + p,
                            "p_" + p,
                            true);
                    var vb = CodeBlock.builder();
                    va.read(u, vb,
                            "r_" + p,
                            "p_" + p,
                            true);
                    if (i || vn)
                        c.add("$T.map($L.getJsonArray($L),$T::new,(p_$L,r_$L)->$L,(p_$L,r_$L)->$L)",
                                Codec.class,
                                r, p, m, p,
                                p,
                                kb.build(),
                                p, p,
                                vb.build());
                    else
                        c.add("$T.map($L.getJsonArray($S),$T::new,(p_$L,r_$L)->$L,(p_$L,r_$L)->$L)",
                                Codec.class,
                                r, p, m, p,
                                p,
                                kb.build(),
                                p, p,
                                vb.build());
                });
        //endregion
        //region OBJECT
        Builder BOOLEAN_OBJECT = new Builder(BuilderType.OBJECT, true,
                (u, c, r, p, v, i, vn) ->
                {
                    if (i)
                        c.add("$L.set($L,$L)", r, p, v);
                    else if (vn)
                        c.add("$L.put($L,$L)", r, p, v);
                    else
                        c.add("$L.put($S,$L)", r, p, v);
                },
                (u, c, r, p, i, vn) ->
                {
                    if (i || vn)
                        c.add("$L.getBoolean($L)", r, p);
                    else
                        c.add("$L.getBoolean($S)", r, p);
                });
        Builder BYTE_OBJECT = BOOLEAN_OBJECT.withR((u, c, r, p, i, vn) ->
        {
            if (i || vn)
                c.add("$T.object($L.getNumber($L),$T::byteValue)", Codec.class,
                        r, p, Number.class);
            else
                c.add("$T.object($L.getNumber($S),$T::byteValue)", Codec.class,
                        r, p, Number.class);
        });
        Builder SHORT_OBJECT = BOOLEAN_OBJECT.withR((u, c, r, p, i, vn) ->
        {
            if (i || vn)
                c.add("$T.object($L.getNumber($L),$T::shortValue)", Codec.class,
                        r, p, Number.class);
            else
                c.add("$T.object($L.getNumber($S),$T::shortValue)", Codec.class,
                        r, p, Number.class);
        });
        Builder INT_OBJECT = BOOLEAN_OBJECT.withR((u, c, r, p, i, vn) ->
        {
            if (i || vn)
                c.add("$T.object($L.getNumber($L),$T::intValue)", Codec.class, r,
                        p, Number.class);
            else
                c.add("$T.object($L.getNumber($S),$T::intValue)", Codec.class, r,
                        p, Number.class);
        });
        Builder CHAR_OBJECT = BOOLEAN_OBJECT.withR((u, c, r, p, i, vn) ->
        {
            if (i || vn)
                c.add("$T.object($L.getNumber($L),x->(char)x.intValue())",
                        Codec.class, r, p);
            else
                c.add("$T.object($L.getNumber($S),x->(char)x.intValue())",
                        Codec.class, r, p);
        });
        Builder LONG_OBJECT = BOOLEAN_OBJECT.withR((u, c, r, p, i, vn) ->
        {
            if (i || vn)
                c.add("$T.object($L.getNumber($L),$T::longValue)", Codec.class,
                        r, p, Number.class);
            else
                c.add("$T.object($L.getNumber($S),$T::longValue)", Codec.class,
                        r, p, Number.class);
        });
        Builder FLOAT_OBJECT = BOOLEAN_OBJECT.withR((u, c, r, p, i, vn) ->
        {
            if (i || vn)
                c.add("$T.object($L.getNumber($L),$T::floatValue)", Codec.class,
                        r, p, Number.class);
            else
                c.add("$T.object($L.getNumber($S),$T::floatValue)", Codec.class,
                        r, p, Number.class);
        });
        Builder DOUBLE_OBJECT = BOOLEAN_OBJECT.withR((u, c, r, p, i, vn) ->
        {
            if (i || vn)
                c.add("$T.object($L.getNumber($L),$T::doubleValue)",
                        Codec.class, r, p, Number.class);
            else
                c.add("$T.object($L.getNumber($S),$T::doubleValue)",
                        Codec.class, r, p, Number.class);
        });
        Builder BUFFER = BOOLEAN_OBJECT.withR((u, c, r, p, i, vn) ->
        {
            if (i || vn)
                c.add("$L.getBuffer($L)", r, p);
            else
                c.add("$L.getBuffer($S)", r, p);
        });
        Builder NUMBER = BOOLEAN_OBJECT.withR((u, c, r, p, i, vn) ->
        {
            if (i || vn)
                c.add("$L.getNumber($L)", r, p);
            else
                c.add("$L.getNumber($S)", r, p);
        });

        Builder BIG_DECIMAL = BOOLEAN_OBJECT.withR((u, c, r, p, i, vn) ->
        {
            if (i || vn)
                c.add("$T.ofNullable($L.getString($L)).map($T::new).orElse(null)",
                        Optional.class, r, p, BigDecimal.class);
            else
                c.add("$T.ofNullable($L.getString($S)).map($T::new).orElse(null)",
                        Optional.class, r, p, BigDecimal.class);
        });
        Builder INSTANT = BOOLEAN_OBJECT.withR((u, c, r, p, i, vn) ->
        {
            if (i || vn)
                c.add("$L.getInstant($L)", r, p);
            else
                c.add("$L.getInstant($S)", r, p);
        });
        Builder STRING = BOOLEAN_OBJECT.withR((u, c, r, p, i, vn) ->
        {
            if (i || vn)
                c.add("$L.getString($L)", r, p);
            else
                c.add("$L.getString($S)", r, p);
        });
        Builder JSON_ARRAY = BOOLEAN_OBJECT.withR((u, c, r, p, i, vn) ->
        {
            if (i || vn)
                c.add("$L.getJsonArray($L)", r, p);
            else
                c.add("$L.getJsonArray($S)", r, p);
        });
        Builder JSON_OBJECT = BOOLEAN_OBJECT.withR((u, c, r, p, i, vn) ->
        {
            if (i || vn)
                c.add("$L.getJsonObject($L)", r, p);
            else
                c.add("$L.getJsonObject($S)", r, p);
        });
        Builder NUMERIC = new Builder(BuilderType.OBJECT, true,
                (u, c, r, p, v, i, vn) ->
                {
                    if (i)
                        c.add("$L.set($L,$L==null?null:$L.toString())", r, p, v, v);
                    else if (vn)
                        c.add("$L.put($L,$L==null?null:$L.toString())", r, p, v, v);
                    else
                        c.add("$L.put($S,$L==null?null:$L.toString())", r, p, v, v);
                },
                (u, c, r, p, i, vn) ->
                {
                    if (i || vn)
                        c.add("$T.ofNullable($L.getString($L)).map($T::parse).orElse(null)", Optional.class, r, p, Numeric.class);
                    else
                        c.add("$T.ofNullable($L.getNumber($S)).map($T::parse).orElse(null)", Optional.class, r, p, Numeric.class);
                });
        Builder TIME = new Builder(BuilderType.OBJECT, true,
                (u, c, r, p, v, i, vn) ->
                {
                    if (i)
                        c.add("$L.set($L,$T.time($L))", r, p, Codec.class, v);
                    else if (vn)
                        c.add("$L.put($L,$T.time($L))", r, p, Codec.class, v);
                    else
                        c.add("$L.put($S,$T.time($L))", r, p, Codec.class, v);
                },
                (u, c, r, p, i, vn) ->
                {
                    if (i || vn)
                        c.add("$T.time($L.getString($L))", Codec.class, r, p);
                    else
                        c.add("$T.time($L.getString($S))", Codec.class, r, p);
                });
        Builder DATE = new Builder(BuilderType.OBJECT, true,
                (u, c, r, p, v, i, vn) ->
                {
                    if (i)
                        c.add("$L.set($L,$T.date($L))", r, p, Codec.class, v);
                    else if (vn)
                        c.add("$L.put($L,$T.date($L))", r, p, Codec.class, v);
                    else
                        c.add("$L.put($S,$T.date($L))", r, p, Codec.class, v);
                },
                (u, c, r, p, i, vn) ->
                {
                    if (i || vn)
                        c.add("$T.date($L.getString($L))", Codec.class, r, p);
                    else
                        c.add("$T.date($L.getString($S))", Codec.class, r, p);
                });
        Builder DATETIME = new Builder(BuilderType.OBJECT, true,
                (u, c, r, p, v, i, vn) ->
                {
                    if (i)
                        c.add("$L.set($L,$T.datetime($L))", r, p, Codec.class, v);
                    else if (vn)
                        c.add("$L.put($L,$T.datetime($L))", r, p, Codec.class, v);
                    else
                        c.add("$L.put($S,$T.datetime($L))", r, p, Codec.class, v);
                },
                (u, c, r, p, i, vn) ->
                {
                    if (i || vn)
                        c.add("$T.datetime($L.getString($L))", Codec.class, r, p);
                    else
                        c.add("$T.datetime($L.getString($S))", Codec.class, r, p);
                });
        Builder DATETIME_TZ = new Builder(BuilderType.OBJECT, true,
                (u, c, r, p, v, i, vn) ->
                {
                    if (i)
                        c.add("$L.set($L,$T.datetimeTZ($L))", r, p, Codec.class, v);
                    else if (vn)
                        c.add("$L.put($L,$T.datetimeTZ($L))", r, p, Codec.class, v);
                    else
                        c.add("$L.put($S,$T.datetimeTZ($L))", r, p, Codec.class, v);
                },
                (u, c, r, p, i, vn) ->
                {
                    if (i || vn)
                        c.add("$T.datetimeTZ($L.getString($L))", Codec.class, r, p);
                    else
                        c.add("$T.datetimeTZ($L.getString($S))", Codec.class, r, p);
                });
        Builder TIME_TZ = new Builder(BuilderType.OBJECT, true,
                (u, c, r, p, v, i, vn) ->
                {
                    if (i)
                        c.add("$L.set($L,$T.timeTZ($L))", r, p, Codec.class, v);
                    else if (vn)
                        c.add("$L.put($L,$T.timeTZ($L))", r, p, Codec.class, v);
                    else
                        c.add("$L.put($S,$T.timeTZ($L))", r, p, Codec.class, v);
                },
                (u, c, r, p, i, vn) ->
                {
                    if (i || vn)
                        c.add("$T.timeTZ($L.getString($L))", Codec.class, r, p);
                    else
                        c.add("$T.timeTZ($L.getString($S))", Codec.class, r, p);
                });
        Builder CLASS = new Builder(BuilderType.OBJECT, true,
                (u, c, r, p, v, i, vn) ->
                {
                    if (i)
                        c.add("$L.set($L,$T.clazz($L))", r, p, Codec.class, v);
                    else if (vn)
                        c.add("$L.put($L,$T.clazz($L))", r, p, Codec.class, v);
                    else
                        c.add("$L.put($S,$T.clazz($L))", r, p, Codec.class, v);
                },
                (u, c, r, p, i, vn) ->
                {
                    if (i || vn)
                        c.add("$T.clazz($L.getString($L))", Codec.class, r, p);
                    else
                        c.add("$T.clazz($L.getString($S))", Codec.class, r, p);
                });
        Builder DURATION = new Builder(BuilderType.OBJECT, true,
                (u, c, r, p, v, i, vn) ->
                {
                    if (i)
                        c.add("$L.set($L,$T.duration($L))", r, p, Codec.class, v);
                    else if (vn)
                        c.add("$L.put($L,$T.duration($L))", r, p, Codec.class, v);
                    else
                        c.add("$L.put($S,$T.duration($L))", r, p, Codec.class, v);
                },
                (u, c, r, p, i, vn) ->
                {
                    if (i || vn)
                        c.add("$T.duration($L.getString($L))", Codec.class, r, p);
                    else
                        c.add("$T.duration($L.getString($S))", Codec.class, r, p);
                });
        Builder PERIOD = new Builder(BuilderType.OBJECT, true,
                (u, c, r, p, v, i, vn) ->
                {
                    if (i)
                        c.add("$L.set($L,$T.period($L))", r, p, Codec.class, v);
                    else if (vn)
                        c.add("$L.put($L,$T.period($L))", r, p, Codec.class, v);
                    else
                        c.add("$L.put($S,$T.period($L))", r, p, Codec.class, v);
                },
                (u, c, r, p, i, vn) ->
                {
                    if (i || vn)
                        c.add("$T.period($L.getString($L))", Codec.class, r, p);
                    else
                        c.add("$T.period($L.getString($S))", Codec.class, r, p);
                });
        Builder UUID = new Builder(BuilderType.OBJECT, true,
                (u, c, r, p, v, i, vn) ->
                {
                    if (i)
                        c.add("$L.set($L,$T.uuid($L))", r, p, Codec.class, v);
                    else if (vn)
                        c.add("$L.put($L,$T.uuid($L))", r, p, Codec.class, v);
                    else
                        c.add("$L.put($S,$T.uuid($L))", r, p, Codec.class, v);
                },
                (u, c, r, p, i, vn) ->
                {
                    if (i || vn)
                        c.add("$T.uuid($L.getString($L))", Codec.class, r, p);
                    else
                        c.add("$T.uuid($L.getString($S))", Codec.class, r, p);
                });

        Builder I_TIME = new Builder(BuilderType.OBJECT, true,
                (u, c, r, p, v, i, vn) ->
                {
                    if (i)
                        c.add("$L.set($L,$L==null?null:$L.value())", r, p, v, v);
                    else if (vn)
                        c.add("$L.put($L,$L==null?null:$L.value())", r, p, v, v);
                    else
                        c.add("$L.put($S,$L==null?null:$L.value())", r, p, v, v);
                },
                (u, c, r, p, i, vn) ->
                {
                    if (i || vn)
                        c.add("$T.ofNullable($L.getInteger($L)).map($T::new).orElse(null)", Optional.class, r, p, ITimes.ITime.class);
                    else
                        c.add("$T.ofNullable($L.getInteger($S)).map($T::new).orElse(null)", Optional.class, r, p, ITimes.ITime.class);
                });
        Builder I_DATE = new Builder(BuilderType.OBJECT, true,
                (u, c, r, p, v, i, vn) ->
                {
                    if (i)
                        c.add("$L.set($L,$L==null?null:$L.value())", r, p, v, v);
                    else if (vn)
                        c.add("$L.put($L,$L==null?null:$L.value())", r, p, v, v);
                    else
                        c.add("$L.put($S,$L==null?null:$L.value())", r, p, v, v);
                },
                (u, c, r, p, i, vn) ->
                {
                    if (i || vn)
                        c.add("$T.ofNullable($L.getInteger($L)).map($T::new).orElse(null)", Optional.class, r, p, ITimes.IDate.class);
                    else
                        c.add("$T.ofNullable($L.getInteger($S)).map($T::new).orElse(null)", Optional.class, r, p, ITimes.IDate.class);
                });
        Builder I_DATETIME = new Builder(BuilderType.OBJECT, true,
                (u, c, r, p, v, i, vn) ->
                {
                    if (i)
                        c.add("$L.set($L,$L==null?null:$L.value())", r, p, v, v);
                    else if (vn)
                        c.add("$L.put($L,$L==null?null:$L.value())", r, p, v, v);
                    else
                        c.add("$L.put($S,$L==null?null:$L.value())", r, p, v, v);
                },
                (u, c, r, p, i, vn) ->
                {
                    if (i || vn)
                        c.add("$T.ofNullable($L.getLong($L)).map($T::new).orElse(null)", Optional.class, r, p, ITimes.IDate.class);
                    else
                        c.add("$T.ofNullable($L.getLong($S)).map($T::new).orElse(null)", Optional.class, r, p, ITimes.IDate.class);
                });
        Builder ENTRY = new Builder(BuilderType.OBJECT, true,
                (u, c, r, p, v, i, vn) ->
                {
                    if (i)
                        c.add("$L.set($L,$L==null?null:$L.toJson())", r, p, v, v);
                    else if (vn)
                        c.add("$L.put($L,$L==null?null:$L.toJson())", r, p, v, v);
                    else
                        c.add("$L.put($S,$L==null?null:$L.toJson())", r, p, v, v);
                },
                (u, c, r, p, i, vn) ->
                {
                    if (i || vn)
                        c.add("$T.ofNullable($L.getJsonObject($L)).map($T::new).orElse(null)", Optional.class, r, p, Entity.Entry.entry.class);
                    else
                        c.add("$T.ofNullable($L.getJsonObject($S)).map($T::new).orElse(null)", Optional.class, r, p, Entity.Entry.entry.class);
                });
        Function<TypeName, JsonCodecBuilder> ENUM = t -> new Builder(BuilderType.ENUM, true,
                (u, c, r, p, v, i, vn) ->
                {
                    if (i)
                        c.add("$L.set($L,$T.enumerate($L))", r, p, Codec.class,
                                v);
                    else if (vn)
                        c.add("$L.put($L,$T.enumerate($L))", r, p, Codec.class,
                                v);
                    else
                        c.add("$L.put($S,$T.enumerate($L))", r, p, Codec.class,
                                v);
                },
                (u, c, r, p, i, vn) ->
                {
                    if (i || vn)
                        c.add("$T.enumerate($L.getInteger($L),$T.class)",
                                Codec.class, r, p, t);
                    else
                        c.add("$T.enumerate($L.getInteger($S),$T.class)",
                                Codec.class, r, p, t);
                });
        Function<TypeName, JsonCodecBuilder> ENUM_TEXT = t -> new Builder(BuilderType.ENUM, true,
                (u, c, r, p, v, i, vn) ->
                {
                    if (i)
                        c.add("$L.set($L,$T.enumerateText($L))", r, p, Codec.class,
                                v);
                    else if (vn)
                        c.add("$L.put($L,$T.enumerateText($L))", r, p, Codec.class,
                                v);
                    else
                        c.add("$L.put($S,$T.enumerateText($L))", r, p, Codec.class,
                                v);
                },
                (u, c, r, p, i, vn) ->
                {
                    if (i || vn)
                        c.add("$T.enumerateText($L.getString($L),$T.class)",
                                Codec.class, r, p, t);
                    else
                        c.add("$T.enumerateText($L.getString($S),$T.class)",
                                Codec.class, r, p, t);
                });
        Function<JsonCodecBuilder, JsonCodecBuilder> OPTIONAL = t -> new Builder(BuilderType.OBJECT, false,
                (u, c, r, p, v, i, vn) ->
                {
                    var k = CodeBlock.builder();
                    t.write(u, k, "r_" + p, "p_" + p, "v_" + p, true);
                    if (i)
                        c.add("$L.set($L,$T.option($L,(p_$L,r_$L,v_$L)->$L))",
                                r, p, Codec.class, v, p, p, p, k.build());
                    else if (vn)
                        c.add("$L.put($L,$T.option($L,(p_$L,r_$L,v_$L)->$L))",
                                r, p, Codec.class, v, p, p, p, k.build());
                    else
                        c.add("$L.put($S,$T.option($L,(p_$L,r_$L,v_$L)->$L))",
                                r, p, Codec.class, v, p, p, p, k.build());
                },
                (u, c, r, p, i, vn) ->
                {
                    var k = CodeBlock.builder();
                    t.read(u, k, "r_" + p, "p_" + p, true);
                    if (i || vn)
                        c.add("$T.option($L.getJsonArray($L),(p_$L,r_$L)->$L)",
                                Codec.class, r, p, p, p, k.build());
                    else
                        c.add("$T.option($L.getJsonArray($S),(p_$L,r_$L)->$L)",
                                Codec.class, r, p, p, p, k.build());
                });
        Function<JsonCodecBuilder, JsonCodecBuilder> JSON_MAP = t -> new Builder(BuilderType.OBJECT, false,
                (u, c, r, p, v, i, vn) ->
                {
                    var k = CodeBlock.builder();
                    t.write(u, k, "r_" + p, "p_" + p, "v_" + p, false, true);
                    if (i)
                        c.add("$L.set($L,$T.jsonMap($L,(p_$L,r_$L,v_$L)->$L))",
                                r, p, Codec.class, v, p, p, p, k.build());
                    else if (vn)
                        c.add("$L.put($L,$T.jsonMap($L,(p_$L,r_$L,v_$L)->$L))",
                                r, p, Codec.class, v, p, p, p, k.build());
                    else
                        c.add("$L.put($S,$T.jsonMap($L,(p_$L,r_$L,v_$L)->$L))",
                                r, p, Codec.class, v, p, p, p, k.build());
                },
                (u, c, r, p, i, vn) ->
                {
                    var k = CodeBlock.builder();
                    t.read(u, k, "r_" + p, "p_" + p, false, true);
                    if (i || vn)
                        c.add("$T.jsonMap($L.getJsonObject($L),(p_$L,r_$L)->$L)",
                                Codec.class, r, p, p, p, k.build());
                    else
                        c.add("$T.jsonMap($L.getJsonObject($S),(p_$L,r_$L)->$L)",
                                Codec.class, r, p, p, p, k.build());
                });

        //endregion
        interface Provider {
            @Nullable Builder lookup(Element where, Context ctx, TypeMirror type);

            default Provider or(Provider provider) {
                return (w, c, t) ->
                {
                    var fb = lookup(w, c, t);
                    if (fb != null) return fb;
                    return provider.lookup(w, c, t);
                };
            }
        }

        Provider PROVIDERS = ServiceLoader
                .load(Provider.class, Provider.class.getClassLoader())
                .stream().map(ServiceLoader.Provider::get)
                .reduce(Provider::or)
                .orElseGet(() -> (w, c, t) -> null);

        static JsonCodecBuilder lookup(Element where, Context ctx, TypeMirror type) {
            return switch (type.getKind()) {
                case BOOLEAN -> BOOLEAN;
                case BYTE -> BYTE;
                case SHORT -> SHORT;
                case INT -> INT;
                case LONG -> LONG;
                case CHAR -> CHAR;
                case FLOAT -> FLOAT;
                case DOUBLE -> DOUBLE;
                case ARRAY -> {
                    var ct = ((ArrayType) type).getComponentType();
                    yield switch (ct.getKind()) {
                        case BOOLEAN -> BOOLEAN_ARRAY;
                        case BYTE -> BYTE_ARRAY;
                        case SHORT -> SHORT_ARRAY;
                        case INT -> INT_ARRAY;
                        case LONG -> LONG_ARRAY;
                        case CHAR -> CHAR_ARRAY;
                        case FLOAT -> FLOAT_ARRAY;
                        case DOUBLE -> DOUBLE_ARRAY;
                        case ARRAY, DECLARED -> GENERIC_ARRAY.apply(lookup(where, ctx, ct), ct);
                        default -> {
                            var f = PROVIDERS.lookup(where, ctx, type);
                            if (f != null) yield f;
                            throw new IllegalStateException("unsupported array to json type: " + type + " at " + where);
                        }
                    };
                }
                case DECLARED -> {
                    var dt = (DeclaredType) type;
                    if (ctx.assignableTo(dt, Data.class)) {
                        var de = dt.asElement();
                        if (de.getKind().isInterface() && AnnotatedValue.find(de, Enhance.class).isPresent()){
                            var internal=Context.isInternal(de);
                            yield DATA_ENHANCED.apply(
                                    ClassName.get(ctx.packageOf(de).toString(), internal?Domain.CODECS_INTERNAL_NAME:Domain.CODECS_NAME),
                                    CaseConv.PASCAL_UPPER_SNAKE.apply(de.getSimpleName().toString()) + Domain.CODEC_DATA_SUFFIX);
                        }
                        else if (de.getKind().isInterface())
                            yield DATA.apply(null);
                        else
                            yield DATA.apply(TypeName.get(dt));
                    }
                    yield switch (dt.getTypeArguments().size()) {
                        //simple
                        case 0 -> {
                            var unbox = ctx.unbox(type);
                            if (unbox.isPresent()) {
                                yield switch (unbox.get().getKind()) {
                                    case BOOLEAN -> BOOLEAN_OBJECT;
                                    case BYTE -> BYTE_OBJECT;
                                    case SHORT -> SHORT_OBJECT;
                                    case INT -> INT_OBJECT;
                                    case LONG -> LONG_OBJECT;
                                    case CHAR -> CHAR_OBJECT;
                                    case FLOAT -> FLOAT_OBJECT;
                                    case DOUBLE -> DOUBLE_OBJECT;
                                    default -> throw new IllegalStateException("invalid boxed type: " + type);
                                };
                            }
                            if (ctx.sameType(dt, ITimes.ITime.class)) yield I_TIME;
                            if (ctx.sameType(dt, ITimes.IDate.class)) yield I_DATE;
                            if (ctx.sameType(dt, ITimes.IDatetime.class)) yield I_DATETIME;
                            if (ctx.sameType(dt, Entity.Entry.class)) yield ENTRY;
                            if (ctx.sameType(dt, UUID.class)) yield UUID;
                            if (ctx.sameType(dt, Period.class)) yield PERIOD;
                            if (ctx.sameType(dt, Duration.class)) yield DURATION;
                            if (ctx.sameType(dt, JsonObject.class)) yield JSON_OBJECT;
                            if (ctx.sameType(dt, JsonArray.class)) yield JSON_ARRAY;
                            if (ctx.sameType(dt, Instant.class)) yield INSTANT;
                            if (ctx.sameType(dt, BigDecimal.class)) yield BIG_DECIMAL;
                            if (ctx.sameType(dt, Numeric.class)) yield NUMERIC;
                            if (ctx.sameType(dt, OffsetDateTime.class)) yield DATETIME_TZ;
                            if (ctx.sameType(dt, OffsetTime.class)) yield TIME_TZ;
                            if (ctx.sameType(dt, LocalTime.class)) yield TIME;
                            if (ctx.sameType(dt, LocalDate.class)) yield DATE;
                            if (ctx.sameType(dt, LocalDateTime.class)) yield DATETIME;
                            if (ctx.sameType(dt, String.class)) yield STRING;
                            if (ctx.sameType(dt, Buffer.class)) yield BUFFER;
                            if (ctx.rawAssignableTo(dt, Enum.class)) {
                                yield ENUM.apply(TypeName.get(dt));
                            }
                            throw new IllegalStateException(
                                    "unsupported declared data type: " + type + " at " + where);
                        }
                        //!! list like
                        case 1 -> {
                            if (ctx.rawSameType(dt, Class.class)) {
                                yield CLASS;
                            }
                            if (ctx.assignableTo(ctx.erasure(dt), Collection.class)) {
                                yield GENERIC_COLLECTION.apply(lookup(where, ctx, dt.getTypeArguments().getFirst()),
                                        switch (dt.asElement().getSimpleName().toString()) {
                                            case "Set" -> ClassName.get(HashSet.class);
                                            case "List" -> ClassName.get(ArrayList.class);
                                            default -> ClassName.get(LinkedList.class);
                                        });
                            }
                            if (ctx.assignableTo(ctx.erasure(dt), Optional.class)) {
                                yield OPTIONAL.apply(lookup(where, ctx, dt.getTypeArguments().getFirst()));
                            }
                            var f = PROVIDERS.lookup(where, ctx, type);
                            if (f != null) yield f;
                            throw new IllegalStateException(
                                    "unsupported one parameter generic data type: " + type + " at " + where);
                        }
                        //!! map like
                        case 2 -> {
                            if (ctx.assignableTo(ctx.erasure(dt), Map.class)) {
                                var key = dt.getTypeArguments().get(0);
                                if (ctx.assignableTo(key, String.class)) {
                                    yield JSON_MAP.apply(lookup(where, ctx, dt.getTypeArguments().get(1)));
                                }
                                yield GENERIC_MAP.apply(lookup(where, ctx, dt.getTypeArguments().get(0)),
                                        lookup(where, ctx, dt.getTypeArguments().get(1)),
                                        ClassName.get(HashMap.class));
                            }
                            var f = PROVIDERS.lookup(where, ctx, type);
                            if (f != null) yield f;
                            throw new IllegalStateException(
                                    "unsupported two parameters generic data type: " + type + " at " + where);
                        }
                        default -> {
                            var f = PROVIDERS.lookup(where, ctx, type);
                            if (f != null) yield f;
                            throw new IllegalStateException("unsupported generic data type: " + type + " at " + where);
                        }
                    };
                }
                default -> {
                    var f = PROVIDERS.lookup(where, ctx, type);
                    if (f != null) yield f;
                    throw new IllegalStateException("unsupported type to json data type: " + type + " at " + where);
                }
            };

        }
    }

    interface FieldCodecBuilder {
        interface Maker {
            void make(CodeBlock.Builder cb, CharSequence model, CharSequence column, @Nullable CharSequence property,
                      @Nullable CodeBlock interceptor);
        }

        Class<?> field();

        default TypeName type() {
            return TypeName.get(field());
        }

        default void make(CodeBlock.Builder cb, CharSequence model, CharSequence column,
                          @Nullable CharSequence property, @Nullable CodeBlock interceptor) {
            if (property == null)
                if (interceptor != null)
                    cb.add("new $T($S,null,$L,$L)", type(), column, model, interceptor);
                else
                    cb.add("new $T($S,null,$L)", type(), column, model);
            else if (interceptor != null)
                cb.add("new $T($S,$S,$L,$L)", type(), column, property, model, interceptor);
            else
                cb.add("new $T($S,$S,$L)", type(), column, property, model);
        }

        record Defined(TypeName type, Class<?> field, Maker maker) implements FieldCodecBuilder {
            @Override
            public void make(CodeBlock.Builder cb, CharSequence model, CharSequence column,
                             @Nullable CharSequence property, @Nullable CodeBlock interceptor) {
                maker.make(cb, model, column, property, interceptor);
            }
        }

        FieldCodecBuilder BOOLEAN_FIELD = () -> Field.BooleanField.class;
        FieldCodecBuilder INTEGER_FIELD = () -> Field.IntegerField.class;
        FieldCodecBuilder BYTE_FIELD = () -> Field.ByteField.class;
        FieldCodecBuilder LONG_FIELD = () -> Field.LongField.class;
        FieldCodecBuilder FLOAT_FIELD = () -> Field.FloatField.class;
        FieldCodecBuilder DOUBLE_FIELD = () -> Field.DoubleField.class;
        FieldCodecBuilder SHORT_FIELD = () -> Field.ShortField.class;
        FieldCodecBuilder STRING_FIELD = () -> Field.StringField.class;
        FieldCodecBuilder NUMERIC_FIELD = () -> Field.NumericField.class;
        FieldCodecBuilder DECIMAL_FIELD = () -> Field.DecimalField.class;
        FieldCodecBuilder INSTANT_FIELD = () -> Field.InstantField.class;
        FieldCodecBuilder DATE_FIELD = () -> Field.DateField.class;
        FieldCodecBuilder TIME_FIELD = () -> Field.TimeField.class;
        FieldCodecBuilder DATE_TIME_FIELD = () -> Field.DateTimeField.class;
        FieldCodecBuilder TIME_TZ_FIELD = () -> Field.TimeTZField.class;
        FieldCodecBuilder DATE_TIME_TZ_FIELD = () -> Field.DateTimeTZField.class;
        FieldCodecBuilder BUFFER_FIELD = () -> Field.BufferField.class;
        FieldCodecBuilder JSON_ARRAY_FIELD = () -> Field.JsonArrayField.class;
        FieldCodecBuilder JSON_OBJECT_FIELD = () -> Field.JsonObjectField.class;
        FieldCodecBuilder CLASS_FIELD = () -> Field.ClassField.class;
        FieldCodecBuilder BYTES_FIELD = () -> Field.BytesField.class;
        FieldCodecBuilder DURATION_FIELD = () -> Field.DurationField.class;
        FieldCodecBuilder PERIOD_FIELD = () -> Field.PeriodField.class;
        FieldCodecBuilder UUID_FIELD = () -> Field.UUIDField.class;
        FieldCodecBuilder INTEGER_TIME_FIELD = () -> Field.IntegerTimeField.class;
        FieldCodecBuilder INTEGER_DATE_FIELD = () -> Field.IntegerDateField.class;
        FieldCodecBuilder LONG_DATETIME_FIELD = () -> Field.LongDatetimeField.class;

        Function<TypeName, FieldCodecBuilder> ENUM_ORDINAL_FIELD = t -> new FieldCodecBuilder.Defined(
                ParameterizedTypeName.get(ClassName.get(Field.EnumOrdinalField.class), t),
                Field.EnumOrdinalField.class,
                (cb, m, c, p, inpt) -> {
                    if (p == null)
                        if (inpt != null)
                            cb.add("new $T<>($S,null,$T.class,$L,$L)", Field.EnumOrdinalField.class, c, t, m, inpt);
                        else
                            cb.add("new $T<>($S,null,$T.class,$L)", Field.EnumOrdinalField.class, c, t, m);
                    else if (inpt != null)
                        cb.add("new $T<>($S,$S,$T.class,$L,$L)", Field.EnumOrdinalField.class, c, p, t, m, inpt);
                    else
                        cb.add("new $T<>($S,$S,$T.class,$L)", Field.EnumOrdinalField.class, c, p, t, m);
                }

        );

        Function<TypeName, FieldCodecBuilder> ENUM_TEXT_FIELD = t -> new FieldCodecBuilder.Defined(
                ParameterizedTypeName.get(ClassName.get(Field.EnumTextField.class), t),
                Field.EnumTextField.class,
                (cb, m, c, p, inpt) -> {
                    if (p == null)
                        if (inpt != null)
                            cb.add("new $T<>($S,null,$T.class,$L,$L)", Field.EnumTextField.class, c, t, m, inpt);
                        else
                            cb.add("new $T<>($S,null,$T.class,$L)", Field.EnumTextField.class, c, t, m);
                    else if (inpt != null)
                        cb.add("new $T<>($S,$S,$T.class,$L,$L)", Field.EnumTextField.class, c, p, t, m, inpt);
                    else
                        cb.add("new $T<>($S,$S,$T.class,$L)", Field.EnumTextField.class, c, p, t, m);
                }
        );

        interface Provider {
            @Nullable FieldCodecBuilder lookup(Element where, Context ctx, TypeMirror type);

            default Provider or(Provider provider) {
                return (w, c, t) ->
                {
                    var fb = lookup(w, c, t);
                    if (fb != null) return fb;
                    return provider.lookup(w, c, t);
                };
            }
        }

        Provider PROVIDERS = ServiceLoader
                .load(Provider.class, Provider.class.getClassLoader())
                .stream().map(ServiceLoader.Provider::get)
                .reduce(Provider::or)
                .orElseGet(() -> (w, c, t) -> null);

        static FieldCodecBuilder lookup(Element where, Context ctx, TypeMirror type) {
            return switch (type.getKind()) {
                case BOOLEAN -> BOOLEAN_FIELD;
                case BYTE -> BYTE_FIELD;
                case SHORT -> SHORT_FIELD;
                case INT -> INTEGER_FIELD;
                case LONG -> LONG_FIELD;
                case FLOAT -> FLOAT_FIELD;
                case DOUBLE -> DOUBLE_FIELD;
                case ARRAY -> {
                    var ct = ((ArrayType) type).getComponentType();
                    yield switch (ct.getKind()) {
                        case BYTE -> BYTES_FIELD;
                        default -> {
                            var f = PROVIDERS.lookup(where, ctx, type);
                            if (f != null) yield f;
                            throw new IllegalStateException("unsupported array to json type: " + type + " at " + where);
                        }
                    };
                }
                case DECLARED -> {
                    var dt = (DeclaredType) type;
                    yield switch (dt.getTypeArguments().size()) {
                        //simple
                        case 0 -> {
                            var unbox = ctx.unbox(type);
                            if (unbox.isPresent()) {
                                yield switch (unbox.get().getKind()) {
                                    case BOOLEAN -> BOOLEAN_FIELD;
                                    case BYTE -> BYTE_FIELD;
                                    case SHORT -> SHORT_FIELD;
                                    case INT -> INTEGER_FIELD;
                                    case LONG -> LONG_FIELD;
                                    case FLOAT -> FLOAT_FIELD;
                                    case DOUBLE -> DOUBLE_FIELD;
                                    default -> throw new IllegalStateException("invalid boxed type: " + type);
                                };
                            }
                            if (ctx.rawAssignableTo(dt, Enum.class)) yield ENUM_ORDINAL_FIELD.apply(TypeName.get(type));
                            if (ctx.sameType(dt, UUID.class)) yield UUID_FIELD;
                            if (ctx.sameType(dt, Period.class)) yield PERIOD_FIELD;
                            if (ctx.sameType(dt, Duration.class)) yield DURATION_FIELD;

                            if (ctx.sameType(dt, ITimes.IDatetime.class)) yield LONG_DATETIME_FIELD;
                            if (ctx.sameType(dt, ITimes.IDate.class)) yield INTEGER_DATE_FIELD;
                            if (ctx.sameType(dt, ITimes.ITime.class)) yield INTEGER_TIME_FIELD;

                            if (ctx.sameType(dt, JsonObject.class)) yield JSON_OBJECT_FIELD;
                            if (ctx.sameType(dt, JsonArray.class)) yield JSON_ARRAY_FIELD;
                            if (ctx.sameType(dt, Instant.class)) yield INSTANT_FIELD;
                            if (ctx.sameType(dt, BigDecimal.class)) yield DECIMAL_FIELD;
                            if (ctx.sameType(dt, OffsetDateTime.class)) yield DATE_TIME_TZ_FIELD;
                            if (ctx.sameType(dt, OffsetTime.class)) yield TIME_TZ_FIELD;
                            if (ctx.sameType(dt, LocalTime.class)) yield TIME_FIELD;
                            if (ctx.sameType(dt, LocalDate.class)) yield DATE_FIELD;
                            if (ctx.sameType(dt, LocalDateTime.class)) yield DATE_TIME_FIELD;
                            if (ctx.sameType(dt, String.class)) yield STRING_FIELD;
                            if (ctx.sameType(dt, Buffer.class)) yield BUFFER_FIELD;
                            if (ctx.sameType(dt, Numeric.class)) yield NUMERIC_FIELD;
                            var f = PROVIDERS.lookup(where, ctx, type);
                            if (f != null) yield f;
                            throw new IllegalStateException(
                                    "unsupported declared field type: " + type + " at " + where);
                        }
                        case 1 -> {
                            if (ctx.rawSameType(dt, Class.class)) yield CLASS_FIELD;
                            var f = PROVIDERS.lookup(where, ctx, type);
                            if (f != null) yield f;
                            throw new IllegalStateException(
                                    "unsupported declared generic field type: " + type + " at " + where);
                        }
                        default -> {
                            var f = PROVIDERS.lookup(where, ctx, type);
                            if (f != null) yield f;
                            throw new IllegalStateException(
                                    "unsupported generic field data type: " + type + " at " + where);
                        }
                    };
                }
                default -> {
                    var f = PROVIDERS.lookup(where, ctx, type);
                    if (f != null) yield f;
                    throw new IllegalStateException("unsupported type to field data type: " + type + " at " + where);
                }
            };
        }

    }

    interface BufferCodecBuilder {
        void apply(CodeBlock.Builder read, CodeBlock.Builder write, String p, boolean required,
                   boolean inOptional,
                   boolean outOptional,
                   boolean prim);

        interface Operate extends UnaryOperator<CodeBlock.Builder> {


            default Operate then(@NotNull Operate after) {
                Objects.requireNonNull(after);
                return t -> after.apply(apply(t));
            }
        }

        interface Functor<T> extends Function<T, Operate> {

        }

        interface Functor2<T0, T1> extends BiFunction<T0, T1, Operate> {

        }

        Functor<String> IDENTITY = s -> i -> i.add("$L", s);
        Operate OR_ELSE_THROW = i -> i.add(".orElseThrow($T.BAD_REQUEST_SYSTEM)", DomainError.class);
        Operate OR_ELSE_NULL = i -> i.add(".orElse(null)");
        Functor<String> OPTIONAL_NULLABLE = n -> i -> i.add("$T.ofNullable($L)", Optional.class, n);
        Functor<String> OPTIONAL_OF = n -> i -> i.add("$T.of($L)", Optional.class, n);
        Functor<String> MAP_BUF_READ = n -> i -> i.add(".map($T::$L)", Buf.class, n);
        Functor<String> MAP_BUF_WRITE = n -> i -> i.add(".map(i->$T.of().$L(i))", Buf.class, n);
        /// map lambda to create buf and set value
        /// (op,cast)->operator, cast should have parens.
        Functor2<String, String> MAP_BUF_CAST_WRITE = (n, c) -> i -> i.add(".map(i->$T.of().$L($Li))", Buf.class, n, c);
        Functor2<String, String> MAP_BUF_CAST_READ = (n, c) -> i -> i.add(".map(i->$L i.$L(i))", c, n);
        Functor<Operate> MAP = a -> i -> i.add(".map($L)", a.apply(CodeBlock.builder()).build());
        BiFunction<BufferCodecBuilder.Operate, BufferCodecBuilder.Operate, BufferCodecBuilder> SCALAR = (write, read) -> (
                r, w, p, q, io, oo, prim
        ) -> {
            //write to buf, input is a value or optional value. output is a buf
            if (w != null) {
                Function<CodeBlock.Builder, CodeBlock.Builder> x =
                        (io ? BufferCodecBuilder.IDENTITY.apply(p) :
                                (prim ? BufferCodecBuilder.OPTIONAL_OF : BufferCodecBuilder.OPTIONAL_NULLABLE).apply(p)).then(write);
                if (!oo) {
                    if (q) x = x.andThen(BufferCodecBuilder.OR_ELSE_THROW);
                    else x = x.andThen(BufferCodecBuilder.OR_ELSE_NULL);
                }
                x.apply(w);
            }
            if (r != null) {
                //read from buffer, input is an optional buf
                Function<CodeBlock.Builder, CodeBlock.Builder> x = read;
                if (!oo) {
                    if (q) x = x.andThen(BufferCodecBuilder.OR_ELSE_THROW);
                    else x = x.andThen(BufferCodecBuilder.OR_ELSE_NULL);
                }
                x.apply(r.add("$L", p));
            }
        };
        Function<String, BufferCodecBuilder> SIMPLE = a -> SCALAR.apply(BufferCodecBuilder.MAP_BUF_WRITE.apply(a), BufferCodecBuilder.MAP_BUF_READ.apply(a));

        interface Func3<T0, T1, T2> {
            BufferCodecBuilder apply(T0 t0, T1 t1, T2 t2);
        }

        Func3<String, String, String> BUF_CAST = (a, to, from) -> SCALAR.apply(BufferCodecBuilder.MAP_BUF_CAST_WRITE.apply(a, to), BufferCodecBuilder.MAP_BUF_CAST_READ.apply(a, from));
        BufferCodecBuilder BUF_BOOLEAN = SIMPLE.apply("bool");
        BufferCodecBuilder BUF_BYTE = SIMPLE.apply("i8");
        BufferCodecBuilder BUF_SHORT = SIMPLE.apply("i16");
        BufferCodecBuilder BUF_INT = SIMPLE.apply("i32");
        BufferCodecBuilder BUF_LONG = SIMPLE.apply("i64");
        BufferCodecBuilder BUF_FLOAT = SIMPLE.apply("f32");
        BufferCodecBuilder BUF_DOUBLE = SIMPLE.apply("f64");
        BufferCodecBuilder BUF_CHAR = BUF_CAST.apply("i32", "(int)(char)", "(char)(int)");
        BufferCodecBuilder BUF_BYTE_ARRAY = SIMPLE.apply("binary");
        BufferCodecBuilder BUF_STRING = SIMPLE.apply("string");

        BufferCodecBuilder BUF_VOID = (r, w, p, q, io, oo, prim) -> {
            //write to buf
            if (w != null)
                w.add("$T.empty()", Optional.class);
            if (r != null)
                r.add("null");
        };
        BufferCodecBuilder BUF_JSON_ARRAY = SCALAR.apply(
                BufferCodecBuilder.MAP.apply(i -> i.add("$T::toBuffer", JsonArray.class))
                        .then(BufferCodecBuilder.MAP.apply(i -> i.add("$T::of", Buf.class))),
                BufferCodecBuilder.MAP.apply(i -> i.add("$T::toJsonArray", Buf.class))
        );
        BufferCodecBuilder BUF_JSON_OBJECT = SCALAR.apply(
                BufferCodecBuilder.MAP.apply(i -> i.add("$T::toBuffer", JsonObject.class))
                        .then(BufferCodecBuilder.MAP.apply(i -> i.add("$T::of", Buf.class))),
                BufferCodecBuilder.MAP.apply(i -> i.add("$T::toJsonObject", Buf.class))
        );
        BufferCodecBuilder BUF_BUFFER = SCALAR.apply(
                BufferCodecBuilder.MAP.apply(i -> i.add("$T::buffer", Buf.class)),
                BufferCodecBuilder.MAP.apply(i -> i.add("$T::of", Buf.class)));

        BiFunction<ClassName, String, BufferCodecBuilder> BUF_DATA = (codec, name) -> SCALAR.apply(
                BufferCodecBuilder.MAP.apply(i -> i.add("$T.$L::buf", codec, name)),
                BufferCodecBuilder.MAP.apply(i -> i.add("$T.$L::buf", codec, name)));

        BiFunction<ClassName, String, BufferCodecBuilder> BUF_BINARY_DATA = (codec, name) -> SCALAR.apply(
                BufferCodecBuilder.MAP.apply(i -> i.add("$T.$L::from).map($T.$L::buf",
                        codec, name,
                        codec, name+Domain.CODEC_BINARY_SUFFIX)),
                BufferCodecBuilder.MAP.apply(i -> i.add("$T.$L::read",
                        codec, name+Domain.CODEC_BINARY_SUFFIX
                        )));

        Function<TypeMirror, BufferCodecBuilder> BUF_DATA_CLASS = t -> SCALAR.apply(
                BufferCodecBuilder.MAP.apply(i -> i.add("$T::toBuf", Codec.class)),
                BufferCodecBuilder.MAP.apply(i -> i.add("$T.fromBuf($T::new)", Codec.class, t))
        );
        Function<TypeMirror, BufferCodecBuilder> BUF_BINARY_DATA_CLASS = t -> SCALAR.apply(
                BufferCodecBuilder.MAP.apply(i -> i.add("$T::binaryToBuf", Codec.class)),
                BufferCodecBuilder.MAP.apply(i -> i.add("$T.binaryFromBuf($T::new)", Codec.class, t))
        );
        Function<TypeMirror, BufferCodecBuilder> BUF_ENUM = t -> SCALAR.apply(
                BufferCodecBuilder.MAP.apply(i -> i.add("$T::enumToBuf", Codec.class)),
                BufferCodecBuilder.MAP.apply(i -> i.add("$T.enumFromBuf($T.class)", Codec.class, t))
        );
        Function3<TypeName,TypeName,BufferCodecBuilder, BufferCodecBuilder> BUF_COLLECTION_LIST = (iType,lType,in) -> SCALAR.apply(
                BufferCodecBuilder.MAP.apply(i -> {
                    var inner = CodeBlock.builder();
                    in.apply(null, inner, "vx", false, false, false, false);
                    i.add("$T.collectionToBuf(vx->$L)", Codec.class, inner.build());
                    return i;
                }),
                BufferCodecBuilder.MAP.apply(i -> {
                    var inner = CodeBlock.builder();
                    in.apply(inner, null, "vx", false, false, false, false);
                    i.add("$T.<$T,$T>collectionFromBuf($T::new,vx->$L)", Codec.class,iType,lType, ArrayList.class, inner.build());
                    return i;
                })
        );
        Function3<TypeName,TypeName,BufferCodecBuilder, BufferCodecBuilder> BUF_COLLECTION_SET = (iType,lType,in) -> SCALAR.apply(
                BufferCodecBuilder.MAP.apply(i -> {
                    var inner = CodeBlock.builder();
                    in.apply(null, inner, "vx", false, false, false, false);
                    i.add("$T.collectionToBuf(vx->$L)", Codec.class, inner.build());
                    return i;
                }),
                BufferCodecBuilder.MAP.apply(i -> {
                    var inner = CodeBlock.builder();
                    in.apply(inner, null, "vx", false, false, false, false);
                    i.add("$T.<$T,$T>collectionFromBuf($T::new,vx->$L)", Codec.class,iType,lType, HashSet.class, inner.build());
                    return i;
                }));
        BiFunction<BufferCodecBuilder, BufferCodecBuilder, BufferCodecBuilder> BUF_MAPPING = (K, V) -> SCALAR.apply(
                BufferCodecBuilder.MAP.apply(i -> {
                    var k = CodeBlock.builder();
                    var v = CodeBlock.builder();
                    K.apply(null, k, "vx", false, false, false, false);
                    V.apply(null, v, "vx", false, false, false, false);
                    i.add("$T.mapToBuf(vx->$L,vx->$L)", Codec.class, k.build(), v.build());
                    return i;
                }),
                BufferCodecBuilder.MAP.apply(i -> {
                    var k = CodeBlock.builder();
                    var v = CodeBlock.builder();
                    K.apply(k, null, "vx", false, false, false, false);
                    V.apply(v, null, "vx", false, false, false, false);
                    i.add("$T.mapFromBuf($T::new,vx->$L,vx->$L)", Codec.class, HashMap.class, k.build(), v.build());
                    return i;
                })
        );

        interface Provider {
            @Nullable CodecBuilder.BufferCodecBuilder lookup(ExecutableElement where, Context ctx, TypeMirror type);

            default Provider or(Provider provider) {
                return (w, c, t) ->
                {
                    var fb = lookup(w, c, t);
                    if (fb != null) return fb;
                    return provider.lookup(w, c, t);
                };
            }
        }

        Provider PROVIDERS = ServiceLoader
                .load(Provider.class, Provider.class.getClassLoader())
                .stream().map(ServiceLoader.Provider::get)
                .reduce(Provider::or)
                .orElseGet(() -> (w, c, t) -> null);

        static BufferCodecBuilder lookup(ExecutableElement where, Context ctx, TypeMirror type) {
            return switch (type.getKind()) {
                case BOOLEAN -> BUF_BOOLEAN;
                case BYTE -> BUF_BYTE;
                case SHORT -> BUF_SHORT;
                case INT -> BUF_INT;
                case LONG -> BUF_LONG;
                case CHAR -> BUF_CHAR;
                case FLOAT -> BUF_FLOAT;
                case DOUBLE -> BUF_DOUBLE;
                case ARRAY -> {
                    var at = (ArrayType) type;
                    var e = at.getComponentType();
                    if (e.getKind() == TypeKind.BYTE) {
                        yield BUF_BYTE_ARRAY;
                    }
                    var t = PROVIDERS.lookup(where, ctx, type);
                    if (t != null) yield t;
                    throw new IllegalStateException("unsupported ARRAY parameter type: " + type);
                }
                case DECLARED -> {
                    var pt = ctx.unbox(type);
                    if (pt.isPresent()) {
                        yield switch (pt.get().getKind()) {
                            case BOOLEAN -> BUF_BOOLEAN;
                            case BYTE -> BUF_BYTE;
                            case SHORT -> BUF_SHORT;
                            case INT -> BUF_INT;
                            case LONG -> BUF_LONG;
                            case CHAR -> BUF_CHAR;
                            case FLOAT -> BUF_FLOAT;
                            case DOUBLE -> BUF_DOUBLE;
                            case VOID -> BUF_VOID;
                            default -> throw new IllegalStateException("unsupported Boxed argument type:" + type);
                        };

                    } else if (ctx.rawAssignableTo(type, String.class)) yield BUF_STRING;
                    else if (ctx.rawAssignableTo(type, Buffer.class)) yield BUF_BUFFER;
                    else if (ctx.rawAssignableTo(type, JsonArray.class)) yield BUF_JSON_ARRAY;
                    else if (ctx.rawAssignableTo(type, JsonObject.class)) yield BUF_JSON_OBJECT;
                    else if (ctx.rawAssignableTo(type, Void.class)) yield BUF_VOID;
                    else if (ctx.rawAssignableTo(type, Data.class)) {
                        var ele = ctx.typeElementOf(type);
                        assert ele != null : "missing element of " + type;
                        if (ele.getKind() == ElementKind.INTERFACE) {
                            var binary=ctx.isBinary(ele);
                            var internal=Context.isInternal(ele);
                            var pkg = ctx.packageOf(ele).getQualifiedName().toString();
                            var name = CaseConv.PASCAL_UPPER_SNAKE.apply(ele.getSimpleName().toString()) + "_DATA";
                            var codec=ClassName.get(pkg,internal?Domain.CODECS_INTERNAL_NAME:Domain.CODECS_NAME);
                            if(binary){
                                yield BUF_BINARY_DATA.apply(codec,name);
                            }
                            yield BUF_DATA.apply(codec, name);
                        } else {
                            if(ctx.rawAssignableTo(type,Data.Binary.class))
                                yield BUF_BINARY_DATA_CLASS.apply(type);
                            yield BUF_DATA_CLASS.apply(type);
                        }
                    }
                    if (ctx.rawAssignableTo(type, Enum.class)) yield BUF_ENUM.apply(type);
                    else if (ctx.rawAssignableTo(type, Collection.class)) {
                        var dt = (DeclaredType) type;
                        var inner = dt.getTypeArguments().getFirst();
                        var in = lookup(where, ctx, inner);
                        if (ctx.rawSameType(type, List.class)) yield BUF_COLLECTION_LIST.apply(TypeName.get(inner),TypeName.get(dt),in);
                        if (ctx.rawSameType(type, Set.class)) yield BUF_COLLECTION_SET.apply(TypeName.get(inner),TypeName.get(dt),in);
                    } else if (ctx.rawSameType(type, Map.class)) {
                        var dt = (DeclaredType) type;
                        var k = dt.getTypeArguments().get(0);
                        var K = lookup(where, ctx, k);
                        var v = dt.getTypeArguments().get(1);
                        var V = lookup(where, ctx, v);
                        yield BUF_MAPPING.apply(K, V);
                    }
                    var t = PROVIDERS.lookup(where, ctx, type);
                    if (t != null) yield t;
                    throw new IllegalStateException("unsupported DECLARED parameter type: " + type);
                }
                default -> {
                    var t = PROVIDERS.lookup(where, ctx, type);
                    if (t != null) yield t;
                    throw new IllegalStateException("unsupported parameter type: " + type);
                }
            };
        }
    }

    interface AuditingCodecBuilder {
        /// build a lambda `x->JsonObject`, that enclosures argument.
        void input(CodeBlock.Builder b, String argument, boolean nullable);

        /// build a lambda `x->JsonObject`
        void output(CodeBlock.Builder b, boolean optional, boolean nullable);

        void element(CodeBlock.Builder b);

        record AuditingCodec(
                Consumer3<CodeBlock.Builder, String, Boolean> in,
                Consumer3<CodeBlock.Builder, Boolean, Boolean> out,
                Consumer<CodeBlock.Builder> ele
        ) implements AuditingCodecBuilder {

            @Override
            public void input(CodeBlock.Builder b, String argument, boolean nullable) {
                in.accept(b, argument, nullable);
            }

            @Override
            public void output(CodeBlock.Builder b, boolean optional, boolean nullable) {
                out.accept(b, optional, nullable);
            }

            @Override
            public void element(CodeBlock.Builder b) {
                ele.accept(b);
            }
        }

        AuditingCodecBuilder AUDITING_PRIMITIVE = new AuditingCodec(
                (b, a, t) -> b.add("$T.of($S,$L)", JsonObject.class, a, a),
                (b, o, t) -> {
                    if (o) b.add("$$$$->$T.of($S,$$$$.orElse(null))", JsonObject.class, "value");
                    else b.add("$$$$->$T.of($S,$$$$)", JsonObject.class, "value");
                },
                (b) -> b.add("$T::identity", Function.class)
        );

        AuditingCodecBuilder AUDITING_DATA = new AuditingCodec(
                (b, a, t) -> {
                    if (t) b.add("$T.of($S,$L==null?null:$L.toJson())", JsonObject.class, a, a, a);
                    else b.add("$T.of($S,$L.toJson())", JsonObject.class, a, a);
                },
                (b, o, t) -> {
                    if (o) {
                        b.add("$$$$->$T.of($S,$$$$.map($T::toJson).orElse(null)", JsonObject.class, "value", Data.class);
                    } else if (t) {
                        b.add("$$$$->$T.of($S,$$$$==null?null:$$$$.toJson())", JsonObject.class, "value");
                    } else {
                        b.add("$$$$->$T.of($S,$$$$.toJson())", JsonObject.class, "value");
                    }
                },
                (b) -> b.add("$T.nullSafe($T::toJson)", Fn.class, Data.class)
        );
        UnaryOperator<AuditingCodecBuilder> AUDITING_COLLECTION = in -> {
            var x = CodeBlock.builder();
            in.element(x);
            var $ = x.build();
            return new AuditingCodec(
                    (b, a, t) -> b.add("$T.of($S,$T.nullSafeCollectionMapping($L).apply($L))", JsonObject.class, a, Fn.class, $, a),
                    (b, o, t) -> {
                        if (o) {
                            b.add("$$$$->$T.of($S,$$$$.map($T.nullSafeCollectionMapping($L)).orElse(null)", JsonObject.class, "value", Fn.class, $);
                        } else {
                            b.add("$$$$->$T.of($S,$T.nullSafeCollectionMapping($L).apply($$$$))", JsonObject.class, "value", Fn.class, $);
                        }
                    },
                    (b) -> b.add("$T.nullSafeCollectionMapping($L)", Fn.class, $)
            );
        };
        BiFunction<AuditingCodecBuilder, AuditingCodecBuilder, AuditingCodecBuilder> AUDITING_MAPPING = (k, v) -> {
            var x = CodeBlock.builder();
            k.element(x);
            var $k = x.build();
            x = CodeBlock.builder();
            v.element(x);
            var $v = x.build();
            return new AuditingCodec(
                    (b, a, t) -> b.add("$T.of($S,$T.nullSafeMapMapping($L,$L).apply($L))", JsonObject.class, a, Fn.class, $k, $v, a),
                    (b, o, t) -> {
                        if (o) {
                            b.add("$$$$->$T.of($S,$$$$.map($T.nullSafeMapMapping($L,$L)).orElse(null)", JsonObject.class, "value", Fn.class, $k, $v);
                        } else {
                            b.add("$$$$->$T.of($S,$T.nullSafeMapMapping($L,$L).apply($$$$))", JsonObject.class, "value", $k, $v);
                        }
                    },
                    (b) -> b.add("$T.nullSafeMapMapping($L,$L))", Fn.class, $k, $v)
            );
        };
        Function<String, AuditingCodecBuilder> AUDITING_ENUM = type -> new AuditingCodec(
                (b, a, t) -> b.add("$T.of($S,$T.of($S,$S,$S,$L==null?\"\":$L.name()))",
                        JsonObject.class, a,
                        JsonObject.class, "type", type,
                        "name", a, a),
                (b, o, t) -> {
                    if (o)
                        b.add("$$$$->$T.of($S,$$$$.map($$_->$T.of($S,$S,$S,$$_==null?\"\":$$_.name())).orElse(null))",
                                JsonObject.class, "value",
                                JsonObject.class,
                                "type", type,
                                "name"
                        );
                    else if (t) b.add("$$$$->$$$$==null?null:$T.of($S,$T.of($S,$S,$S,$$$$==null?\"\":$$$$.name())",
                            JsonObject.class, "value",
                            JsonObject.class, "type", type,
                            "name"
                    );
                    else b.add("$$$$->$T.of($S,$T.of($S,$S,$S,$$$$==null?\"\":$$$$.name())",
                                JsonObject.class, "value",
                                JsonObject.class, "type", type,
                                "name"
                        );
                },
                (b) -> b.add("$T::identity", Function.class)
        );

        interface Provider {
            @Nullable AuditingCodecBuilder lookup(Element where, Context ctx, TypeMirror type);

            default Provider or(Provider provider) {
                return (w, c, t) ->
                {
                    var fb = lookup(w, c, t);
                    if (fb != null) return fb;
                    return provider.lookup(w, c, t);
                };
            }
        }

        Provider PROVIDERS = ServiceLoader
                .load(Provider.class, Provider.class.getClassLoader())
                .stream().map(ServiceLoader.Provider::get)
                .reduce(Provider::or)
                .orElseGet(() -> (w, c, t) -> null);

        static AuditingCodecBuilder lookup(Element where, Context ctx, TypeMirror type) {
            return switch (type.getKind()) {
                case BOOLEAN,
                     BYTE,
                     SHORT,
                     INT,
                     LONG,
                     CHAR,
                     FLOAT,
                     DOUBLE, VOID -> AUDITING_PRIMITIVE;
                case ARRAY -> {
                    var at = (ArrayType) type;
                    var e = at.getComponentType();
                    if (e.getKind() == TypeKind.BYTE) {
                        yield AUDITING_PRIMITIVE;
                    }
                    var t = PROVIDERS.lookup(where, ctx, type);
                    if (t != null) yield t;
                    throw new IllegalStateException("unsupported ARRAY auditing  type: " + type + " in " + where);
                }
                case DECLARED -> {
                    var pt = ctx.unbox(type);
                    if (pt.isPresent()) {
                        yield switch (pt.get().getKind()) {
                            case BOOLEAN,
                                 BYTE,
                                 SHORT,
                                 INT,
                                 LONG,
                                 CHAR,
                                 FLOAT,
                                 DOUBLE, VOID -> AUDITING_PRIMITIVE;
                            default -> throw new IllegalStateException("unsupported Boxed argument type:" + type);
                        };

                    } else if (ctx.rawAssignableTo(type, String.class)) yield AUDITING_PRIMITIVE;
                    else if (ctx.rawAssignableTo(type, Buffer.class)) yield AUDITING_PRIMITIVE;
                    else if (ctx.rawAssignableTo(type, JsonArray.class)) yield AUDITING_PRIMITIVE;
                    else if (ctx.rawAssignableTo(type, JsonObject.class)) yield AUDITING_PRIMITIVE;
                    else if (ctx.rawAssignableTo(type, Void.class)) yield AUDITING_PRIMITIVE;
                    else if (ctx.rawAssignableTo(type, Data.class)) {
                        yield AUDITING_DATA;
                    }
                    if (ctx.rawAssignableTo(type, Enum.class))
                        yield AUDITING_ENUM.apply(((TypeElement) ((DeclaredType) type).asElement()).getQualifiedName().toString());
                    else if (ctx.rawAssignableTo(type, Collection.class)) {
                        var dt = (DeclaredType) type;
                        var inner = dt.getTypeArguments().getFirst();
                        var in = lookup(where, ctx, inner);
                        yield AUDITING_COLLECTION.apply(in);
                    } else if (ctx.rawSameType(type, Map.class)) {
                        var dt = (DeclaredType) type;
                        var k = dt.getTypeArguments().get(0);
                        var K = lookup(where, ctx, k);
                        var v = dt.getTypeArguments().get(1);
                        var V = lookup(where, ctx, v);
                        yield AUDITING_MAPPING.apply(K, V);
                    }
                    var t = PROVIDERS.lookup(where, ctx, type);
                    if (t != null) yield t;
                    throw new IllegalStateException("unsupported DECLARED auditing type: " + type);
                }
                default -> {
                    var t = PROVIDERS.lookup(where, ctx, type);
                    if (t != null) yield t;
                    throw new IllegalStateException("unsupported auditing type: " + type + " in " + where);
                }
            };
        }
    }

    interface BinaryCodecBuilder {
        interface Writer {
            CodeBlock.Builder write(Context ctx, CodeBlock.Builder cb, CharSequence receiver, CharSequence value, int deep);
        }

        interface Reader {
            CodeBlock.Builder read(Context ctx, CodeBlock.Builder cb, CharSequence receiver, int deep);
        }

        CodeBlock.Builder write(Context ctx, CodeBlock.Builder cb, CharSequence receiver, CharSequence value, int deep);

        CodeBlock.Builder read(Context ctx, CodeBlock.Builder cb, CharSequence receiver, int deep);

        record BinaryCodec(Writer w, Reader r) implements BinaryCodecBuilder {

            @Override
            public CodeBlock.Builder write(Context ctx, CodeBlock.Builder cb, CharSequence receiver, CharSequence value, int deep) {
                return w.write(ctx, cb, receiver, value, deep);
            }

            @Override
            public CodeBlock.Builder read(Context ctx, CodeBlock.Builder cb, CharSequence receiver, int deep) {
                return r.read(ctx, cb, receiver, deep);
            }
        }

        interface Provider {
            @Nullable BinaryCodecBuilder lookup(Element where, Context ctx, TypeMirror type);

            default Provider or(Provider provider) {
                return (w, c, t) ->
                {
                    var fb = lookup(w, c, t);
                    if (fb != null) return fb;
                    return provider.lookup(w, c, t);
                };
            }
        }

        Provider PROVIDERS = ServiceLoader
                .load(Provider.class, Provider.class.getClassLoader())
                .stream().map(ServiceLoader.Provider::get)
                .reduce(Provider::or)
                .orElseGet(() -> (w, c, t) -> null);

        BinaryCodecBuilder BOOLEAN_ARRAY = new BinaryCodec(
                (u, c, r, v, d) ->
                        c.add("$L.boolArray($L)", r, v),
                (u, c, r, d) ->
                        c.add("$L.boolArray()", r)
        );
        BinaryCodecBuilder SHORT_ARRAY = new BinaryCodec(
                (u, c, r, v, d) ->
                        c.add("$L.i16Array($L)", r, v),
                (u, c, r, d) ->
                        c.add("$L.i16Array()", r)
        );
        BinaryCodecBuilder INT_ARRAY = new BinaryCodec(
                (u, c, r, v, d) ->
                        c.add("$L.i32Array($L)", r, v),
                (u, c, r, d) ->
                        c.add("$L.i32Array()", r)
        );
        BinaryCodecBuilder CHAR_ARRAY = new BinaryCodec(
                (u, c, r, v, d) ->
                        c.add("$L.characterArray($L)", r, v),
                (u, c, r, d) ->
                        c.add("$L.characterArray()", r)
        );
        BinaryCodecBuilder LONG_ARRAY = new BinaryCodec(
                (u, c, r, v, d) ->
                        c.add("$L.i64Array($L)", r, v),
                (u, c, r, d) ->
                        c.add("$L.i64Array()", r)
        );
        BinaryCodecBuilder FLOAT_ARRAY = new BinaryCodec(
                (u, c, r, v, d) ->
                        c.add("$L.f32Array($L)", r, v),
                (u, c, r, d) ->
                        c.add("$L.f32Array()", r)
        );
        BinaryCodecBuilder DOUBLE_ARRAY = new BinaryCodec(
                (u, c, r, v, d) ->
                        c.add("$L.f64Array($L)", r, v),
                (u, c, r, d) ->
                        c.add("$L.f64Array()", r)
        );
        BinaryCodecBuilder BYTE_ARRAY = new BinaryCodec(
                (u, c, r, v, d) ->
                        c.add("$L.binary($L)", r, v),
                (u, c, r, d) ->
                        c.add("$L.binary()", r)
        );
        BiFunction<BinaryCodecBuilder, TypeMirror, BinaryCodecBuilder> GENERIC_ARRAY = (in, t) -> new BinaryCodec(
                (u, c, r, v, d) -> {
                    var bf = "b" + d;
                    var vf = "v" + d;
                    return c.add("$L.array($L,($L,$L)->$L)", r, v, bf, vf, in.write(u, CodeBlock.builder(), bf, vf, d+1).build());
                },

                (u, c, r, d) -> {
                    var bf = "b" + d;
                    return c.add("$L.array($T[]::new,$L->$L)", r, t, bf, in.read(u, CodeBlock.builder(), bf, d+1).build());
                }

        );
        BiFunction<TypeName, String, BinaryCodecBuilder> DATA_ENHANCED = (cn, f) -> new BinaryCodec(
                (u, c, r, v, d) ->
                        c.add("$T.$L.write($L,$L)", cn, f, r, v),
                (u, c, r, d) ->
                        c.add("$T.$L.read($L)", cn, f, r)
        );
        BiFunction<TypeName, Boolean, BinaryCodecBuilder> DATA = (type, def) -> new BinaryCodec(
                !def ? (u, c, r, v, d) ->
                        c.add("$T.binaryData($L,$L,null)", Codec.class, r, v)
                        : (u, c, r, v, d) ->
                        c.add("$T.binaryData($L,$L,$T.class)", Codec.class, r, v, type),
                !def ? (u, c, r, d) ->
                        c.add("$T.binaryData($L,null)", r)
                        : (u, c, r, d) ->
                        c.add("$T.binaryData($L,$T.class)", r, type)
        );

        Function<String, BinaryCodecBuilder> COMMON_NAMED = name -> new BinaryCodec(
                (u, c, r, v, d) ->
                        c.add("$T.$L.write($L,$L)", CommonCodec.class, name, r, v),
                (u, c, r, d) ->
                        c.add("$T.$L.read($L)", CommonCodec.class, name, r)
        );
        Function<String, BinaryCodecBuilder> CODEC_NAMED = name -> new BinaryCodec(
                (u, c, r, v, d) ->
                        c.add("$T.$L.write($L,$L)", Codec.class, name, r, v),
                (u, c, r, d) ->
                        c.add("$T.$L.read($L)", Codec.class, name, r)
        );
        BinaryCodecBuilder UUID = COMMON_NAMED.apply("UUID_BINARY");
        BinaryCodecBuilder PERIOD = COMMON_NAMED.apply("JAVA_TIME__PERIOD_BINARY");
        BinaryCodecBuilder DURATION = COMMON_NAMED.apply("JAVA_TIME__DURATION_BINARY");
        BinaryCodecBuilder JSON_OBJECT = CODEC_NAMED.apply("JSON_OBJECT_BINARY");
        BinaryCodecBuilder JSON_ARRAY = CODEC_NAMED.apply("JSON_ARRAY_BINARY");
        BinaryCodecBuilder INSTANT = CODEC_NAMED.apply("INSTANT_BINARY");
        BinaryCodecBuilder BIG_DECIMAL = COMMON_NAMED.apply("JAVA_MATH__BIG_DECIMAL_BINARY");
        BinaryCodecBuilder DATETIME_TZ = COMMON_NAMED.apply("JAVA_TIME__OFFSET_DATE_TIME_BINARY");
        BinaryCodecBuilder TIME_TZ = COMMON_NAMED.apply("JAVA_TIME__OFFSET_TIME_BINARY");
        BinaryCodecBuilder TIME = COMMON_NAMED.apply("JAVA_TIME__LOCAL_TIME_BINARY");
        BinaryCodecBuilder DATE = COMMON_NAMED.apply("JAVA_TIME__LOCAL_DATE_BINARY");
        BinaryCodecBuilder DATETIME = COMMON_NAMED.apply("JAVA_TIME__LOCAL_DATE_TIME_BINARY");
        BinaryCodecBuilder STRING = CODEC_NAMED.apply("STRING_BINARY");
        BinaryCodecBuilder BUFFER = CODEC_NAMED.apply("BUFFER_BINARY");
        BinaryCodecBuilder NUMERIC = new BinaryCodec(
                (u, c, r, v, d) ->
                        c.add("$L.string($L==null?null:$L.toString())",  r, v, v),
                (u, c, r, d) ->
                        c.add("$T.ofNullable($L.string()).map($T::parse).orElse(null)", Optional.class, r,Numeric.class)
        );
        BinaryCodecBuilder CLASS = COMMON_NAMED.apply("CLASS_BINARY");
        BinaryCodecBuilder I_TIME = COMMON_NAMED.apply("I_TIME_BINARY");
        BinaryCodecBuilder I_DATE = COMMON_NAMED.apply("I_DATE_BINARY");
        BinaryCodecBuilder I_DATETIME = COMMON_NAMED.apply("I_DATETIME_BINARY");
        BinaryCodecBuilder ENTRY = COMMON_NAMED.apply("ENTRY_BINARY");

        BinaryCodecBuilder BOOLEAN = CODEC_NAMED.apply("BOOLEAN_BINARY");
        BinaryCodecBuilder BYTE = CODEC_NAMED.apply("BYTE_BINARY");
        BinaryCodecBuilder SHORT = CODEC_NAMED.apply("SHORT_BINARY");
        BinaryCodecBuilder INT = CODEC_NAMED.apply("INT_BINARY");
        BinaryCodecBuilder LONG = CODEC_NAMED.apply("LONG_BINARY");
        BinaryCodecBuilder CHAR = CODEC_NAMED.apply("CHAR_BINARY");
        BinaryCodecBuilder FLOAT = CODEC_NAMED.apply("FLOAT_BINARY");
        BinaryCodecBuilder DOUBLE = CODEC_NAMED.apply("DOUBLE_BINARY");

        BinaryCodecBuilder BOOLEAN_OBJECT = CODEC_NAMED.apply("BOOLEAN_OBJECT_BINARY");
        BinaryCodecBuilder BYTE_OBJECT = CODEC_NAMED.apply("BYTE_OBJECT_BINARY");
        BinaryCodecBuilder SHORT_OBJECT = CODEC_NAMED.apply("SHORT_OBJECT_BINARY");
        BinaryCodecBuilder INT_OBJECT = CODEC_NAMED.apply("INT_OBJECT_BINARY");
        BinaryCodecBuilder LONG_OBJECT = CODEC_NAMED.apply("LONG_OBJECT_BINARY");
        BinaryCodecBuilder CHAR_OBJECT = CODEC_NAMED.apply("CHAR_OBJECT_BINARY");
        BinaryCodecBuilder FLOAT_OBJECT = CODEC_NAMED.apply("FLOAT_OBJECT_BINARY");
        BinaryCodecBuilder DOUBLE_OBJECT = CODEC_NAMED.apply("DOUBLE_OBJECT_BINARY");

        Function<TypeName, BinaryCodecBuilder> ENUM = name -> new BinaryCodec(
                (u, c, r, v, d) ->
                        c.add("$T.binaryEnum($L,$L)", Codec.class, r, v),
                (u, c, r, d) ->
                        c.add("$T.binaryEnum($L,$T.class)", Codec.class, r, name)
        );
        Function<TypeName, BinaryCodecBuilder> ENUM_TEXT = name -> new BinaryCodec(
                (u, c, r, v, d) ->
                        c.add("$T.binaryEnumText($L,$L)", Codec.class, r, v),
                (u, c, r, d) ->
                        c.add("$T.binaryEnumText($L,$T.class)", Codec.class, r, name)
        );
        BiFunction<BinaryCodecBuilder, TypeName, BinaryCodecBuilder> GENERIC_COLLECTION = (in, container) ->
                new BinaryCodec(
                        (u, c, r, v, d) ->
                        {
                            var bf = "b" + d;
                            var vf = "v" + d;
                            return c.add("$L.repeat($L,($L,$L)->$L)", r, v, vf, bf, in.write(u, CodeBlock.builder(), bf, vf, d+1).build());
                        },
                        (u, c, r, d) ->
                        {
                            var bf = "b" + d;
                            return c.add("$L.repeat($T::new,$L->$L)", r, container, bf, in.read(u, CodeBlock.builder(), bf, d+1).build());
                        }
                );
        Function<BinaryCodecBuilder, BinaryCodecBuilder> OPTIONAL = (in) ->
                new BinaryCodec(
                        (u, c, r, v, d) ->
                        {
                            var bf = "b" + d;
                            var vf = "v" + d;
                            return c.add("$T.binaryOption($L,$L,($L,$L)->$L)", Codec.class, r, v, vf, bf, in.write(u, CodeBlock.builder(), vf, bf, d+1).build());
                        },
                        (u, c, r, d) ->
                        {
                            var bf = "b" + d;
                            return c.add("$T.binaryOption($L,$L->$L)", Codec.class, r, bf, in.read(u, CodeBlock.builder(), bf, d+1).build());
                        }
                );
        Function3<BinaryCodecBuilder, BinaryCodecBuilder, ClassName, BinaryCodecBuilder> GENERIC_MAP = (key, val, container) ->
                new BinaryCodec(
                        (u, c, r, v, d) ->
                        {
                            var bf = "b" + d;
                            var kf = "k" + d;
                            var vf = "v" + d;
                            return c.add("$L.map($L,($L,$L)->$L,($L,$L)->$L)", r, v, bf, kf, key.write(u, CodeBlock.builder(), bf, kf, d+1).build(), vf, bf, val.write(u, CodeBlock.builder(), bf, vf, d + 1).build());
                        },
                        (u, c, r, d) ->
                        {
                            var bf = "b" + d;
                            return c.add("$L.map($L::new,$L->$L,$L->$L)", r, container, bf, key.read(u, CodeBlock.builder(), bf, d + 1).build(), bf, val.read(u, CodeBlock.builder(), bf, d + 1).build());
                        }
                );

        static BinaryCodecBuilder lookup(Element where, Context ctx, TypeMirror type) {
            return switch (type.getKind()) {
                case BOOLEAN -> BOOLEAN;
                case BYTE -> BYTE;
                case SHORT -> SHORT;
                case INT -> INT;
                case LONG -> LONG;
                case CHAR -> CHAR;
                case FLOAT -> FLOAT;
                case DOUBLE -> DOUBLE;
                case ARRAY -> {
                    var ct = ((ArrayType) type).getComponentType();
                    yield switch (ct.getKind()) {
                        case BOOLEAN -> BOOLEAN_ARRAY;
                        case BYTE -> BYTE_ARRAY;
                        case SHORT -> SHORT_ARRAY;
                        case INT -> INT_ARRAY;
                        case LONG -> LONG_ARRAY;
                        case CHAR -> CHAR_ARRAY;
                        case FLOAT -> FLOAT_ARRAY;
                        case DOUBLE -> DOUBLE_ARRAY;
                        case ARRAY, DECLARED -> GENERIC_ARRAY.apply(lookup(where, ctx, ct), ct);
                        default -> {
                            var f = PROVIDERS.lookup(where, ctx, type);
                            if (f != null) yield f;
                            throw new IllegalStateException("unsupported array to json type: " + type + " at " + where);
                        }
                    };
                }
                case DECLARED -> {
                    var dt = (DeclaredType) type;
                    if (ctx.assignableTo(dt, Data.class)) {
                        var de = dt.asElement();
                        if (de.getKind().isInterface() && AnnotatedValue.find(de, Enhance.class).isPresent()){
                            var codec=ClassName.get(ctx.packageOf(de).toString(), Context.isInternal(de)?Domain.CODECS_INTERNAL_NAME:Domain.CODECS_NAME);
                            yield DATA_ENHANCED.apply(codec, CaseConv.PASCAL_UPPER_SNAKE.apply(de.getSimpleName().toString()) + Domain.CODEC_BINARY_SUFFIX);
                        }
                        else if (de.getKind().isInterface())
                            yield DATA.apply(null, true);
                        else
                            yield DATA.apply(TypeName.get(dt), false);
                    }
                    yield switch (dt.getTypeArguments().size()) {
                        //simple
                        case 0 -> {
                            var unbox = ctx.unbox(type);
                            if (unbox.isPresent()) {
                                yield switch (unbox.get().getKind()) {
                                    case BOOLEAN -> BOOLEAN_OBJECT;
                                    case BYTE -> BYTE_OBJECT;
                                    case SHORT -> SHORT_OBJECT;
                                    case INT -> INT_OBJECT;
                                    case LONG -> LONG_OBJECT;
                                    case CHAR -> CHAR_OBJECT;
                                    case FLOAT -> FLOAT_OBJECT;
                                    case DOUBLE -> DOUBLE_OBJECT;
                                    default -> throw new IllegalStateException("invalid boxed type: " + type);
                                };
                            }
                            if (ctx.sameType(dt, ITimes.ITime.class)) yield I_TIME;
                            if (ctx.sameType(dt, ITimes.IDate.class)) yield I_DATE;
                            if (ctx.sameType(dt, ITimes.IDatetime.class)) yield I_DATETIME;
                            if (ctx.sameType(dt, Entity.Entry.class)) yield ENTRY;

                            if (ctx.sameType(dt, UUID.class)) yield UUID;
                            if (ctx.sameType(dt, Period.class)) yield PERIOD;
                            if (ctx.sameType(dt, Duration.class)) yield DURATION;
                            if (ctx.sameType(dt, JsonObject.class)) yield JSON_OBJECT;
                            if (ctx.sameType(dt, JsonArray.class)) yield JSON_ARRAY;
                            if (ctx.sameType(dt, Instant.class)) yield INSTANT;
                            if (ctx.sameType(dt, BigDecimal.class)) yield BIG_DECIMAL;
                            if (ctx.sameType(dt, Numeric.class)) yield NUMERIC;
                            if (ctx.sameType(dt, OffsetDateTime.class)) yield DATETIME_TZ;
                            if (ctx.sameType(dt, OffsetTime.class)) yield TIME_TZ;
                            if (ctx.sameType(dt, LocalTime.class)) yield TIME;
                            if (ctx.sameType(dt, LocalDate.class)) yield DATE;
                            if (ctx.sameType(dt, LocalDateTime.class)) yield DATETIME;
                            if (ctx.sameType(dt, String.class)) yield STRING;
                            if (ctx.sameType(dt, Buffer.class)) yield BUFFER;
                            if (ctx.rawAssignableTo(dt, Enum.class)) yield ENUM.apply(TypeName.get(dt));
                            throw new IllegalStateException(
                                    "unsupported declared data type: " + type + " at " + where);
                        }
                        //!! list like
                        case 1 -> {
                            if (ctx.rawSameType(dt, Class.class)) {
                                yield CLASS;
                            }
                            if (ctx.assignableTo(ctx.erasure(dt), Collection.class)) {
                                yield GENERIC_COLLECTION.apply(lookup(where, ctx, dt.getTypeArguments().getFirst()),
                                        switch (dt.asElement().getSimpleName().toString()) {
                                            case "Set" -> ClassName.get(HashSet.class);
                                            case "List" -> ClassName.get(ArrayList.class);
                                            default -> ClassName.get(LinkedList.class);
                                        });
                            }
                            if (ctx.assignableTo(ctx.erasure(dt), Optional.class)) {
                                yield OPTIONAL.apply(lookup(where, ctx, dt.getTypeArguments().getFirst()));
                            }
                            var f = PROVIDERS.lookup(where, ctx, type);
                            if (f != null) yield f;
                            throw new IllegalStateException(
                                    "unsupported one parameter generic data type: " + type + " at " + where);
                        }
                        //!! map like
                        case 2 -> {
                            if (ctx.assignableTo(ctx.erasure(dt), Map.class)) {
                                yield GENERIC_MAP.apply(lookup(where, ctx, dt.getTypeArguments().get(0)),
                                        lookup(where, ctx, dt.getTypeArguments().get(1)),
                                        ClassName.get(HashMap.class));
                            }
                            var f = PROVIDERS.lookup(where, ctx, type);
                            if (f != null) yield f;
                            throw new IllegalStateException(
                                    "unsupported two parameters generic data type: " + type + " at " + where);
                        }
                        default -> {
                            var f = PROVIDERS.lookup(where, ctx, type);
                            if (f != null) yield f;
                            throw new IllegalStateException("unsupported generic data type: " + type + " at " + where);
                        }
                    };
                }
                default -> {
                    var f = PROVIDERS.lookup(where, ctx, type);
                    if (f != null) yield f;
                    throw new IllegalStateException("unsupported type to json data type: " + type + " at " + where);
                }
            };

        }
    }

}

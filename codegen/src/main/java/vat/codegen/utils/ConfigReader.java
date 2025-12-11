package vat.codegen.utils;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import javax.lang.model.element.Element;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.math.BigDecimal;
import java.time.Instant;

///
/// @author Zen.Liu
/// @since 2025-11-05


public interface ConfigReader {

    static String lookup(Element where, Context ctx, TypeMirror type) {
      return   switch (type.getKind()) {
            case BOOLEAN,
                 BYTE,
                 SHORT,
                 LONG,
                 CHAR,
                 FLOAT,
                 DOUBLE -> "get" + CaseConv.captionWord(type.getKind().name());
            case INT -> "getInteger";
            case ARRAY -> {
                var at = (ArrayType) type;
                var et = at.getComponentType();
                if (et.getKind() == TypeKind.BYTE) {
                    yield "getBinary";
                }
                throw new IllegalStateException("unsupported ARRAY config type:" + type+" "+where);
            }
            case DECLARED -> {
                var pt = ctx.unbox(type);
                if (pt.isPresent()) {
                    yield switch (pt.get().getKind()) {
                        case BOOLEAN,
                             BYTE,
                             SHORT,
                             LONG,
                             CHAR,
                             FLOAT,
                             DOUBLE -> "get" + CaseConv.captionWord(type.getKind().name());
                        case INT -> "getInteger";
                        default ->
                                throw new IllegalStateException("unsupported Boxed config type:" + type+" "+where);
                    };
                }
                if (ctx.sameType(type, String.class)) {
                    yield "getString";
                }
                if (ctx.sameType(type, BigDecimal.class)) yield "getDecimal";
                if (ctx.sameType(type, Buffer.class)) yield "getBuffer";
                if (ctx.sameType(type, Instant.class)) yield "getInstant";
                if (ctx.sameType(type, JsonObject.class)) yield "getObject";
                if (ctx.sameType(type, JsonArray.class)) yield "getArray";
                throw new IllegalStateException("unsupported DECLARED config type:" + type+" "+where);
            }
            default -> throw new IllegalStateException("unsupported config type:" + type+" "+where);
        };
    }


}

package vat.codegen.utils;

import com.palantir.javapoet.TypeName;
import lombok.With;
import vat.api.implement.Codec;

import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

///
/// @author Zen.Liu
/// @since 2025-12-03
public record CodecInfo(
        TypeName holder,
        AnnotatedConstruct type,
        String name,
        @With String binaryName,
        @With String jsName
) {
    static final Map<String, CodecInfo> CODECS = new ConcurrentHashMap<>();

    public static Map<String, CodecInfo> loadCodecs(Context ctx) {
        if (!CODECS.isEmpty()) return new HashMap<>(CODECS);
        var types = ServiceLoader.load(Codec.Provider.class, ctx.getClass().getClassLoader())
                .stream().map(ServiceLoader.Provider::type).distinct().toList();
        var codecs = new HashMap<>(types.stream()
                .flatMap(x -> Arrays.stream(x.getDeclaredFields()))
                .distinct()
                .filter(x -> java.lang.reflect.Modifier.isStatic(x.getModifiers())
                             && (
                                     Codec.DataCodec.class.isAssignableFrom(x.getType())
                                     || Codec.JsDecoder.class.isAssignableFrom(x.getType())
                                     || Codec.DataProperty.class.isAssignableFrom(x.getType())
                                     || Codec.BinaryProperty.class.isAssignableFrom(x.getType())
                             )
                )
                .map(x -> new CodecInfo(
                        TypeName.get(x.getDeclaringClass()),
                        typeLoad(ctx, x.getGenericType()),
                        trimName(x.getName()),
                        x.getName().endsWith(Domain.CODEC_BINARY_SUFFIX) ? x.getName() : null,
                        x.getName().endsWith(Domain.CODEC_JS_SUFFIX) ? x.getName() : null
                ))
                .collect(Collectors.toMap(x -> x.name, Function.identity(), (m, m1) -> {
                    if (m1.binaryName != null && m.binaryName == null) return m.withBinaryName(m1.binaryName);
                    if (m.binaryName != null && m1.binaryName == null) return m1.withBinaryName(m.binaryName);
                    if (m1.jsName != null && m.jsName == null) return m.withJsName(m1.jsName);
                    if (m.jsName != null && m1.jsName == null) return m1.withJsName(m.jsName);
                    throw new IllegalStateException("can merge " + m + " and " + m1);
                })));
        CODECS.putAll(codecs);
        return new HashMap<>(codecs);
    }

    private static String trimName(String name) {
        if (name.endsWith(Domain.CODEC_BINARY_SUFFIX)) return
                name.substring(0, name.length() - Domain.CODEC_BINARY_SUFFIX.length());
        if (name.endsWith(Domain.CODEC_JS_SUFFIX))
            return name.substring(0, name.length() - Domain.CODEC_JS_SUFFIX.length());
        return name;
    }

    /// make sure always create a AnnotatedConstruct
    private static AnnotatedConstruct processTypeArgument(Context ctx, Type type) {
        // Handle array types
        if (type instanceof GenericArrayType arrayType) {
            Type componentType = arrayType.getGenericComponentType();
            var componentCt = processTypeArgument(ctx, componentType);
            return ctx.types().getArrayType(((TypeElement) componentCt).asType());
        }

        // Handle raw array types (like int[], String[], etc.)
        if (type instanceof Class<?> clazz && clazz.isArray()) {
            Class<?> componentType = clazz.getComponentType();
            var componentCt = processTypeArgument(ctx, componentType);
            return ctx.types()
                    .getArrayType(componentCt instanceof TypeElement t ? t.asType() : (TypeMirror) componentCt);
        }

        // Handle primitive types
        if (type instanceof Class<?> clazz && clazz.isPrimitive()) {
            return ctx.types().getPrimitiveType(switch (clazz.getName()) {
                case "boolean" -> TypeKind.BOOLEAN;
                case "byte" -> TypeKind.BYTE;
                case "short" -> TypeKind.SHORT;
                case "int" -> TypeKind.INT;
                case "long" -> TypeKind.LONG;
                case "char" -> TypeKind.CHAR;
                case "float" -> TypeKind.FLOAT;
                case "double" -> TypeKind.DOUBLE;
                case "void" -> TypeKind.VOID;
                default -> throw new IllegalArgumentException("Unknown primitive type: " + clazz.getName());
            });
        }

        // Handle regular types
        var ct = ctx.typeElement(type);
        assert ct != null : type + " not a type element";
        return ct;
    }

    private static AnnotatedConstruct typeLoad(Context ctx, Type genericType) {
        if (genericType instanceof ParameterizedType p) {
            var t = p.getActualTypeArguments()[0];
            return processTypeArgument(ctx, t);
        }
        throw new IllegalArgumentException("should be generic type with one parameter: " + genericType);
    }
}

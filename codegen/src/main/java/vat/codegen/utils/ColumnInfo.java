package vat.codegen.utils;

import com.palantir.javapoet.CodeBlock;
import lombok.With;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

///
/// @author Zen.Liu
/// @since 2025-12-03
public record ColumnInfo(
        ResolvedField field,
        @With int index,
        ColumnType kind,
        String property,
        String name,
        TypeMirror type,
        boolean nullable,
        boolean enumName,
        boolean enumType,
        int size,
        int max,
        int min,
        int precision,
        int scale,
        List<String> indexed,
        List<String> unique,
        CodecBuilder.FieldCodecBuilder codec,
        CodeBlock interceptor
) implements Comparable<ColumnInfo> {
    public boolean hasAnyIndex() {
        return (indexed != null) || (unique != null);
    }

    public boolean hasIndex() {
        return (indexed != null);
    }

    public boolean hasUnique() {
        return (unique != null);
    }

    public String identityString() {
        return "%s#%s".formatted(field.getter(), type);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    ColumnInfo(
            ResolvedField field,
            int index,
            ColumnType kind,
            String property,
            String name,
            TypeMirror type,
            boolean nullable,
            boolean enumName,
            boolean enumType,
            Optional<AnnotatedValue> anno,
            CodecBuilder.FieldCodecBuilder codec,
            CodeBlock interceptor
    ) {
        this(field, index, kind, property, name, type, nullable, enumName, enumType,
                anno.flatMap(x -> x.getInteger("size")).orElse(-1),
                anno.flatMap(x -> x.getInteger("max")).orElse(-1),
                anno.flatMap(x -> x.getInteger("min")).orElse(-1),
                anno.flatMap(x -> x.getInteger("precision")).orElse(-1),
                anno.flatMap(x -> x.getInteger("scale")).orElse(-1),
                anno.flatMap(x -> x.getList("indexed", String.class))
                        .map(x -> {
                            if (!x.contains(name)) {
                                x = new ArrayList<>(x);
                                x.add(name);
                            }
                            return x;
                        }).orElse(null),
                anno.flatMap(x -> x.getList("unique", String.class))
                        .map(x -> {
                            if (!x.contains(name)) {
                                x = new ArrayList<>(x);
                                x.add(name);
                            }
                            return x;
                        }).orElse(null),
                codec,
                interceptor
        );
    }

    @Override
    public int compareTo(@NotNull ColumnInfo o) {
        var ko = kind.compareTo(o.kind);
        if (ko == 0) return index - o.index;
        return ko;
    }
}

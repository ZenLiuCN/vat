package vat.codegen.utils;

import javax.lang.model.element.Element;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.Objects;
import java.util.Optional;

///
/// @author Zen.Liu
/// @since 2025-10-27
interface TypeTool extends LoggingTool, ElementTool {
    Types types();


    default TypeMirror erasure(TypeMirror type) {
        return types().erasure(type);
    }

    /// t1 can assign to t2
    default boolean assignableTo(TypeMirror t1, TypeMirror t2) {
        return types().isAssignable(t1, t2);
    }

    default boolean rawAssignableTo(TypeMirror t1, TypeMirror t2) {
        return types().isAssignable(erasure(t1), erasure(t2));
    }

    default boolean rawAssignableTo(TypeMirror t1, Class<?> t2) {
        return types().isAssignable(erasure(t1), erasure(type(t2)));
    }

    default boolean assignableTo(TypeMirror t1, Class<?> t2) {
        return types().isAssignable(t1, type(t2));
    }

    default boolean sameType(TypeMirror t1, TypeMirror t2) {
        return types().isSameType(t1, t2);
    }

    default boolean sameType(TypeMirror t1, Class<?> t2) {
        return types().isSameType(t1, type(t2));
    }

    default boolean rawSameType(TypeMirror type, Class<?> clazz) {
        return rawSameType(type, type(clazz));
    }

    default boolean rawSameType(TypeMirror type, TypeMirror type2) {
        return types().isSameType(erasure(type), erasure(type2));
    }

    @Override
    default Element asElement(TypeMirror t) {
        return types().asElement(t);
    }

    @Override
    default TypeMirror type(Class<?> clazz) {
        Objects.requireNonNull(clazz, "Class parameter cannot be null");
        if (clazz == void.class) {
            return types().getNoType(TypeKind.VOID);
        } else if (clazz.isPrimitive()) {
            return types().getPrimitiveType(getTypeKind(clazz));
        } else {
            String canonicalName = clazz.getCanonicalName();
            return elements().getTypeElement(canonicalName).asType();
        }
    }

    // Helper method to convert Class<?> to TypeKind
    private TypeKind getTypeKind(Class<?> clazz) {
        if (clazz == byte.class) return TypeKind.BYTE;
        if (clazz == short.class) return TypeKind.SHORT;
        if (clazz == int.class) return TypeKind.INT;
        if (clazz == long.class) return TypeKind.LONG;
        if (clazz == char.class) return TypeKind.CHAR;
        if (clazz == float.class) return TypeKind.FLOAT;
        if (clazz == double.class) return TypeKind.DOUBLE;
        if (clazz == boolean.class) return TypeKind.BOOLEAN;
        throw new IllegalArgumentException("Not a primitive type: " + clazz);
    }

    default boolean subTypeOf(TypeMirror t1, TypeMirror t2) {
        return types().isSubtype(t1, t2);
    }

    default Optional<PrimitiveType> unbox(TypeMirror type) {
        try {
            return Optional.of(types().unboxedType(type));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    default boolean isVoid(TypeMirror mirror) {
        return mirror.getKind() == TypeKind.VOID;
    }
    default boolean isAnyVoid(TypeMirror mirror) {
        return mirror.getKind() == TypeKind.VOID||sameType(mirror,Void.class);
    }
}

package vat.codegen.utils;

import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;

///
/// @author Zen.Liu
/// @since 2025-12-03
public interface OverridesMethod<T extends OverridesMethod<T>> extends Comparable<T> {
    Context ctx();

    T _this();

    ExecutableElement method();

    int index();

    T withIndex(int index);

    List<ExecutableElement> overrides();

    default TypeMirror resolvedType(Element own) {
        return ctx().resolve(method().getReturnType(), own, method());
    }

    default String signature() {
        return ctx().methodSignature(method());
    }

    default T withOverrides(ExecutableElement method) {
        overrides().add(method);
        return _this();
    }

    default Optional<AnnotatedValue> annotationValues(Class<? extends Annotation> type) {
        var di = AnnotatedValue.of(method(), type);
        if (di.isPresent()) return di;
        var sorted = ctx().overrideSort(overrides());
        for (ExecutableElement override : sorted) {
            di = AnnotatedValue.of(override, type);
            if (di.isPresent()) return di;
        }
        return Optional.empty();
    }

    default boolean annotation(Class<? extends Annotation> type) {
        if (AnnotatedValue.find(method(), type).isPresent()) return true;
        var sorted = ctx().overrideSort(overrides());
        for (var override : sorted) {
            if (AnnotatedValue.find(override, type).isPresent()) return true;
        }
        return false;
    }

    @Override
    default int compareTo(@NotNull T o) {

        return 0;
    }
}

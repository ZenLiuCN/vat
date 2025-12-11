package vat.codegen.utils;

import lombok.With;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

///
/// @author Zen.Liu
/// @since 2025-12-03
public record ExecutableMethod(
        Context ctx,
        @With int index,
        ExecutableElement method,
        List<ExecutableElement> overrides,
        AtomicReference<TypeMirror> resolved
) implements OverridesMethod<ExecutableMethod> {
    public ExecutableMethod(Context ctx, int index, ExecutableElement method) {
        this(ctx, index, method, new ArrayList<>(), new AtomicReference<>());
    }

    @Override
    public TypeMirror resolvedType(Element own) {
        if (resolved.get() != null) return resolved.get();
        var res = OverridesMethod.super.resolvedType(own);
        resolved.set(res);
        return res;
    }

    @Override
    public ExecutableMethod _this() {
        return this;
    }
}

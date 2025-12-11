package vat.codegen.utils;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import lombok.With;
import vat.api.implement.Interceptors;
import vat.api.implement.Validators;
import vat.api.meta.Alias;
import vat.api.meta.Intercept;
import vat.api.meta.Validate;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;

///
/// @author Zen.Liu
/// @since 2025-12-03
public record GetterField(
        Context ctx,
        @With int index,
        @With String name,
        AliasInfo alias,
        TypeMirror type,
        ExecutableElement getter,
        List<ExecutableElement> overrides) implements OverridesMethod<GetterField> {
    public GetterField(Context ctx, int index, ExecutableElement getter) {
        this(ctx, index,
                Context.getterToField(getter.getSimpleName(), getter.getReturnType()),
                alias(getter),
                getter.getReturnType(), getter,
                new ArrayList<>());
    }


    static AliasInfo alias(ExecutableElement getter) {
        return AnnotatedValue.of(getter, Alias.class)
                .map(v -> new AliasInfo(
                        v.getString("value").orElseThrow(() -> new IllegalStateException("@Alias value required: " + getter))
                        , v.getBoolean("strict").orElse(true)))
                .orElse(null);
    }

    /// Preferred write name
    public String prefer() {
        return alias == null ? name : alias.alias();
    }

    public List<Validator> validators() {
        return AnnotatedValue.of(getter, Validate.class, Validate.List.class)
                .map(a -> a.stream()
                        .map(x -> new Validator(
                                x.getType("holder")
                                        .map(TypeName::get)
                                        .orElseGet(() -> ClassName.get(Validators.class))
                                ,
                                x.getString("value").orElseThrow(),
                                x.getBoolean("construct").orElse(false))
                        ).toList())
                .orElseGet(List::of);
    }

    public List<Interceptor> interceptors() {
        return AnnotatedValue.of(getter, Intercept.class, Intercept.List.class)
                .map(a -> a.stream()
                        .map(x -> new Interceptor(
                                x.getType("holder")
                                        .map(TypeName::get)
                                        .orElseGet(() -> ClassName.get(Interceptors.class))
                                ,
                                x.getString("value").orElseThrow(),
                                x.getBoolean("construct").orElse(false))
                        ).toList())
                .orElseGet(List::of);
    }


    @Override
    public GetterField _this() {
        return this;
    }

    @Override
    public ExecutableElement method() {
        return getter;
    }
}

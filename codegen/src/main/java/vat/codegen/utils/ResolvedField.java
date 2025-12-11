package vat.codegen.utils;

import com.palantir.javapoet.CodeBlock;
import lombok.With;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;

///
/// @author Zen.Liu
/// @since 2025-12-03
public record ResolvedField(
        Context ctx,
        @With int index,
        boolean resolved,
        String name,
        AliasInfo alias,
        TypeMirror type,
        ExecutableElement getter,
        List<ExecutableElement> overrides,
        CodecInfo codec,
        @Nullable String vField,
        @Nullable String vKey,
        boolean required,
        boolean opt,
        CodeBlock prefer,
        boolean toJs,
        boolean enumName,
        TypeMirror rawType,
        List<Validator> validators,
        List<Interceptor> interceptors
)
        implements OverridesMethod<ResolvedField> {

    public ResolvedField(GetterField f, CodecInfo codec, boolean required, boolean opt, CodeBlock prefer, boolean toJs, boolean enumName,TypeMirror rawType) {
        this(f.ctx(), f.index(),false, f.name(), f.alias(), f.type(), f.getter(), f.overrides(), codec, null, null,required, opt, prefer, toJs, enumName,rawType,
                f.validators(), f.interceptors());
    }

    public ResolvedField(TypeMirror resolve, GetterField f, CodecInfo codec,boolean required, boolean opt, CodeBlock prefer, boolean toJs, boolean enumName,TypeMirror rawType) {
        this(f.ctx(), f.index(),true, f.name(), f.alias(), resolve, f.getter(), f.overrides(), codec, null, null,required, opt, prefer, toJs, enumName,rawType,
                f.validators(), f.interceptors());
    }

    public ResolvedField(GetterField f, CodecInfo codec, String vField, String vKey,boolean required, boolean opt, CodeBlock prefer, boolean toJs, boolean enumName,TypeMirror rawType) {
        this(f.ctx(), f.index(),false, f.name(), f.alias(), f.type(), f.getter(), f.overrides(), codec, vField, vKey, required,opt, prefer, toJs, enumName,rawType,
                f.validators(), f.interceptors());
    }

    public ResolvedField(TypeMirror resolve, GetterField f, CodecInfo codec, String vField, String vKey, boolean required,boolean opt, CodeBlock prefer, boolean toJs, boolean enumName,TypeMirror rawType) {
        this(f.ctx(), f.index(), true,f.name(), f.alias(), resolve, f.getter(), f.overrides(), codec, vField, vKey,required, opt, prefer, toJs, enumName,rawType,
                f.validators(), f.interceptors());
    }

    public String preferName() {
        return alias == null ? name : alias.alias();
    }

    @Override
    public ResolvedField _this() {
        return this;
    }

    @Override
    public ExecutableElement method() {
        return getter;
    }


    public List<Validator> constructValidators() {
        return validators.stream().filter(Validator::construct).toList();
    }

    public List<Interceptor> constructInterceptors() {
        return interceptors.stream().filter(Interceptor::construct).toList();
    }

}

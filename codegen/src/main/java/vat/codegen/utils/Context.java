package vat.codegen.utils;


import io.vertx.core.Future;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import vat.api.Data;
import vat.api.Event;
import vat.api.Store;
import vat.api.meta.Enhance;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static vat.api.implement.Codec.any;

///
/// @author Zen.Liu
/// @since 2025-10-26

public record Context(
        ProcessingEnvironment env,
        RoundEnvironment roundEnv,
        Messager messager,
        Elements elements,
        Filer filer,
        Types types,
        AtomicReference<Path> root
) implements LoggingTool, TypeTool, ElementTool, FileTool, CaseConv {
    @Override
    public @NotNull String toString() {
        return "CONTEXT";
    }

    public Context(
            ProcessingEnvironment env,
            RoundEnvironment roundEnv
    ) {
        this(env, roundEnv, env.getMessager(), env.getElementUtils(), env.getFiler(), env.getTypeUtils(), new AtomicReference<>());
    }

    public static boolean isInternal(Element de) {
        return
                AnnotatedValue.of(de, Enhance.class).flatMap(x -> x.getBoolean("internal")).orElse(false)
                || Optional.ofNullable(de.getEnclosingElement()).map(x -> x.getSimpleName().contentEquals("Internal")).orElse(false)
                || AnnotatedValue.find(de, ApiStatus.Internal.class).isPresent()
                ;
    }

    public boolean isBinary(TypeElement ele) {
        return isBinary(ele.asType());
    }

    public boolean isBinary(TypeMirror ele) {
        return rawAssignableTo(ele, Data.Binary.class);
    }

    @Override
    public Path path() {
        return root.get();
    }

    @Override
    public void path(Path path) {
        root.set(path);
    }


    public boolean isVertxFuture(TypeMirror mirror) {
        return mirror instanceof DeclaredType d && sameType(erasure(d), erasure(type(Future.class)));
    }

    public TypeMirror validateSubscriberSignature(ExecutableElement method) {
        if (
                method.isDefault()
                && isVoid(method.getReturnType())
                && method.getParameters().size() == 1
                && rawAssignableTo(method.getParameters().getFirst().asType(), Event.class)
        ) return method.getParameters().getFirst().asType();
        throw new IllegalStateException("invalid subscriber method: " + method);
    }

    public TypeMirror validatePublisherSignature(ExecutableElement method) {
        if (
                method.isDefault()
                && isVoid(method.getReturnType())
                && method.getParameters().size() == 1
                && eventConsumer(method.getParameters().getFirst().asType())
        ) {
            var p = ((DeclaredType) method.getParameters().getFirst().asType()).getTypeArguments().getFirst();
            if (p instanceof WildcardType t)
                return t.getSuperBound();
            return p;
        }
        throw new IllegalStateException("invalid publisher method: " + method);
    }

    private boolean eventConsumer(TypeMirror type) {
        if (!rawAssignableTo(type, type(Consumer.class))) {
            return false;
        }
        var p = ((DeclaredType) type).getTypeArguments().getFirst();
        if (p instanceof DeclaredType e) {
            return rawAssignableTo(e, type(Event.class));
        }
        if (p instanceof WildcardType e) {
            var sb = e.getSuperBound();
            return sb != null && rawAssignableTo(sb, type(Event.class));
        }
        return false;
    }

    public TypeMirror futureContent(TypeMirror mirror) {
        if (mirror instanceof DeclaredType d && sameType(erasure(d), erasure(type(Future.class)))) {
            return d.getTypeArguments().getFirst();
        }
        return null;
    }


    public TypeMirror functionParameter0(VariableElement element) {
        var t = element.asType();
        if (rawAssignableTo(t, type(Function.class))) {
            return ((DeclaredType) t).getTypeArguments().getFirst();
        }
        return null;
    }

    public TypeMirror functionParameter1(VariableElement element) {
        var t = element.asType();
        if (rawAssignableTo(t, type(Function.class))) {
            return ((DeclaredType) t).getTypeArguments().get(1);
        }
        return null;
    }

    public TypeMirror storeParameter(TypeMirror type) {
        if (rawAssignableTo(type, type(Store.class))) {
            return ((DeclaredType) type).getTypeArguments().getFirst();
        }
        return null;
    }

    public TypeMirror maybeOptional(TypeMirror t) {
        if (rawAssignableTo(t, type(Optional.class))) {
            return ((DeclaredType) t).getTypeArguments().getFirst();
        }
        return null;
    }

    public TypeMirror orOptional(TypeMirror t) {
        if (rawAssignableTo(t, type(Optional.class))) {
            return ((DeclaredType) t).getTypeArguments().getFirst();
        }
        return t;
    }

    public String configReader(Element where, TypeMirror type) {
        return ConfigReader.lookup(where, this, type);
    }

    public TypeMirror boxed(TypeMirror t) {
        if (t.getKind().isPrimitive()) return types().boxedClass(((PrimitiveType) t)).asType();
        return t;
    }

    public boolean sameEntity(TypeMirror storeEntity, TypeMirror methodRet) {
        if (methodRet == null || storeEntity == null) return false;
        var opt = orOptional(methodRet);
        if (opt == null) opt = methodRet;
        return rawSameType(storeEntity, opt);
    }




    List<ExecutableElement> overrideSort(List<ExecutableElement> overrides) {
        if (overrides == null || overrides.isEmpty()) {
            return new ArrayList<>();
        }
        return overrides.stream()
                .sorted(Comparator.comparing(x -> new InheritedCompare(x.getEnclosingElement().asType(), this)))
                .collect(Collectors.toList());
    }

    /// ! only resolve for interfaces chain and current method.
    TypeMirror resolve(TypeMirror type, Element own, ExecutableElement mth) {
        if (type.getKind() != TypeKind.TYPEVAR) return type;
        var typeVar = (TypeVariable) type;
        //current method
        var resolved = resolveFromTypeParameters(typeVar, mth.getTypeParameters());
        if (resolved != null) return resolved;
        // current type
        if (own.getKind() == ElementKind.CLASS || own.getKind() == ElementKind.INTERFACE) {
            resolved = resolveFromTypeParameters(typeVar, ((TypeElement) own).getTypeParameters());
            if (resolved != null) return resolved;
        }
        // lookup for interface
        for (var face : ((TypeElement) own).getInterfaces()) {
            if (face.getKind() == TypeKind.DECLARED) {
                resolved = resolveFromSupertype(typeVar, (DeclaredType) face);
                if (resolved != null) return resolved;
            }
        }
        // check boundary
        return typeVar.getUpperBound();
    }

    TypeMirror resolveFromTypeParameters(TypeVariable typeVar, List<? extends TypeParameterElement> typeParams) {
        for (var typeParam : typeParams) {
            if (typeParam.getSimpleName().equals(typeVar.asElement().getSimpleName())) {
                if (!typeParam.getBounds().isEmpty()) {//! TODO
                    return typeParam.getBounds().getFirst();
                }
                return null;
            }
        }
        return null;
    }

    TypeMirror resolveFromSupertype(TypeVariable typeVar, DeclaredType supertype) {
        var subtypeParams = ((TypeElement) supertype.asElement()).getTypeParameters();
        var supertypeArgs = supertype.getTypeArguments();
        var index = -1;
        for (var i = 0; i < subtypeParams.size(); i++) {
            if (subtypeParams.get(i).getSimpleName().equals(typeVar.asElement().getSimpleName())) {
                index = i;
                break;
            }
        }
        if (index >= 0 && index < supertypeArgs.size()) {
            return supertypeArgs.get(index);
        }

        return null;
    }

    public String methodSignature(ExecutableElement method) {
        return method.getSimpleName() + "=(" + method.getParameters()
                .stream()
                .map(x -> signatureName(method, x.asType()))
                .collect(Collectors.joining(",")) + ")=>"
               + signatureName(method, method.getReturnType());
    }


    public ExecutableMethod toMethod(int index, ExecutableElement executableElement) {
        return new ExecutableMethod(this, index, executableElement);
    }

    public GetterField getterToField(int index, ExecutableElement executableElement) {
        return new GetterField(this, index, executableElement);
    }


    static public String getterToField(CharSequence methodName, TypeMirror ret) {
        var name = methodName.toString();
        if (
                name.startsWith("get")
                && name.length() > 3
                && Character.isUpperCase(name.charAt(3))
        ) return PASCAL_CAMEL.apply(name.substring(3));
        if (
                name.startsWith("is")
                && ret.getKind() == TypeKind.BOOLEAN
                && name.length() > 2
                && Character.isUpperCase(name.charAt(2))
        ) return PASCAL_CAMEL.apply(name.substring(2));
        return name;
    }



    ///  format style:
    /// 1. PRIMITIVE_NAME = NAME$$
    /// 2. QUALIFIED_NAME = PACKAGE_PACKAGE__CLASS_NAME
    /// 3. TYPE_NAME = PRIMITIVE_NAME|QUALIFIED_NAME
    /// 3. GENERIC_NAME = TYPE_NAME_$$QUALIFIED_NAME_$$QUALIFIED_NAME...
    /// 4. ARRAY_NAM E= TYPE_NAME_$$ARRAY
    ///
    /// Trimmed packages:
    /// 1. java.lang
    /// 2. java.util
    /// 3. io.vertx.core.json
    /// 4. io.vertx.core.buffer
    public static String simpleCodecNameOf(PackageElement pkgElement, TypeElement fieldType) {
        var pkg = pkgElement.getQualifiedName().toString();
        if (any(pkg,
                "java.lang",
                "java.util",
                "io.vertx.core.buffer",
                "io.vertx.core.json",
                "vat.api.utils"
        )) {
            return PASCAL_UPPER_SNAKE.apply(fieldType.getSimpleName().toString());
        }
        return CODEC_NAME.apply(pkg) + "__" + PASCAL_UPPER_SNAKE.apply(fieldType.getSimpleName().toString());
    }

    public String codecName(TypeMirror type) {
        if (type.getKind() == TypeKind.DECLARED) {
            var fieldType = typeElementOf(type);
            assert fieldType != null : "missing field type of " + type;
            if (rawSameType(type, Class.class)) {
                return simpleCodecNameOf(packageOf(fieldType), fieldType);
            }
            if (fieldType.getTypeParameters().isEmpty()) {
                return simpleCodecNameOf(packageOf(fieldType), fieldType);
            }
            var raw = new StringBuilder(simpleCodecNameOf(packageOf(fieldType), fieldType));
            for (var typeArgument : ((DeclaredType) type).getTypeArguments()) {
                raw.append("_$$").append(codecName(typeArgument));
            }
            return raw.toString();
        } else if (type.getKind() == TypeKind.ARRAY) {
            var in = ((ArrayType) type).getComponentType();
            return codecName(in) + "_$ARRAY";
        } else if (type.getKind().isPrimitive()) {
            return type.getKind().name() + "$$";
        } else throw new IllegalArgumentException("Unsupported type: " + type.getKind() + " " + type);
    }

    public String signatureName(Element who, TypeMirror type) {
        if (type.getKind() == TypeKind.DECLARED) {
            var fieldType = typeElementOf(type);
            assert fieldType != null : "missing field type of " + type+ " of "+who;
            if (fieldType.getTypeParameters().isEmpty()) {
                return simpleCodecNameOf(packageOf(fieldType), fieldType);
            }
            var raw = new StringBuilder(simpleCodecNameOf(packageOf(fieldType), fieldType));
            for (var typeArgument : ((DeclaredType) type).getTypeArguments()) {
                raw.append("_$$").append(signatureName(who, typeArgument));
            }
            return raw.toString();
        } else if (type.getKind() == TypeKind.ARRAY) {
            var in = ((ArrayType) type).getComponentType();
            return signatureName(who, in) + "_$ARRAY";
        } else if (type.getKind().isPrimitive()) {
            return type.getKind().name() + "$$";
        } else if (type.getKind() == TypeKind.VOID) {
            return type.getKind().name() + "$$";
        } else if (type.getKind() == TypeKind.WILDCARD) {
            var w = ((WildcardType) type);
            var e = Optional.ofNullable(w.getExtendsBound()).map(x -> signatureName(who, x)).orElse("_$");
            var s = Optional.ofNullable(w.getSuperBound()).map(x -> signatureName(who, x)).orElse("_$");
            return "$x" + e + s + "_$$";
        } else if (type.getKind() == TypeKind.TYPEVAR) {
            var w = ((TypeVariable) type);
            var ta = lookupTypeParameter(who, w);
            return (ta == null ? w.asElement().getSimpleName().toString() : signatureName(who, ta)) + "_$$";
        } else
            throw new IllegalArgumentException("Unsupported type: " + type.getKind()+ " of "+who);
    }

    private TypeMirror lookupTypeParameter(Element who, TypeVariable w) {
        //! TODO
        var n = w.asElement().getSimpleName().toString();
        return null;
    }


    public <T extends OverridesMethod<T>> T preferOverride(@NotNull List<T> getterFields) {
        if (getterFields.size() == 1) return getterFields.getFirst();
        T find = null;
        TypeMirror defined = null;
        for (var f : getterFields) {
            var def = f.method().getEnclosingElement().asType();
            if (defined == null) {
                find = f;
                defined = def;
            } else if (assignableTo(def, defined)) {
                find = f.withIndex(find.index()).withOverrides(find.method());
                defined = def;
            } else {
                find = find.withIndex(f.index()).withOverrides(find.method());
            }
        }
        return find;
    }




    public boolean nullable(AnnotatedConstruct ele) {
        return nullableAny(ele);
    }

    public boolean nullableAny(AnnotatedConstruct ele) {
        return ele.getAnnotation(vat.api.meta.Nullable.class) != null
               || ele.getAnnotation(Nullable.class) != null
               || ele.getAnnotationMirrors().stream().anyMatch(x -> x.getAnnotationType().asElement().getSimpleName().contentEquals("Nullable"));
    }

    public <T> Optional<T> genericInfo(TypeMirror mirror, Class<?> type, Function<List<? extends TypeMirror>, T> map) {
        return Optional.of(mirror)
                .filter(x -> rawAssignableTo(x, type))
                .map(DeclaredType.class::cast)
                .map(DeclaredType::getTypeArguments)
                .map(map);
    }

    public Function<TypeMirror, TypeMirror> voidReplace(TypeMirror replace) {
        return t -> isVoid(t) ? replace : t;
    }
}

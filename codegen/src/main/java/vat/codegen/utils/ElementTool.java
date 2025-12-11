package vat.codegen.utils;

import com.palantir.javapoet.TypeName;
import org.jetbrains.annotations.Nullable;
import vat.api.Prototype;

import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

///
/// @author Zen.Liu
/// @since 2025-10-27


public interface ElementTool extends LoggingTool {
    Elements elements();

    TypeMirror type(Class<?> clazz);

    Element asElement(TypeMirror t);

    default Element element(Class<?> type) {
        Objects.requireNonNull(type, "Class parameter cannot be null");
        var t = type(type);
        if (t.getKind() == TypeKind.DECLARED) {
            // Handle reference types (String, Object, etc.)
            String canonicalName = type.getCanonicalName();
            if (canonicalName == null) {
                throw new IllegalArgumentException(
                        "Class has no canonical name (possibly anonymous/local class): " + type);
            }

            TypeElement typeElement = elements().getTypeElement(canonicalName);
            if (typeElement == null) {
                throw new IllegalArgumentException(
                        "Class not found in processing environment: " + type);
            }

            return typeElement;
        }
        return asElement(t);

    }


    default TypeElement typeElement(Class<?> type) {
        Objects.requireNonNull(type, "Class parameter cannot be null");

        if (type.isPrimitive() || type == void.class) {
            throw new IllegalArgumentException(
                    "Cannot load TypeElement for primitive or void type: " + type);
        }
        if (type.isArray()) {
            throw new IllegalArgumentException(
                    "Cannot load TypeElement for array type (use getComponentType() first): " + type);
        }

        String canonicalName = type.getCanonicalName();
        if (canonicalName == null) {
            throw new IllegalArgumentException(
                    "Class has no canonical name (possibly anonymous/local class): " + type);
        }

        TypeElement element = elements().getTypeElement(canonicalName);
        if (element == null) {
            throw new IllegalArgumentException(
                    "No TypeElement found for class (not in the current processing environment): " + type);
        }
        return element;
    }

    private String getClassName(Type type) {
        if (type instanceof Class<?> clazz) {
            if (clazz.isArray()) {
                return getClassName(clazz.getComponentType()) + "[]";
            }
            return clazz.getCanonicalName();
        } else if (type instanceof ParameterizedType p) {
            return getClassName(p.getRawType());
        } else if (type instanceof WildcardType || type instanceof TypeVariable<?>) {
            // Handle wildcards and type variables (may return null or a fallback)
            return null;
        } else {
            // Other cases (GenericArrayType, etc.)
            return type.getTypeName(); // Fallback, may not work for all cases
        }
    }

    default TypeElement typeElement(Type type) {
        if (type == null) {
            throw new NullPointerException("Type cannot be null");
        }

        var className = getClassName(type);
        if (className == null) {
            throw new IllegalArgumentException("Could not resolve class name for type: " + type);
        }
        return elements().getTypeElement(className);
    }

    default PackageElement packageOf(Element element) {
        return elements().getPackageOf(element);
    }

    default @Nullable TypeElement typeElementOf(TypeMirror mirror) {
        if (mirror.getKind() == TypeKind.DECLARED) {
            return (TypeElement) ((DeclaredType) mirror).asElement();
        }
        return null;
    }


    default AnnotatedConstruct find(TypeName type, String field) {
        var te = elements().getTypeElement(type.toString());
        if (te == null) return null;
        return te.getEnclosedElements().stream()
                .filter(x -> x.getKind() == ElementKind.FIELD)
                .filter(x ->
                        x.getModifiers().contains(Modifier.STATIC)
                        && x.getModifiers().contains(Modifier.PUBLIC)
                        && x.getSimpleName().contentEquals(field)
                ).findFirst().orElse(null);
    }


    default Set<TypeElement> interfaces(TypeElement e) {
        if (e == null) return Set.of();
        var face = e.getInterfaces()
                .stream()
                .map(this::typeElementOf)
                .filter(Objects::nonNull)
                .filter(Predicate.not(IS_PROTOTYPE))
                .distinct()
                .toList();
        var as = face.stream()
                .flatMap(x -> interfaces(x).stream())
                .filter(Objects::nonNull)
                .filter(Predicate.not(IS_PROTOTYPE))
                .distinct()
                .toList();
        var all = new HashSet<>(face);
        all.addAll(as);
        return all;
    }


    static Predicate<AnnotatedConstruct> hasAnnotation(Class<? extends Annotation> e) {
        return x -> {
            var a = x.getAnnotationsByType(e);
            return a != null && a.length > 0;
        };
    }

    Predicate<AnnotatedConstruct> IS_PROTOTYPE = hasAnnotation(Prototype.class);

    default Stream<ExecutableElement> methods(TypeElement element) {
        return element.getEnclosedElements().stream()
                .filter(x -> x.getKind() == ElementKind.METHOD)
                .map(x -> (ExecutableElement) x);


    }

    default Stream<VariableElement> fields(TypeElement element) {
        return element.getEnclosedElements().stream()
                .filter(x -> x.getKind() == ElementKind.FIELD)
                .map(x -> (VariableElement) x);

    }
    default Optional<VariableElement> functorField(TypeElement element,String name) {
        return fields(element)
                .filter(x ->
                        x.getModifiers().contains(Modifier.STATIC)
                       && x.getModifiers().contains(Modifier.FINAL)
                        && x.getModifiers().contains(Modifier.PUBLIC)
                        && x.getSimpleName().contentEquals(name)
                )
                .findFirst();

    }

    default boolean notObjectClass(TypeMirror receiverType) {
        var obj = element(Object.class).asType();
        return receiverType != obj;
    }

    default boolean isStatic(Element x) {
        return x.getModifiers().contains(Modifier.STATIC);
    }

    default boolean isPublic(Element x) {
        return x.getModifiers().contains(Modifier.PUBLIC);
    }

    default boolean isDefault(Element x) {
        return x.getModifiers().contains(Modifier.DEFAULT);
    }

    default Predicate<ExecutableElement> parameter(int i) {
        return x -> x.getParameters().size() == i;
    }


    default Predicate<ExecutableElement> returnType(Predicate<TypeMirror> wanna) {
        return x -> wanna.test(x.getReturnType());
    }

    default boolean noParameters(ExecutableElement x) {
        return x.getParameters().isEmpty();
    }

    default boolean isField(Element element) {
        return element.getKind() == ElementKind.FIELD;
    }


}

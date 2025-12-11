package vat.codegen.utils;

import lombok.experimental.Delegate;

import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

///
/// @author Zen.Liu
/// @since 2025-12-03
public record AnnotatedValue(
        @Delegate Map<String, Object> value,
        AnnotationMirror mirror,
        AnnotatedConstruct holder)
        implements Map<String, Object> {
    public <T> Optional<T> get(String key, Class<T> clazz) {
        return Optional.ofNullable(value.get(key))
                .filter(clazz::isInstance)
                .map(clazz::cast);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<List<T>> getList(String key, Class<T> clazz) {
        return Optional.ofNullable(value.get(key))
                .filter(List.class::isInstance)
                .map(List.class::cast)
                .map(x -> ((List<AnnotationValue>) x).stream()
                        .map(AnnotationValue::getValue)
                        .filter(clazz::isInstance)
                        .map(clazz::cast).toList())
                ;
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<List<T>> getList(String key, Function<AnnotationValue, T> inner) {
        return Optional.ofNullable(value.get(key))
                .filter(List.class::isInstance)
                .map(List.class::cast)
                .map(x -> ((List<AnnotationValue>) x).stream()
                        .map(inner)
                        .toList())
                ;
    }

    @SuppressWarnings("unchecked")
    public Optional<List<AnnotatedValue>> getAnnotationList(String key) {
        return Optional.ofNullable(value.get(key))
                .filter(List.class::isInstance)
                .map(List.class::cast)
                .map(x -> ((List<AnnotationValue>) x).stream()
                        .map(AnnotationValue::getValue)
                        .map(i -> i instanceof AnnotationMirror am ? am : null)
                        .filter(Objects::nonNull)
                        .map(am -> new AnnotatedValue(annotationValue(am), am, holder))
                        .toList())
                ;
    }
    public Optional<String> getString(String key) {
        return get(key, String.class);
    }
    public <T extends Enum<T>> Optional<T> getEnum(String key, Class<T> type) {
        return getEnum(key).map(x -> x.getSimpleName().toString())
                .filter(Predicate.not(String::isBlank))
                .map(s -> Enum.valueOf(type, s));
    }
    public Optional<VariableElement> getEnum(String key) {
        return get(key, VariableElement.class);
    }
    public Optional<Boolean> getBoolean(String key) {
        return get(key, Boolean.class);
    }
    public Optional<Integer> getInteger(String key) {
        return get(key, Number.class).map(Number::intValue);
    }
    public Optional<TypeMirror> getType(String key) {
        return get(key, TypeMirror.class);
    }

    /// for Repeatable container
    public Optional<List<AnnotatedValue>> getAnnotationList() {
        return getAnnotationList("value");
    }
    public Optional<String> getString() {
        return getString("value");
    }
    public Optional<VariableElement> getEnum() {
        return getEnum("value");
    }
    public <T extends Enum<T>> Optional<T> getEnum(Class<T> type) {
        return getEnum("value",type);
    }
    public <T> Optional<List<T>> getList(Class<T> clazz) {
        return getList("value",clazz);
    }
    public Optional<Boolean> getBoolean(){return getBoolean("value");}
    public Optional<Integer> getInteger(){return getInteger("value");}
    public Optional<TypeMirror> getType(){return getType("value");}




    public static Optional<AnnotationMirror> find(AnnotatedConstruct e, Class<? extends Annotation> type) {
        return e.getAnnotationMirrors()
                .stream()
                .filter(x ->
                        x.getAnnotationType().asElement() instanceof TypeElement xe && xe.getQualifiedName()
                                .contentEquals(
                                        type.getCanonicalName()))
                .findFirst()
                .map(x -> (AnnotationMirror) x);
    }

    public static Optional<AnnotatedValue> of(AnnotatedConstruct e, Class<? extends Annotation> type) {
        return find(e, type)
                .map(m -> new AnnotatedValue(annotationValue(m), m, e));
    }
    public static Optional<List<AnnotatedValue>> of(AnnotatedConstruct e, Class<? extends Annotation> type,Class<? extends Annotation> listType) {
        return of(e, type)
                .map(List::of)
                .or(()-> of(e,listType).flatMap(AnnotatedValue::getAnnotationList))
                ;
    }
    @SuppressWarnings("DataFlowIssue")
    public static Map<String, Object> annotationValue(AnnotationMirror e) {
        return e.getElementValues().entrySet().stream()
                .map(
                        x ->
                                Map.entry(x.getKey().getSimpleName().toString()
                                        , Optional
                                                .ofNullable(x.getValue())
                                                .map(AnnotationValue::getValue)
                                                .orElseGet(() -> Optional
                                                        .ofNullable(x.getKey().getDefaultValue())
                                                        .map(AnnotationValue::getValue)
                                                        .orElse(null))

                                )
                )
                .filter(x -> Objects.nonNull(x.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }




}

package vat.codegen.utils;

import com.palantir.javapoet.*;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Pool;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.With;
import org.jetbrains.annotations.ApiStatus;
import org.jooq.lambda.function.Function2;
import org.jooq.lambda.function.Function4;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import vat.api.Ability;
import vat.api.DomainError;
import vat.api.Entity;
import vat.api.implement.*;
import vat.api.meta.*;
import vat.api.store.Dialect;
import vat.api.trait.Accessor;
import vat.api.trait.Applicative;
import vat.api.utils.Fn;
import vat.api.utils.Pointer;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

///
/// @author Zen.Liu
/// @since 2025-12-03
@NullMarked
@SuppressWarnings({"UnusedReturnValue", "unused"})
public interface Builders {


    String name();

    ClassName type();

    TypeSpec.Builder spec();

    interface Json<T extends Json<T>> extends Builders {
        T _this();

        CodeBlock.@Nullable Builder asJson();

        default T asJson(Consumer<CodeBlock.Builder> build) {
            var j = asJson();
            if (j != null) build.accept(j);
            return _this();
        }
    }

    interface JS<T extends JS<T>> extends Builders {
        T _this();

        CodeBlock.@Nullable Builder toJs();

        CodeBlock.@Nullable Builder fromJs();

        default T asJs(boolean js, BiConsumer<CodeBlock.Builder, CodeBlock.Builder> build) {
            if (!js) return _this();
            var j = toJs();
            var f = fromJs();
            if (j != null && f != null) build.accept(j, f);
            return _this();
        }

        default T asJs(BiConsumer<CodeBlock.Builder, CodeBlock.Builder> build) {
            var j = toJs();
            var f = fromJs();
            if (j != null && f != null) build.accept(j, f);
            return _this();
        }
    }

    interface Bin<T extends Bin<T>> extends Builders {
        T _this();

        CodeBlock.@Nullable Builder toBin();

        CodeBlock.@Nullable Builder fromBin();


        default T asBin(boolean js, BiConsumer<CodeBlock.Builder, CodeBlock.Builder> build) {
            if (!js) return _this();
            var j = toBin();
            var f = fromBin();
            if (j != null && f != null) build.accept(j, f);
            return _this();
        }

        default T asBin(BiConsumer<CodeBlock.Builder, CodeBlock.Builder> build) {
            var j = toBin();
            var f = fromBin();
            if (j != null && f != null) build.accept(j, f);
            return _this();
        }
    }

    record Pojo(
            String name,
            ClassName type,
            TypeSpec.Builder spec,

            CodeBlock.Builder ctor,
            CodeBlock.Builder copy,
            CodeBlock.Builder computes,

            CodeBlock.Builder asJson,
            CodeBlock.Builder toJs,
            CodeBlock.Builder fromJs,
            CodeBlock.Builder toBin,
            CodeBlock.Builder fromBin
    ) implements Json<Pojo>, JS<Pojo>, Bin<Pojo>, Applicative<Pojo> {
        static Pojo of(Domain domain, TypeElement e, boolean internal) {
            var name = domain.pojoName(e);
            var type = domain.pojoTypeName(e);
            return new Pojo(
                    name, type,
                    Fn.Monad.<TypeSpec.Builder>operator()
                            .apply(internal, p -> p.addAnnotation(ApiStatus.Internal.class))
                            .apply(Domain.addDomainIdentity(e, TypeSpec.classBuilder(name))
                                    .addAnnotations(EqualStringNoSuper)
                                    .addModifiers(Modifier.PUBLIC)
                                    .superclass(ParameterizedTypeName.get(ClassName.get(vat.api.Data.DataObject.class), type))
                                    .addSuperinterface(TypeName.get(e.asType()))
                                    .addMethod(MethodSpec.methodBuilder("_this")
                                            .addAnnotation(Override.class)
                                            .addModifiers(Modifier.PUBLIC)
                                            .returns(type)
                                            .addStatement("return this")
                                            .build())),
                    CodeBlock.builder(),
                    CodeBlock.builder(),
                    CodeBlock.builder(),
                    CodeBlock.builder(),
                    CodeBlock.builder(),
                    CodeBlock.builder(),
                    CodeBlock.builder(),
                    CodeBlock.builder()
            );
        }

        @Override
        public Pojo _this() {
            return this;
        }

        public void flush(Domain domain) {
            domain.ctx().save(domain.pkg(), spec.build());
        }
    }

    static Data data(Domain domain, TypeElement e) {
        var enhance = AnnotatedValue.of(e, Enhance.class).orElseThrow(() -> DomainError.System.conflict("missing Enhance annotation"));
        boolean request = domain.ctx().rawAssignableTo(e.asType(), vat.api.Data.Request.class);
        boolean validation = domain.ctx().rawAssignableTo(e.asType(), vat.api.Data.Validation.class);
        boolean binary = domain.ctx().rawAssignableTo(e.asType(), vat.api.Data.Binary.class);
        boolean internal = Context.isInternal(e);
        boolean withPojo = enhance.getBoolean("pojo").orElse(false);
        boolean record = enhance.getBoolean("record").orElse(true);
        var dataJsonName = record ? "asJson" : "json";
        var dataName = domain.dataName(e);
        var dataType = domain.dataTypeName(e);
        return new Data(
                e,
                domain,
                request,
                validation,
                binary,
                internal,
                record,

                dataName,
                dataType,
                Fn.Monad.<TypeSpec.Builder>operator()
                        .apply(internal, t -> t.addAnnotation(ApiStatus.Internal.class))
                        .apply(Domain.addDomainIdentity(e,
                                        (record
                                                ? TypeSpec.recordBuilder(dataName)
                                                .addModifiers(Modifier.PUBLIC)
                                                .addSuperinterface(ParameterizedTypeName.get(
                                                        ClassName.get(Applicative.class), dataType))
                                                .recordConstructor(MethodSpec.constructorBuilder()
                                                        .addParameter(JsonObject.class, "asJson").build())
                                                : TypeSpec.classBuilder(dataName)
                                                .addAnnotations(EqualStringSuper)
                                                .addModifiers(Modifier.PUBLIC)
                                                .superclass(ParameterizedTypeName.get(
                                                        ClassName.get(vat.api.Data.DataJson.class), dataType))
                                        )
                                                .addMethod(MethodSpec.methodBuilder("_this")
                                                        .addAnnotation(Override.class)
                                                        .addModifiers(Modifier.PUBLIC)
                                                        .returns(dataType)
                                                        .addStatement("return this")
                                                        .build())

                                )
                                .addSuperinterface(TypeName.get(e.asType()))),

                dataJsonName,
                new AtomicInteger(-1),
                new ArrayList<>(),
                withPojo ? Pojo.of(domain, e, internal) : null,
                CodeBlock.builder(),
                CodeBlock.builder(),
                CodeBlock.builder(),

                CodeBlock.builder(),
                CodeBlock.builder(),
                CodeBlock.builder(),
                CodeBlock.builder(),
                CodeBlock.builder(),
                Stream.concat(domain.ctx().interfaces(e).stream(), Stream.of(e)).toList(),
                extractCopier(e),
                new AtomicInteger(-2)
        );
    }

    //region  AnnotationSpec
    List<AnnotationSpec> EqualStringNoSuper = List.of(
            AnnotationSpec.builder(EqualsAndHashCode.class)
                    .addMember("callSuper", "false")
                    .build(),
            AnnotationSpec.builder(ToString.class)
                    .addMember("callSuper", "false")
                    .addMember("doNotUseGetters", "true")
                    .build());
    List<AnnotationSpec> EqualString = List.of(
            AnnotationSpec.builder(EqualsAndHashCode.class)
                    .build(),
            AnnotationSpec.builder(ToString.class)
                    .addMember("doNotUseGetters", "true")
                    .build());
    List<AnnotationSpec> EqualStringSuper = List.of(
            AnnotationSpec.builder(EqualsAndHashCode.class)
                    .addMember("callSuper", "true")
                    .build(),
            AnnotationSpec.builder(ToString.class)
                    .addMember("callSuper", "true")
                    .addMember("doNotUseGetters", "true")
                    .build());

    //endregion


    record Data(
            TypeElement e,
            Domain domain,

            boolean request,
            boolean validation,
            boolean binary,
            boolean internal,
            boolean record,

            String name,
            ClassName type,
            TypeSpec.Builder spec,

            String jsonName,
            AtomicInteger index,
            ArrayList<ResolvedField> resolved,

            @Nullable Pojo pojo,

            CodeBlock.Builder ctor,
            CodeBlock.Builder copy,
            CodeBlock.Builder computes,

            CodeBlock.Builder asJson,
            CodeBlock.Builder toJs,
            CodeBlock.Builder fromJs,
            CodeBlock.Builder toBin,
            CodeBlock.Builder fromBin,
            List<TypeElement> faces,
            Map<String, CopyInfo> copier,
            AtomicInteger propertySize
    ) implements Json<Data>, JS<Data>, Bin<Data>, Applicative<Data>, Flushable {
        @Override
        public String toString() {
            return "" + e;
        }

        public void flush(Domain domain) {
            if (pojo != null) pojo.flush(domain);
            domain.ctx().save(domain.pkg(), spec.build());
        }

        @Override
        public Data _this() {
            return this;
        }

        public Data pojo(Consumer<Pojo> act) {
            if (pojo != null) act.accept(pojo);
            return this;
        }

        public Stream<ExecutableElement> allMethods() {
            return faces.stream().flatMap(domain.ctx()::methods);
        }

        public Stream<ResolvedField> definedProperties() {
            return switch (propertySize.get()) {
                case -2 -> {
                    Builders.extractPropertyField(this);
                    propertySize.set(-1);
                    yield resolved.stream();
                }
                case -1 -> resolved.stream();
                default -> resolved.stream().limit(propertySize.get());
            };
        }

        public Stream<ResolvedField> computedProperties() {
            return switch (propertySize.get()) {
                case -2 -> {
                    Builders.extractPropertyField(this);
                    propertySize.set(resolved.size());
                    yield extractComputedProperties(this).stream();
                }
                case -1 -> {
                    propertySize.set(resolved.size());
                    yield extractComputedProperties(this).stream();
                }
                default -> resolved.stream().skip(propertySize.get());
            };
        }


        public Data definedProperties(Consumer<Stream<ResolvedField>> act) {
            act.accept(definedProperties());
            return this;
        }

        public Data computedProperties(Consumer<Stream<ResolvedField>> act) {
            act.accept(computedProperties());
            return this;
        }

        public Data computeCopier(ResolvedField f) {
            if (copier.isEmpty()) return this;
            Builders.copier(domain.ctx(), e, f, copier);
            return this;
        }
    }

    Function<Context, Predicate<ExecutableElement>> SKIP_VALIDATIONS = ctx ->
            x -> !(x.getSimpleName().contentEquals("applyValidateFuture")
                   && ctx.isVertxFuture(x.getReturnType()))
                 && !(x.getSimpleName().contentEquals("applyValidate"));

    private static void extractPropertyField(Data data) {
        var e = data.e;
        var domain = data.domain;
        var ctx = domain.ctx();
        var prd = data.request() || data.validation() ? SKIP_VALIDATIONS.apply(ctx) : Fn.<ExecutableElement>truePredicate();
        data.allMethods()
                .filter(x -> ctx.notObjectClass(x.getReceiverType()))
                .filter(Predicate.not(ctx::isStatic))
                .filter(Predicate.not(ctx::isDefault))
                .filter(ctx::noParameters)
                .filter(ctx.returnType(Predicate.not(ctx::isVoid)))
                //* Data.Request field.
                .filter(prd)
                .map(x -> ctx.getterToField(data.index().incrementAndGet(), x))
                .collect(Collectors.groupingBy(GetterField::signature))
                .values()
                .stream()
                .map(ctx::preferOverride)
                .sorted(Comparator.comparingInt(GetterField::index))
                .peek(x -> {
                    var t = x.resolvedType(e);
                    if (ctx.isVertxFuture(t))
                        throw new IllegalStateException("data object can't have Future property: " + x);
                })
                .map(f -> Domain.resolveField(f, data.e, data.domain, data.binary))
                .forEach(data.resolved()::add);
    }


    private static List<ResolvedField> extractComputedProperties(Data data) {
        var domain = data.domain;
        var ctx = domain.ctx();
        return data.allMethods()
                .filter(x -> ctx.notObjectClass(x.getReceiverType()))
                .filter(Predicate.not(ctx::isStatic))
                .filter(ctx::isDefault)
                .filter(ctx::isPublic)
                .filter(ctx::noParameters)
                .filter(ctx.returnType(Predicate.not(ctx::isVoid)))
                .map(x -> ctx.getterToField(data.index().incrementAndGet(), x))
                .collect(Collectors.groupingBy(GetterField::signature))
                .values()
                .stream()
                .map(ctx::preferOverride)
                .filter(x -> x.annotation(Computed.class))
                .sorted(Comparator.comparingInt(GetterField::index))
                .map(f -> {
                    var resolved = Domain.resolveField(f, data.e, data.domain, data.binary);
                    var superType = data.e.asType();
                    var rawType = resolved.rawType();
                    var jsonProperty = data.jsonName;
                    var jsonKey = resolved.preferName();
                    var opt = resolved.opt();
                    var holder = resolved.codec().holder();
                    var field = resolved.codec().name();
                    var superName = resolved.method().getSimpleName();
                    var isJs = resolved.toJs();
                    var isResolved = resolved.resolved();
                    data.pojo(p -> p
                                    .asJson(j -> j.addStatement("$T.$L.set(json,$S,$T.super.$L()$L)"
                                            , holder, field, jsonKey, superType, superName, opt ? ".orElse(null)" : ""))
                                    .asJs(isJs, (to, from) -> {
                                        to.addStatement("$T.$L(js,$T::getLong,$S,$T.super.$L)",
                                                Codec.class, opt ? "toJsOpt" : "toJs",
                                                JsonObject.class, jsonKey,
                                                superType, superName);
                                        from.addStatement("$T.$L(js,$T::getString,$T::parseLong,$L)",
                                                Codec.class, opt ? "fromJsOpt" : "fromJs",
                                                JsonObject.class, Long.class, jsonKey,
                                                superType, superName);
                                    }))
                            .asJs(isJs, (to, from) -> {
                                to.addStatement("$T.$L(js,$T::getLong,$S,$T.super.$L)",
                                        Codec.class,
                                        opt ? "toJsOpt" : "toJs",
                                        JsonObject.class,
                                        jsonKey,
                                        superType,
                                        superName);
                                from.addStatement("$T.$L(js,$T::getString,$T::parseLong,$L)",
                                        Codec.class,
                                        opt ? "fromJsOpt" : "fromJs",
                                        JsonObject.class,
                                        Long.class,
                                        jsonKey,
                                        superType,
                                        superName);
                            })
                            .spec().addMethod(
                                    (isResolved
                                            ? MethodSpec.methodBuilder(f.getter().getSimpleName().toString())
                                            .addAnnotation(Override.class)
                                            .returns(TypeName.get(resolved.type()))
                                            : MethodSpec.overriding(f.getter()))
                                            .addModifiers(Modifier.PUBLIC)
                                            .addCode(opt
                                                    ? CodeBlock.builder().addStatement("return this.$L.containsKey($S)? $T.ofNullable($T.$L.get(this.$L,$S)):$T.super.$L()"
                                                    , jsonProperty, jsonKey, Optional.class
                                                    , holder, field, jsonProperty, jsonKey,
                                                    superType, superName).build()
                                                    : CodeBlock.builder().addStatement("return this.$L.containsKey($S)? $T.$L.get(this.$L,$S):$T.super.$L()"
                                                    , jsonProperty, jsonKey
                                                    , holder, field, jsonProperty, jsonKey,
                                                    superType, superName).build()
                                            )
                                            .build())
                            .addMethod(MethodSpec.methodBuilder(superName.toString())
                                    .addModifiers(Modifier.PUBLIC)
                                    .addParameter(opt
                                            ? ParameterSpec.builder(TypeName.get(rawType), "v").addAnnotation(Nullable.class).build()
                                            : ParameterSpec.builder(TypeName.get(rawType), "v").build())
                                    .returns(data.type())
                                    .addCode(CodeBlock.builder()
                                            .addStatement("$T.$L.set(this.$L,$S,v)",
                                                    holder, field,
                                                    jsonProperty, jsonKey)
                                            .addStatement("return this")
                                            .build())
                                    .build());
                    data.resolved().add(resolved);
                    return resolved;
                })
                .collect(Collectors.toCollection(ArrayList::new));

    }

    //region Copier
    record CopyInfo(TypeMirror input, @Nullable String name, TypeElement own, Map<String, CopyProcInfo> values,
                    CodeBlock.Builder code) {
        public String methodName(Context ctx) {
            return name == null || name.isEmpty()
                    ? (Domain.COPY_PREFIX + ctx.asElement(input).getSimpleName())
                    : name;
        }
    }

    record CopyProcInfo(
            String strategy,
            boolean withDefault,
            String from,
            @Nullable FuncField provide,
            @Nullable FuncField validate,
            @Nullable FuncField convert
    ) {
    }

    static Map<String, CopyInfo> extractCopier(TypeElement e) {
        return AnnotatedValue.of(e, Copier.class, Copier.List.class)
                .map(c -> c.stream()
                        .map(x -> x.getType("value")
                                .map(v -> new CopyInfo(v,
                                        x.getString("name").orElse(""),
                                        e,
                                        new HashMap<>(),
                                        CodeBlock.builder()
                                ))
                                .orElse(null))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(CopyInfo::name, Function.identity(),
                                (a, b) -> {
                                    throw new IllegalStateException("duplicated copier define");
                                })))
                .<Map<String, CopyInfo>>map(HashMap::new)
                .orElse(Map.of());
    }

    static void copier(Context ctx, TypeElement e, ResolvedField rt, Map<String, CopyInfo> registered) {
        AnnotatedValue.of(rt.method(), Copier.Process.class, Copier.Process.List.class)
                .map(t -> t.stream().map(x -> {
                    var ti = x
                            .getList("holders", i -> ((TypeMirror) i.getValue()))
                            .map(l -> {
                                if (l.isEmpty()) return List.of(e.asType(), e.asType(), e.asType());
                                if (l.size() != 3)
                                    throw new IllegalStateException("should strict have 3 Holder:" + rt.method());
                                return l.stream().map(ctx.voidReplace(e.asType())).toList();
                            }).orElseGet(() -> List.of(e.asType(), e.asType(), e.asType()));
                    return new CopyProcInfo(
                            x.getString("strategy").orElse(""),
                            x.getBoolean("withDefault").orElse(false),
                            x.getString("from").orElse(rt.name()),
                            x.getString("provide").filter(Predicate.not(String::isBlank))
                                    .map(i -> new FuncField(ti.getFirst(), i))
                                    .map(i -> ctx.isVoid(i.holder()) ? i.withHolder(e.asType()) : i)
                                    .orElse(null),
                            x.getString("validate").filter(Predicate.not(String::isBlank))
                                    .map(i -> new FuncField(ti.get(1), i))
                                    .map(i -> ctx.isVoid(i.holder()) ? i.withHolder(e.asType()) : i)
                                    .orElse(null),
                            x.getString("convert").filter(Predicate.not(String::isBlank))
                                    .map(i -> new FuncField(ti.getLast(), i))
                                    .map(i -> ctx.isVoid(i.holder()) ? i.withHolder(e.asType()) : i)
                                    .orElse(null)
                    );
                }).toList())
                .ifPresent(s -> s.forEach(ci -> {
                    var group = registered.get(ci.strategy);
                    if (group == null)
                        throw new IllegalStateException("missing Copier alias of " + ci.strategy + " in " + registered);
                    var old = group.values.put(rt.preferName(), ci);
                    if (old != null)
                        throw new IllegalStateException("duplicated Copier process: " + rt.preferName() + " with " + ci + " and " + old);
                }));
    }
    //endregion


    record Store(
            String name,
            ClassName type,
            TypeName entity,
            @Nullable String schema,
            String table,
            TypeSpec.Builder spec,
            List<ColumnInfo> columns,
            Map<ColumnType, ColumnInfo> typedColumns
    ) implements Builders, Applicative<Store> {
        static Store of(Data data) {
            var name = data.domain.storeName(data.e);
            var cn = ClassName.get(data.domain.pkg(), name);
            var table = AnnotatedValue.of(data.e, Table.class)
                    .flatMap(AnnotatedValue::getString)
                    .orElseGet(() -> CaseConv.PASCAL_SNAKE.apply(data.e.getSimpleName().toString()));
            var entityType = TypeName.get(data.e.asType());
            var schema = table.contains(".") ? table.substring(0, table.lastIndexOf(".")) : null;
            if (schema != null) table = table.substring(table.lastIndexOf(".") + 1);
            return new Store(
                    name,
                    cn,
                    entityType,
                    schema,
                    table,
                    TypeSpec.classBuilder(name)
                            .addAnnotation(ApiStatus.Internal.class)
                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    ,
                    new ArrayList<>(),
                    new HashMap<>()
            );
        }

        @Override
        public Store _this() {
            return this;
        }

        public Stream<ColumnInfo> definedColumns(Data data) {
            if (!columns.isEmpty()) return columns.stream();
            var columns = new ArrayList<ColumnInfo>();
            var it = data.resolved.stream().filter(x -> x.vField() == null).iterator();
            var index = 0;
            var id = -1;
            var version = -1;
            var removed = -1;
            var creator = -1;
            var created = -1;
            var modifier = -1;
            var modified = -1;
            var history = -1;
            TypeName idType = null;
            var ctx = data.domain.ctx();
            while (it.hasNext()) {
                var f = it.next();
                var property = f.name();
                var type = f.rawType();
                var opt = f.opt();
                var nullable = opt || ctx.nullable(f.getter());
                var kind = ColumnType.NORMAL;
                //* specific column check
                {
                    if (f.annotation(Identity.class)) {
                        if (id > 0) throw new IllegalArgumentException("Duplicate identity field : " + f);
                        id = index;
                        idType = TypeName.get(type);
                        kind = ColumnType.IDENTITY;
                    } else if (f.annotation(OptimisticLock.class)) {
                        if (version > 0) throw new IllegalArgumentException("Duplicate OptimisticLock field : " + f);
                        version = index;
                        kind = ColumnType.VERSION;
                    } else if (f.annotation(SoftRemoved.class)) {
                        if (removed > 0) throw new IllegalArgumentException("Duplicate SoftRemoved field : " + f);
                        removed = index;
                        kind = ColumnType.REMOVED;
                    } else if (f.annotation(Audit.Creator.class)) {
                        if (creator > 0) throw new IllegalArgumentException("Duplicate Audit.Creator field : " + f);
                        creator = index;
                        kind = ColumnType.CREATOR;
                    } else if (f.annotation(Audit.Created.class)) {
                        if (created > 0) throw new IllegalArgumentException("Duplicate Audit.Created field : " + f);
                        created = index;
                        kind = ColumnType.CREATED;
                    } else if (f.annotation(Audit.Modifier.class)) {
                        if (modifier > 0) throw new IllegalArgumentException("Duplicate Audit.Modifier field : " + f);
                        modifier = index;
                        kind = ColumnType.MODIFIER;
                    } else if (f.annotation(Audit.Modified.class)) {
                        if (modified > 0) throw new IllegalArgumentException("Duplicate Audit.Modified field : " + f);
                        modified = index;
                        kind = ColumnType.MODIFIED;
                    } else if (f.annotation(Historic.class)) {
                        if (history > 0) throw new IllegalArgumentException("Duplicate Historic field : " + f);
                        history = index;
                        kind = ColumnType.HISTORY;
                    }
                }
                var fieldBuilder = data.domain.lookupField(data.e, type, f.getter());
                var col = f.annotationValues(Column.class);
                var column = col
                        .flatMap(x -> x.getString("value"))
                        .filter(x -> !x.isEmpty())
                        .orElseGet(() -> CaseConv.CAMEL_SNAKE.apply(property));
                var enumName = f.enumName();
                var enumType = ctx.rawAssignableTo(type, Enum.class);
                if (enumName && enumType) {
                    fieldBuilder = CodecBuilder.FieldCodecBuilder.ENUM_TEXT_FIELD.apply(TypeName.get(type));
                }
                var interceptor = col
                        .map(x -> Map.entry(x.getString("interceptField"), x.getType("interceptHolder")))
                        .filter(x -> x.getKey().isPresent())
                        .map(x -> Map.entry(x.getKey().orElseThrow(), x.getValue()))
                        .map(x -> {
                            TypeMirror holder;
                            if (x.getValue().isPresent()) holder = x.getValue().get();
                            else holder = null;
                            if (holder == null || ctx.sameType(holder, void.class)) {
                                holder = data.e.asType();
                            }
                            return CodeBlock.builder().add("$T.$L", holder, x.getKey()).build();
                        }).orElse(null);
                index++;
                columns.add(new ColumnInfo(f, index, kind, property, column, type, nullable, enumName, enumType, col, fieldBuilder, interceptor));
            }
            if (idType == null) throw new IllegalStateException("missing identity field");
            var idx = new AtomicInteger(-1);
            this.columns.addAll(columns
                    .stream()
                    .sorted(ColumnInfo::compareTo)
                    .map(f -> {
                        var x = f.withIndex(idx.incrementAndGet());
                        if (x.kind() != ColumnType.NORMAL) {
                            var old = typedColumns.put(x.kind(), x);
                            if (old != null) throw new IllegalStateException("duplicated specific column: " + x);
                        }
                        return x;
                    })
                    .toList());
            return this.columns.stream();
        }

        public int indexOf(ColumnType type) {
            if (typedColumns.isEmpty()) throw new IllegalStateException("typed column is empty");
            return typedColumns.containsKey(type) ? typedColumns.get(type).index() : -1;
        }

        public void flush(Domain domain) {
            domain.ctx().save(domain.pkg(), spec.build());
        }

    }

    record Storage(Store store, Data data) implements Applicative<Storage>, Flushable {
        @Override
        public Storage _this() {
            return this;
        }

        public Stream<ColumnInfo> definedColumns() {
            return store.definedColumns(data);
        }

        public void flush(Domain domain) {
            store.flush(domain);
            data.flush(domain);
        }
    }

    static Storage storage(Data data) {
        return new Storage(
                Store.of(data),
                data
        );
    }

    record Audited(String topic, Auditing.Mode mode) {
        public Audited(AnnotatedValue a, String aqn, ExecutableElement method) {
            this(a.getString("topic").orElseGet(() -> aqn + "::" + method.getSimpleName()),
                    a.getEnum("mode", Auditing.Mode.class).orElse(Auditing.Mode.FAILURE));
        }
    }

    record ActionInfo(
            @Nullable VariableElement argument,
            @Nullable TypeMirror inType,
            @Nullable String inName,
            TypeMirror outType,
            ExecutableElement method,

            CodecBuilder.@Nullable BufferCodecBuilder inCodec,
            CodecBuilder.BufferCodecBuilder outCodec,

            boolean inNullable,
            boolean outNullable,
            boolean outOpt,
            @Nullable Audited audit,
            boolean storeTrait
    ) {
    }

    record Activities(
            Domain domain,
            TypeElement e,
            String name,
            ClassName type,
            TypeSpec.Builder spec,
            List<TypeElement> faces,
            List<ActionInfo> actions
    ) implements Builders, Applicative<Activities>, Flushable {
        @Override
        public String toString() {
            return "" + e;
        }

        public Stream<ActionInfo> definedActions() {
            if (actions.isEmpty()) {
                buildActions(domain, null, e, e.getQualifiedName().toString(), faces, this.actions, null);
            }
            return actions.stream();
        }

        @Override
        public void flush(Domain domain) {
            domain.ctx().save(JavaFile.builder(domain.pkg(), spec.build()).build());
        }

        @Override
        public Activities _this() {
            return this;
        }
    }

    private static void buildActions(Domain domain,
                                     @Nullable TypeElement context,
                                     TypeElement e,
                                     String activitiesQualifiedName,
                                     List<TypeElement> faces,
                                     List<ActionInfo> actionList,
                                     @Nullable Map<StoreRefer, StoreMethod> stores
    ) {
        var ctx = domain.ctx();
        var index = new AtomicInteger(-1);
        var actions = faces.stream()
                .flatMap(ctx::methods)
                .filter(ctx::isPublic)
                .filter(Predicate.not(ctx::isStatic))
                .filter(Predicate.not(ctx::isDefault))
                .filter(x -> ctx.notObjectClass(x.getReceiverType()))
                .filter(ctx.parameter(1).or(ctx::noParameters))
                .filter(x -> ctx.isVertxFuture(x.getReturnType()))
                .map(x -> ctx.toMethod(index.incrementAndGet(), x))
                .collect(Collectors.groupingBy(ExecutableMethod::signature))
                .values()
                .stream()
                .map(ctx::preferOverride)
                .sorted(Comparator.comparingInt(ExecutableMethod::index))
                .toList();
        var actionNames = new HashSet<String>();
        var dominate = stores != null;
        for (var action : actions) {
            var method = action.method();
            var name = method.getSimpleName().toString();
            if (!actionNames.add(name))
                throw new IllegalStateException("Duplicate Activities " + activitiesQualifiedName + " action name: " + name);
            var p = method.getParameters().isEmpty() ? null : method.getParameters().getFirst();
            var r = ctx.futureContent(method.getReturnType());
            if (r == null)
                throw new IllegalStateException("Activities " + activitiesQualifiedName + " action should returns Future " + method);
            var ir = p == null ? null : CodeBlock.builder();
            var iw = p == null ? null : CodeBlock.builder();
            CodecBuilder.BufferCodecBuilder inCodec = null;
            TypeMirror inType = null;
            boolean inNullable = false;
            if (p != null) {
                inType = p.asType();
                inCodec = domain.buffer(method, inType);
                inNullable = ctx.nullable(inType);
                var arg = p.getSimpleName().toString();
                inCodec.apply(null, iw, arg, !inNullable, false, false, inType.getKind().isPrimitive());
                inCodec.apply(ir, null, "b", !inNullable, false, false, false);
            }
            var ro = ctx.maybeOptional(r);
            var rt = ro != null ? ro : r;
            CodecBuilder.BufferCodecBuilder ret = domain.buffer(method, rt);
            var retRequired = ro == null
                              && !ctx.nullable(rt)
                              && !ctx.sameType(rt, Void.class);
            var storeTrait = false;
            var curd = !dominate ? null : action.annotationValues(vat.api.meta.Access.class).orElse(null);
            if (curd != null) {
                if (p == null) throw new IllegalStateException("parameter required for Access methods: " + method);
                var actionName = action.method().getSimpleName().toString();
                if (actionName.startsWith("identity")) {
                    if (ro == null)
                        throw new IllegalStateException("identity should product optional value: " + method);
                    if (!ctx.rawAssignableTo(p.asType(), long.class))
                        throw new IllegalStateException("identity should consume a long entity id: " + method);
                    updateCommon(ctx, e, stores, action, curd, StoreCurdMethod.make(StoreCurdMethod.Mode.IDENTITY, domain, context), StoreMethod::identity, StoreMethod::withIdentity);
                    storeTrait = true;
                } else if (actionName.startsWith("authorize")) {
                    if (ro == null)
                        throw new IllegalStateException("authorize should product optional value " + method);
                    if (!ctx.rawAssignableTo(p.asType(), long.class))
                        throw new IllegalStateException("authorize should consume a long user id: " + method);
                    if (!ctx.rawAssignableTo(rt, Ability.class))
                        throw new IllegalStateException("authorize should product an Ability Entity: " + method);
                    updateCommon(ctx, e, stores, action, curd, StoreCurdMethod.make(StoreCurdMethod.Mode.AUTHORIZE, domain, context), StoreMethod::authorize, StoreMethod::withAuthorize);
                    storeTrait = true;
                } else if (actionName.startsWith("create")) {
                    if (!ctx.rawAssignableTo(p.asType(), Accessor.Creator.class))
                        throw new IllegalStateException("create should consume a Access.Creator: " + method);
                    if (!ctx.rawAssignableTo(rt, Entity.class))
                        throw new IllegalStateException("create should product an Entity: " + method);
                    updateCommon(ctx, e, stores, action, curd, StoreCurdMethod.make(StoreCurdMethod.Mode.CREATE, domain, context), StoreMethod::create, StoreMethod::withCreate);
                    storeTrait = true;
                } else if (actionName.startsWith("remove")) {
                    if (!ctx.rawAssignableTo(p.asType(), Accessor.Remover.class))
                        throw new IllegalStateException("remove should consume a Access.Remover: " + method);
                    if (!ctx.rawAssignableTo(rt, Void.class))
                        throw new IllegalStateException("remove should product Void: " + method);
                    updateCommon(ctx, e, stores, action, curd, StoreCurdMethod.make(StoreCurdMethod.Mode.REMOVE, domain, context), StoreMethod::remove, StoreMethod::withRemove);
                    storeTrait = true;
                } else if (actionName.startsWith("update")) {
                    if (!ctx.rawAssignableTo(p.asType(), Accessor.Modificator.class))
                        throw new IllegalStateException("update should consume a Access.Modificator: " + method);
                    if (!ctx.rawAssignableTo(rt, Void.class) && !ctx.rawAssignableTo(rt, Entity.Entry.class))
                        throw new IllegalStateException("update should product Void or Any of Entity.Entry: " + method);
                    updateCommon(ctx, e, stores, action, curd, StoreCurdMethod.make(StoreCurdMethod.Mode.UPDATE, domain, context), null, StoreMethod::withUpdate);
                    storeTrait = true;
                } else throw new IllegalStateException("unsupported Access method: " + action);

            }
            actionList.add(new ActionInfo(p, inType, inType == null ? null : p.getSimpleName().toString(), rt, method, inCodec, ret, inNullable, !retRequired, ro != null, action
                    .annotationValues(Auditing.class)
                    .map(a -> new Audited(a, activitiesQualifiedName, method))
                    .orElse(null), storeTrait
            ));
        }
    }

    ClassName AUDIT_REQUEST = ClassName.get("vat.foundation.audits.api.Audits", "AuditRequestAuditor");
    ClassName AUDIT_REQUEST_EVENT = ClassName.get("vat.foundation.audits.api.Audits", "AuditRequest");
    ClassName AUDIT_RESPONSE = ClassName.get("vat.foundation.audits.api.Audits", "AuditResponseAuditor");
    ClassName AUDIT_RESPONSE_EVENT = ClassName.get("vat.foundation.audits.api.Audits", "AuditResponse");
    ClassName AUDIT_INVOKE = ClassName.get("vat.foundation.audits.api.Audits", "AuditInvokeAuditor");
    ClassName AUDIT_INVOKE_EVENT = ClassName.get("vat.foundation.audits.api.Audits", "AuditInvoke");


    private static void updateCommon(Context ctx, TypeElement where
            , Map<StoreRefer, StoreMethod> commons
            , ExecutableMethod action
            , AnnotatedValue curd
            , BiFunction<ExecutableMethod, AnnotatedValue, StoreCurdMethod> create
            , @Nullable Function<StoreMethod, @Nullable StoreCurdMethod> check
            , BiFunction<StoreMethod, StoreCurdMethod, StoreMethod> act) {
        var out = create.apply(action, curd);
        var hold = commons.values().stream().filter(x -> {
            var t = x.store.entity;
            return t != null
                   && (ctx.sameEntity(t.asType(), out.entity)
                       || ctx.sameEntity(t.asType(), ctx.futureContent(action.resolvedType(where))));
        }).findFirst().orElse(null);
        if (hold == null) return;
        if (check != null && check.apply(hold) != null)
            throw new IllegalStateException("common method already defined: " + check.apply(hold) + " conflict with " + action);
        commons.remove(hold.store);
        commons.put(hold.store, act.apply(hold, out));
    }

    static Activities activities(Domain domain, TypeElement e) {
        var proxyName = domain.activitiesProxyName(e);
        var proxyType = domain.activitiesProxyTypeName(e);
        return new Activities(
                domain,
                e,
                proxyName,
                proxyType,
                Domain.addDomainIdentity(e, TypeSpec.classBuilder(proxyName)
                                .addAnnotations(EqualStringNoSuper)
                                .addModifiers(Modifier.PUBLIC)
                                .superclass(BaseActivitiesProxy.class)
                                .addSuperinterface(e.asType()))
                        .addMethod(MethodSpec.constructorBuilder()
                                .addModifiers(Modifier.PUBLIC)
                                .addParameter(Vertx.class, "vertx")
                                .addParameter(String.class, "address")
                                .addParameter(DeliveryOptions.class, "options")
                                .addStatement(
                                        "super(vertx,address==null?$T.class.getCanonicalName():address,options)",
                                        e.asType())
                                .build()),
                Stream.concat(domain.ctx().interfaces(e).stream(), Stream.of(e)).distinct().toList(),
                new ArrayList<>()
        );
    }

    sealed interface ContextMethod {
    }

    record ConfigMethod(
            String fieldName,
            ExecutableElement method,
            String pointer,
            boolean once,
            @Nullable FuncField mapping,
            TypeMirror result,
            TypeMirror rawType,
            boolean opt,
            String reader
    ) implements ContextMethod {
    }

    record ErrorMethod(
            String fieldName,
            ExecutableElement method,
            String pointer
    ) implements ContextMethod {
    }

    record EventMethod(
            TypeMirror eventType,
            boolean subscriber,
            ExecutableElement method,
            @Nullable String address,
            boolean pointer,
            String eventName,
            ClassName eventDataType,
            @Nullable String parameterName
    ) implements ContextMethod {
    }

    record UsesMethod(
            TypeMirror domain,
            ExecutableElement method,
            @Nullable String address,
            String addressFieldName,
            boolean pointer
    ) implements ContextMethod {
    }

    record StoreRefer(
            @Nullable TypeElement entity,
            @Nullable String schema,
            boolean pointer,
            ClassName storeType,
            String schemaFieldName
    ) {
        @Override
        public boolean equals(Object obj) {
            return obj instanceof StoreRefer o && Objects.equals(o.entity, entity);
        }
    }

    record StoreCurdMethod(
            ExecutableMethod method,
            TypeMirror entity,
            String copierStrategy,
            @Nullable AuthorizeInfo authorize
    ) {
        enum Mode {
            IDENTITY, UPDATE, REMOVE, CREATE, AUTHORIZE
        }

        static final Function4<Domain, @Nullable TypeElement, AnnotatedValue, ExecutableMethod, AuthorizeInfo> AUTHORIZE = (ctx, e, c, m) ->
                AuthorizeInfo.parse(c, ctx, e, m.method());
        static Function<Mode, UnaryOperator<AuthorizeInfo>> ERROR_AUTHORIZE_PREDICATE = m -> $ -> {
            throw new IllegalStateException(m.name().toLowerCase() + " should not have authorize predicate defined");
        };
        static Function<Mode, UnaryOperator<String>> ERROR_COPIER = m -> $ -> {
            throw new IllegalStateException(m.name().toLowerCase() + " should not have copier defined");
        };
        static Function<Mode, UnaryOperator<String>> ERROR_AUTHORIZE = m -> $ -> {
            throw new IllegalStateException(m.name().toLowerCase() + " should not have authorize defined");
        };
        static Function2<ExecutableMethod, AnnotatedValue, TypeMirror> ENTITY = (m, c) -> c.getType("entity")
                .filter(x -> !m.ctx().isVoid(x))
                .orElseGet(() -> {
                    var raw = m.ctx().futureContent(m.method().getReturnType());
                    raw = m.ctx().orOptional(raw);
                    if (!m.ctx().rawAssignableTo(raw, Entity.class))
                        throw new IllegalStateException("can't found entity type for: " + m.method());
                    return raw;
                });

        static BiFunction<ExecutableMethod, AnnotatedValue, StoreCurdMethod> make(Mode mode, Domain dom, @Nullable TypeElement own) {
            return switch (mode) {
                case IDENTITY, AUTHORIZE -> (m, c) -> new StoreCurdMethod(
                        m,
                        ENTITY.apply(m, c),
                        c.getString().map(ERROR_COPIER.apply(mode)).orElse(""),
                        AnnotatedValue.of(m.method(), Authorized.class).map(v -> AUTHORIZE.apply(dom, own, v, m)).map(ERROR_AUTHORIZE_PREDICATE.apply(mode)).orElse(null)
                );
                case UPDATE,
                     REMOVE,
                     CREATE -> (m, c) -> new StoreCurdMethod(
                        m,
                        ENTITY.apply(m, c),
                        c.getString().orElse(""),
                        AnnotatedValue.of(m.method(), Authorized.class).map(v -> AUTHORIZE.apply(dom, own, v, m)).orElse(null)
                );
            };
        }

    }

    record StoreMethod(
            StoreRefer store,
            ExecutableElement method,
            @Nullable String txName,
            @With boolean generated,
            @Nullable @With StoreCurdMethod identity,
            @Nullable @With StoreCurdMethod authorize,
            @Nullable @With StoreCurdMethod create,
            @Nullable @With StoreCurdMethod remove,
            Set<StoreCurdMethod> update
    ) implements ContextMethod {
        StoreMethod(StoreRefer store, ExecutableElement method, @Nullable String txName) {
            this(store, method, txName, false, null, null, null, null, new HashSet<>());
        }

        StoreMethod withUpdate(StoreCurdMethod update) {
            var old = this.update.add(update);
            if (!old) throw new IllegalStateException("duplicated update method: " + update);
            return this;
        }
    }

    String AUDIT_IMPL = "do";
    String AUTH_IMPL = "doAuth";

    record Dominate(
            Domain domain,
            TypeElement e,
            TypeMirror activities,
            String name,
            ClassName type,
            TypeSpec.Builder spec,
            /// Monadic context
            TypeSpec.@Nullable Builder ctx,
            boolean endpoint,
            List<TypeElement> faces,
            List<ParameterSpec> parameters,
            List<ContextMethod> contextual,
            List<ActionInfo> actions,
            Map<StoreRefer, StoreMethod> stores
    ) implements Builders, Flushable, Applicative<Dominate> {

        @Override
        public Dominate _this() {
            return this;
        }

        public TypeSpec.Builder ctxOrSpec() {
            return ctx == null ? spec : ctx;
        }

        public boolean haveMonadic() {
            return ctx != null;
        }

        @Override
        public void flush(Domain domain) {
            domain.ctx().save(JavaFile.builder(domain.pkg(), spec.build()).build());
        }

        public Dominate computes() {
            if (!stores.isEmpty() || !this.actions.isEmpty() || !this.contextual.isEmpty()) return this;
            var sql = false;
            var config = false;
            var errors = false;
            var ctx = domain.ctx();
            var index = new AtomicInteger(-1);
            var contextual = faces.stream()
                    .flatMap(ctx::methods)
                    .filter(ctx::isPublic)
                    .filter(Predicate.not(ctx::isStatic))
                    .filter(ctx::isDefault)
                    .filter(x -> ctx.notObjectClass(x.getReceiverType()))
                    .map(x -> ctx.toMethod(index.incrementAndGet(), x))
                    .collect(Collectors.groupingBy(ExecutableMethod::signature))
                    .values()
                    .stream()
                    .map(ctx::preferOverride)
                    .sorted(Comparator.comparingInt(ExecutableMethod::index))
                    .toList();
            for (var cx : contextual) {
                var ok = 0;
                var method = cx.method();
                var name = method.getSimpleName().toString();
                //config
                {
                    var anno = cx.annotationValues(Config.class).orElse(null);
                    if (!config) config = anno != null;
                    if (anno != null) {
                        ok++;
                        if (!method.getParameters().isEmpty()) throw new IllegalStateException();
                        var pointer = anno.getString("value").orElseGet(() -> "/" + name);
                        var once = anno.getBoolean("once").orElse(false);
                        Pointer.of(pointer);//* check
                        var type = method.getReturnType();
                        var mapping = anno.getString("mapping").filter(Predicate.not(String::isBlank))
                                .map(field -> new FuncField(anno.getType("holder")
                                        .filter(Predicate.not(ctx::isVoid))
                                        .orElse(e.asType()), field));
                        var confType = mapping.flatMap(u -> ctx.asElement(u.holder()).getEnclosedElements().stream()
                                        .filter(ctx::isField)
                                        .map(VariableElement.class::cast)
                                        .filter(x -> x.getSimpleName().contentEquals(u.field()))
                                        .findFirst()
                                        .map(ctx::functionParameter0))
                                .orElse(type);
                        var raw = ctx.maybeOptional(confType);
                        var opt = raw != null;
                        if (!opt) raw = confType;
                        var read = ctx.configReader(method, raw);
                        raw = ctx.boxed(raw);
                        this.contextual.add(new ConfigMethod("_conf" + CaseConv.caption(name), method, pointer, once, mapping.orElse(null), confType, raw, opt, read));
                    }
                }
                //error
                {
                    var error = cx.annotationValues(Errors.class).orElse(null);
                    if (!config) config = error != null;
                    if (!errors) errors = error != null;
                    if (error != null) {
                        if (ctx.maybeOptional(method.getReturnType()) != null)
                            throw new IllegalStateException("error define should not contains Optional: " + method);
                        if (!ctx.rawAssignableTo(method.getReturnType(), DomainError.class))
                            throw new IllegalStateException("error define should products DomainError : " + method);
                        ok++;
                        var pointer = error.getString("value")
                                .filter(Predicate.not(String::isBlank))
                                .map(x -> x.startsWith("/errors/") ? x : ((x.startsWith(
                                        "/") ? "/errors" : "/errors/") + x))
                                .orElse("/errors/" + name);
                        var fieldName = "_error" + CaseConv.caption(name);
                        this.contextual.add(new ErrorMethod(fieldName, method, pointer));
                    }
                }
                //storage
                {
                    var anno = cx.annotationValues(vat.api.meta.Storage.class).orElse(null);
                    if (anno != null) {
                        if (!sql) sql = true;
                        ok++;
                        var addr = anno.getString("value").filter(Predicate.not(String::isBlank)).orElse(null);
                        var pointer = addr != null && addr.startsWith("/");
                        if (pointer && !config) config = true;
                        var entity = ctx.storeParameter(method.getReturnType());
                        assert entity != null : "not found entity" + method.getReturnType();
                        var entityEl = ctx.typeElementOf(entity);
                        assert entityEl != null : "missing type element of " + entity;
                        var storeType = ClassName.get(domain.pkg(), entityEl.getSimpleName().toString() + "Store");
                        var sqlName = method.getParameters().isEmpty() ? null : method.getParameters().getFirst()
                                .getSimpleName().toString();
                        var store = new StoreRefer(entityEl, addr, pointer, storeType, "_schema" + CaseConv.caption(name));
                        var storeMth = new StoreMethod(store, method, sqlName);
                        this.contextual.add(storeMth);
                        stores.put(store, storeMth);
                    }

                }
                //publisher
                {
                    var anno = cx.annotationValues(Publish.class).orElse(null);
                    if (anno != null) {
                        ok++;
                        var addr = anno.getString("value").filter(Predicate.not(String::isBlank)).orElse(null);
                        var pointer = addr != null && addr.startsWith("/");
                        if (pointer && !config) config = true;
                        var evt = ctx.validatePublisherSignature(method);
                        var evtName = ((DeclaredType) evt).asElement().getSimpleName().toString();
                        var evtDataType = ClassName.get(domain.pkg(), ((DeclaredType) evt).asElement().getSimpleName()
                                                                              .toString() + "Data");
                        var paramName = method.getParameters().getFirst().getSimpleName().toString();
                        this.contextual.add(new EventMethod(evt, false, method, addr, pointer, evtName, evtDataType, paramName));
                    }

                }
                //subscriber
                {
                    var anno = cx.annotationValues(Subscribe.class).orElse(null);
                    if (anno != null) {
                        ok++;
                        var evt = ctx.validateSubscriberSignature(method);
                        var addr = anno.getString("value").filter(Predicate.not(String::isBlank)).orElse(null);
                        var evtName = ((DeclaredType) evt).asElement().getSimpleName().toString();
                        var pointer = addr != null && addr.startsWith("/");
                        if (pointer && !config) config = true;
                        var evtDataType = ClassName.get(domain.pkg(), ((DeclaredType) evt).asElement().getSimpleName()
                                                                              .toString() + "Data");
                        this.contextual.add(new EventMethod(evt, true, method, addr, pointer, evtName, evtDataType, null));
                    }

                }
                //uses
                {
                    var anno = cx.annotationValues(Uses.class).orElse(null);
                    if (anno != null) {
                        var dom = cx.resolvedType(e);
                        if (dom == null || !ctx.rawAssignableTo(dom, vat.api.Activities.class))
                            throw new IllegalStateException("@Uses should returns Future of Activities: " + method);
                        var addr = anno.getString("value")
                                .filter(Predicate.not(String::isBlank))
                                .orElse(null);
                        var pointer = addr != null && addr.startsWith("/");
                        this.contextual.add(new UsesMethod(dom, method, addr, "_address" + CaseConv.caption(name), pointer));
                    }
                }
                if (ok > 1)
                    throw new IllegalStateException("Config,Errors,Storage... should only exists one");
            }
            if (sql) {
                parameters.add(ParameterSpec.builder(Pool.class, "sql").build());
                parameters.add(ParameterSpec.builder(Dialect.class, "dialect").build());
            }
            if (config) parameters.add(ParameterSpec.builder(JsonObject.class, "conf").build());
            var activities = ctx.typeElementOf(this.activities);
            assert activities != null : "missing activities";
            Builders.buildActions(domain, e, activities, activities.getQualifiedName().toString(), faces, this.actions, stores);
            return this;
        }
    }

    ClassName LOGGER_FACTORY = ClassName.get(" org.slf4j", "LoggerFactory");
    ClassName LOGGER = ClassName.get(" org.slf4j", "Logger");

    static Dominate dominate(Domain domain, TypeElement e) {
        var activitiesDefineType = domain.findActivitiesDefine(e);
        var activitiesName = domain.activitiesDomainName(e);
        var activitiesType = domain.activitiesDomainTypeName(e);
        var anno = AnnotatedValue.of(e, Enhance.class).orElseThrow(() -> DomainError.System.conflict("missing Enhance annotation"));
        var ctx = anno.getBoolean("monadic").orElse(true);

        var activities = Domain.addDomainIdentity(e, TypeSpec.classBuilder(activitiesName)
                        .addAnnotation(ApiStatus.Internal.class)
                        .addTypeVariable(TypeVariableName.get("T",
                                ParameterizedTypeName.get(
                                        activitiesType,
                                        TypeVariableName.get("T"))))
                        .addAnnotations(EqualStringSuper)
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .superclass(ParameterizedTypeName.get(
                                ClassName.get(BaseActivities.class),
                                TypeName.get(e.asType()),
                                TypeVariableName.get("T")))
                        .addSuperinterface(e.asType()))
                .addField(FieldSpec.builder(LOGGER, "log")
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
                        .initializer("$T.getLogger($T.class)", LOGGER_FACTORY, e.asType())
                        .build());

        var ctxType = ctx ? TypeSpec.recordBuilder("Context")
                .addSuperinterface(e.asType())
                .addSuperinterface(MonadicContext.class) : null;
        var web = anno
                .getBoolean("endpoint").orElse(false);
        var parameters = new ArrayList<>(List.of(
                ParameterSpec.builder(Vertx.class, "vertx").build(),
                ParameterSpec.builder(String.class, "address").build()
        ));
        if (web) parameters.add(ParameterSpec.builder(Web.Factory.class, "web").build());
        return new Dominate(
                domain, e,
                activitiesDefineType,
                activitiesName,
                activitiesType,
                activities,
                ctxType,
                web,
                Stream.concat(domain.ctx().interfaces(e).stream(), Stream.of(e)).distinct().toList(),
                parameters,
                new ArrayList<>(),
                new ArrayList<>(),
                new HashMap<>()
        );
    }
}

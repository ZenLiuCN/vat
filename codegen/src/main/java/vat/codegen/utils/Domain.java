package vat.codegen.utils;

import com.google.auto.service.AutoService;
import com.palantir.javapoet.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import vat.api.Activities;
import vat.api.Data;
import vat.api.Prototype;
import vat.api.implement.Codec;
import vat.api.meta.Column;
import vat.api.meta.EnumName;
import vat.api.meta.Virtual;
import vat.api.metadata.MetaData;
import vat.api.store.Field;
import vat.api.store.Model;
import vat.api.utils.Fn;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static vat.api.implement.Codec.any;
import static vat.codegen.utils.Builders.SKIP_VALIDATIONS;

///
/// @author Zen.Liu
/// @since 2025-12-03
public record Domain(
        Context ctx,
        String pkg,
        String simpleName,
        String internalSimpleName,
        TypeSpec.Builder codec,
        TypeSpec.Builder internalCodec,
        /// shared codec registry.
        Map<String, CodecInfo> codecs,
        List<TypeElement> elements,
        /// domain declared elements
        Map<ElementType, Set<Flushable>> declared,
        /// domain used
        Set<TypeName> uses,
        MetaData.Domain.Meta meta,
        Map<DDL, StringBuilder> ddl,
        AtomicBoolean haveInternal
) {
    public static Stream<ResolvedField> resolveDataFields(Domain domain, TypeMirror input) {
        var ctx=domain.ctx;
        var src= ctx.typeElementOf(input);
        if(src==null) throw new IllegalStateException("missing type element of "+input);
        var prd=Fn.<ExecutableElement>truePredicate();
        if(ctx.rawAssignableTo(input,Data.Request.class)||ctx.rawAssignableTo(input, Data.Validation.class)){
            prd =  SKIP_VALIDATIONS.apply(ctx) ;
        }
        if(Context.isInternal(src)) throw new IllegalStateException("should not be internal type of "+input);
        var index=new AtomicInteger(-1);
        return  Stream.concat(ctx.interfaces(src).stream(),Stream.of(src))
                .flatMap(ctx::methods)
                .filter(x -> ctx.notObjectClass(x.getReceiverType()))
                .filter(Predicate.not(ctx::isStatic))
                .filter(ctx::noParameters)
                .filter(ctx.returnType(Predicate.not(ctx::isVoid)))
                //* Data.Request field.
                .filter(prd)
                .map(x -> ctx.getterToField(index.incrementAndGet(), x))
                .collect(Collectors.groupingBy(GetterField::signature))
                .values()
                .stream()
                .map(ctx::preferOverride)
                .sorted(Comparator.comparingInt(GetterField::index))
                .peek(x -> {
                    var t = x.resolvedType(src);
                    if (ctx.isVertxFuture(t))
                        throw new IllegalStateException("data object can't have Future property: " + x);
                })
                .map(f -> resolveField(f, src,domain,false))
                ;

    }
     static ResolvedField resolveField(GetterField f,TypeElement e,Domain domain ,boolean binary){
        var ctx = domain.ctx();
        var internal= Context.isInternal(e);
        var fieldMirror = f.resolvedType(e);
        var toJs = ctx.sameType(fieldMirror, long.class) || ctx.sameType(fieldMirror, Long.class);
        var resolvedType = !ctx.rawSameType(fieldMirror, f.type());
        var rawType = fieldMirror;
        var required=!(ctx.nullable(f.type())||ctx.nullable(fieldMirror));
        var opt = false;
        {
            var o = ctx.maybeOptional(rawType);
            if (o != null) {
                rawType = o;
                opt = true;
                required = false;
            }
        }
        CodeBlock prefer;
        if (f.alias() != null) {
            var alias = f.alias();
            prefer = alias.strict()
                    ? CodeBlock.builder().add("$S", alias.alias()).build()
                    : CodeBlock.builder().add("$S,$S", alias.alias(), f.name()).build();
        } else {
            prefer = CodeBlock.builder().add("$S", f.name()).build();
        }
        var forEnumName = false;
        //! enum type use ordinal or name
        if (ctx.rawSameType(rawType, Enum.class)) {
            boolean col = AnnotatedValue.of(f.method(), Column.class).flatMap(x -> x.getBoolean("enumName")).orElse(false);
            var en = AnnotatedValue.find(f.method(), EnumName.class).isPresent();
            if (col || en) {
                forEnumName = true;
            }
        }
        var codecInfo = domain.codec(e, rawType, domain.preferClassName(internal), domain.preferCodec(internal), binary, forEnumName);
        var virtual = AnnotatedValue.of(f.getter(), Virtual.class).orElse(null);
        String holdField;
        String holdKey;
        if (virtual != null) {
            if (!f.validators().isEmpty() || !f.interceptors().isEmpty()) {
                throw new IllegalStateException("virtual field not support validator and interceptor");
            }
            holdField = Optional.ofNullable(virtual.get("value"))
                    .map(String.class::cast)
                    .orElseThrow(() -> new IllegalStateException("virtual field value not found"));
            holdKey = Optional.ofNullable(virtual.get("key")).map(String.class::cast).orElse(f.prefer());
            return resolvedType ?
                    new ResolvedField(fieldMirror, f, codecInfo, holdField, holdKey,required, opt, prefer, toJs, forEnumName, rawType)
                    : new ResolvedField(f, codecInfo, holdField, holdKey,required, opt, prefer, toJs, forEnumName, rawType);
        } else {
            return resolvedType
                    ? new ResolvedField(fieldMirror, f, codecInfo,required, opt, prefer, toJs, forEnumName, rawType)
                    : new ResolvedField(f, codecInfo,required, opt, prefer, toJs, forEnumName, rawType);
        }
    }
    @Override
    public int hashCode() {
        return Objects.hashCode(pkg);
    }

    public void put(ElementType type, Flushable element) {
        var set = declared.computeIfAbsent(type, $ -> new HashSet<>());
        var ok = set.add(element);
        if (!ok) throw new IllegalStateException("duplicated element: " + element);
    }

    public Domain(
            Context ctx,
            String pkg,
            String simpleName,
            String internalSimpleName,
            TypeSpec.Builder codec,
            TypeSpec.Builder internalCodec,
            Map<String, CodecInfo> codecs,
            List<TypeElement> elements

    ) {
        this(ctx, pkg, simpleName, internalSimpleName, codec, internalCodec, codecs,
                elements,
                new HashMap<>(),
                new HashSet<>(),
                new MetaData.Domain.Meta(new JsonObject()),
                new HashMap<>(),
                new AtomicBoolean(false)
        );
    }

    public StringBuilder getDDL(DDL ddl) {
        return this.ddl.computeIfAbsent(ddl, d -> new StringBuilder());
    }

    public static final ParameterizedTypeName StoreFieldType = ParameterizedTypeName.get(ClassName.get(Field.class),
            WildcardTypeName.subtypeOf(Object.class));
    public static final ParameterizedTypeName StoreModelType = ParameterizedTypeName.get(ClassName.get(Model.class),
            WildcardTypeName.subtypeOf(Object.class));

    public Domain(Context ctx, Map<String, CodecInfo> codecs, String pkg, List<TypeElement> elements) {
        this(
                ctx, pkg, CODECS_NAME, CODECS_INTERNAL_NAME,
                TypeSpec.classBuilder(CODECS_NAME)
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addAnnotation(AnnotationSpec.builder(AutoService.class)
                                .addMember("value", TypeName.get(Codec.Provider.class).toString() + ".class")
                                .build())
                        .addSuperinterface(TypeName.get(Codec.Provider.class))
                        .addJavadoc("Generated Domain Codecs"),
                TypeSpec.classBuilder(CODECS_INTERNAL_NAME)
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addAnnotation(AnnotationSpec.builder(AutoService.class)
                                .addMember("value", TypeName.get(Codec.InternalProvider.class).toString() + ".class")
                                .build())
                        .addSuperinterface(TypeName.get(Codec.InternalProvider.class))
                        .addJavadoc("Generated Domain Internal Codecs"),
                codecs,
                elements);
    }

    public static final String CODECS_INTERNAL_NAME = "InternalCodecs";
    public static final String CODECS_NAME = "Codecs";
    public static final String COPY_PREFIX = "copyFrom";
    public static final String CODEC_DATA_SUFFIX = "_DATA";
    public static final String CODEC_BINARY_SUFFIX = "_BINARY";
    public static final String CODEC_JS_SUFFIX = "_JS";
    public static ClassName DATA_CODEC = ClassName.get("vat.api.implement", "Codec");
    public static ClassName DATA_COMMON_CODEC = ClassName.get("vat.api.implement", "CommonCodec");

    public void flush() {
        codec.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC).build());
        ctx.save(JavaFile.builder(pkg, codec.build()).build());
        if (haveInternalCodec()) {
            internalCodec.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC).build());
            ctx.save(JavaFile.builder(pkg, internalCodec.build()).build());
        }
        ctx.saveResource("META-INF/meta/" + meta.identity() + ".json", clean(meta.asJson()));
        declared.forEach((k, v) -> v.forEach(e -> e.flush(this)));
        ddl.forEach((k, v) -> {
            if (!v.isEmpty())
                ctx.saveToRoot(Codec.QUALIFIED_UPPER_SNAKE.apply(pkg) + ".%s.ddl.sql".formatted(k.name().toLowerCase()), Buffer.buffer(v.toString()));
        });

    }

    static Buffer clean(JsonObject v) {
        clear(v);
        return v.toBuffer();
    }

    static void clear(JsonArray v) {
        var n = v.size();
        var cleans = new ArrayList<Integer>();
        for (int i = 0; i < n; i++) {
            switch (v.getValue(i)) {
                case JsonArray j -> clear(j);
                case JsonObject j -> clear(j);
                case null -> cleans.add(i);
                case String s when s.isEmpty() -> cleans.add(i);
                default -> {
                }
            }
        }
        for (var clean : cleans) {
            v.remove((int) clean);
        }
    }

    static void clear(JsonObject v) {
        var cleans = new ArrayList<String>();
        for (var k : v.getMap().keySet()) {
            switch (v.getValue(k)) {
                case JsonArray j -> clear(j);
                case JsonObject j -> clear(j);
                case null -> cleans.add(k);
                case String s when s.isEmpty() -> cleans.add(k);
                default -> {
                }
            }
        }
        for (var clean : cleans) {
            v.remove(clean);
        }
    }

    public TypeSpec.Builder preferCodec(boolean internal) {
        var v= internal ? internalCodec : codec;
        if(v==internalCodec) haveInternal.set(true);
        return v;
    }


    public boolean haveInternalCodec() {
        return haveInternal.get();
    }

    public String fullName() {
        return pkg + "." + simpleName;
    }

    public ClassName fullClassName() {
        return ClassName.get(pkg, simpleName);
    }

    public String fullInternalName() {
        return pkg + "." + internalSimpleName;
    }

    public ClassName fullInternalClassName() {
        return ClassName.get(pkg, internalSimpleName);
    }

    public ClassName preferClassName(boolean internal) {
        if (internal) haveInternal.compareAndExchange(false, true);
        return ClassName.get(pkg, internal ? internalSimpleName : simpleName);
    }

    public String dataName(Element e) {
        return e.getSimpleName() + "Data";
    }

    public String pojoName(Element e) {
        return e.getSimpleName() + "Object";
    }

    public String storeName(Element e) {
        return e.getSimpleName() + "Store";
    }

    public TypeMirror findActivitiesDefine(Element e) {
        if (ctx.rawAssignableTo(e.asType(), vat.api.Domain.Context.class)) {
            return ((TypeElement) e).getInterfaces()
                    .stream()
                    .filter(x ->
                            x.getAnnotationsByType(Prototype.class).length == 0 && ctx.rawAssignableTo(x, Activities.class)
                    )
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("not found context supported activities: " + e));
        }
        return e.asType();
    }

    public String activitiesDomainName(Element e) {
        var t = findActivitiesDefine(e);
        return Objects.requireNonNull(ctx.typeElementOf(t)).getSimpleName() + "Domain";
    }

    public String activitiesProxyName(Element e) {
        return e.getSimpleName() + "Proxy";
    }

    public ClassName dataTypeName(Element e) {
        return ClassName.get(pkg, dataName(e));
    }

    public ClassName pojoTypeName(Element e) {
        return ClassName.get(pkg, pojoName(e));
    }

    public ClassName storeTypeName(Element e) {
        return ClassName.get(pkg, storeName(e));
    }

    public ClassName activitiesDomainTypeName(Element e) {
        return ClassName.get(pkg, activitiesDomainName(e));
    }

    public ClassName activitiesProxyTypeName(Element e) {
        return ClassName.get(pkg, activitiesProxyName(e));
    }


    public static TypeSpec.Builder addDomainIdentity(Element domain, TypeSpec.Builder builder) {
        return builder.addMethod(MethodSpec.methodBuilder("domainIdentity")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.FINAL, Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(Class.class),
                        TypeName.get(domain.asType())))
                .addStatement("return $T.class", domain.asType())
                .build());
    }

    interface Member {
        TypeElement define();

        Builders builder();
    }


    record Members(
            AtomicReference<Member> activities,
            Map<TypeName, Member> actors,
            Map<TypeName, Member> events,
            Map<TypeName, Member> abilities,
            Map<TypeName, Member> records,
            Map<TypeName, Member> data,
            Map<TypeName, Member> dataType,
            Map<TypeName, Member> pojoType,
            Map<TypeName, Member> store
    ) {
        Members() {
            this(new AtomicReference<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>());
        }
    }

    //region JSON codec
    public CodecInfo codec(Element where,
                           TypeMirror type,
                           ClassName codecQualifiedName,
                           TypeSpec.Builder codec,
                           boolean binary,
                           boolean preferEnumName
    ) {
        var internal = codecQualifiedName.canonicalName().endsWith(CODECS_INTERNAL_NAME);
        return switch (type.getKind()) {
            case BOOLEAN,
                 BYTE,
                 SHORT,
                 INT,
                 LONG,
                 CHAR,
                 FLOAT,
                 DOUBLE -> builtIn.computeIfAbsent(type.toString(), $ -> new CodecInfo(
                    DATA_CODEC, type,
                    type.getKind().name(),
                    type.getKind().name() + CODEC_BINARY_SUFFIX,
//                    type.getKind()==TypeKind.LONG?(type.getKind().name() + CODEC_JS_SUFFIX):
                    null
            ));
            case ARRAY -> {
                var in = ((ArrayType) type).getComponentType();
                if (in.getKind() == TypeKind.BYTE) {
                    yield builtIn.computeIfAbsent("BINARY", $ -> new CodecInfo(DATA_CODEC, type, "BINARY", "BYTES_BINARY", null));
                }
                var codec2Name = ctx.codecName(type);
                if (codecs.containsKey(codec2Name)) {
                    yield codecs.get(codec2Name);
                } else {
                    buildPropertyCodec(codec2Name, where, type, codec, binary, preferEnumName);
                    var c = new CodecInfo(codecQualifiedName, type, codec2Name, binary ? codec2Name + CODEC_BINARY_SUFFIX : null, null);
                    if (!internal) codecs.put(codec2Name, c);
                    yield c;
                }
            }
            case DECLARED -> {
                var dt = (DeclaredType) type;
                var unbox = ctx.unbox(type);
                if (unbox.isPresent()) {
                    yield switch (unbox.get().getKind()) {
                        case VOID,
                             BOOLEAN,
                             BYTE,
                             SHORT,
                             INT,
                             LONG,
                             CHAR,
                             FLOAT,
                             DOUBLE ->
                                builtIn.computeIfAbsent(unbox.get().getKind().name() + "_OBJECT", $ -> new CodecInfo(
                                        DATA_CODEC, type,
                                        unbox.get().getKind().name() + "_OBJECT",
                                        unbox.get().getKind().name() + "_OBJECT" + CODEC_BINARY_SUFFIX,
                                        null
                                ));
                        default -> throw new IllegalStateException("invalid boxed type: " + type);
                    };
                }
                var declaredType = ctx.typeElementOf(dt);
                assert declaredType != null;
                if (declaredType.getTypeParameters().isEmpty()) {
                    var name = declaredType.getQualifiedName().toString();
                    if (any(name,

                            "java.lang.String",
                            "io.vertx.core.buffer.Buffer",
                            "io.vertx.core.json.JsonObject",
                            "io.vertx.core.json.JsonArray",
                            "java.time.Instant"
                    )) {
                        yield builtIn.computeIfAbsent(name, n -> new CodecInfo(
                                DATA_CODEC, type,
                                CaseConv.PASCAL_UPPER_SNAKE.apply(declaredType.getSimpleName().toString()),
                                CaseConv.PASCAL_UPPER_SNAKE.apply(declaredType.getSimpleName().toString()) + CODEC_BINARY_SUFFIX,
                                null
                        ));
                    }
                }
                var fieldType = ctx.typeElementOf(type);
                assert fieldType != null : "missing field type of " + type;
                var codec2Name = ctx.codecName(type);
                if (preferEnumName) codec2Name = codec2Name + "_TEXT";
                if (codecs.containsKey(codec2Name)) {
                    yield codecs.get(codec2Name);
                } else {
                    buildPropertyCodec(codec2Name, where, type, codec, binary, preferEnumName);
                    var c = new CodecInfo(codecQualifiedName, type, codec2Name, binary ? codec2Name + CODEC_BINARY_SUFFIX : null, null);
                    if (!internal) codecs.put(codec2Name, c);
                    yield c;
                }
            }
            default -> throw new IllegalArgumentException("Unsupported type: " + type);
        };

    }

    public CodecInfo codec(Element where,
                           TypeMirror type,
                           ClassName codecQualifiedName,
                           TypeSpec.Builder codec,
                           boolean binary) {
        return codec(where, type, codecQualifiedName, codec, binary, false);
    }

    static final Map<String, CodecInfo> builtIn = new ConcurrentHashMap<>();


    void buildPropertyCodec(String name, Element where, TypeMirror type, TypeSpec.Builder codec, boolean binary, boolean preferEnumName) {
        var body = CodeBlock.builder();
        var code = CodecBuilder.JsonCodecBuilder.lookup(where, ctx, type);
        //! only for pure enum type.
        if (preferEnumName && code.type() == CodecBuilder.JsonCodecBuilder.BuilderType.ENUM) {
            code = CodecBuilder.JsonCodecBuilder.ENUM_TEXT.apply(TypeName.get(type));
        }
        var write = CodeBlock.builder();
        var read = CodeBlock.builder();
        code.write(ctx, write, "o", "k", "v", false, true);
        code.read(ctx, read, "o", "k", false, true);
        body.add("new $T<>(\n  (o,k)->$L,\n  (o,k,v)->$L)", Codec.CombineProperty.class, read.build(), write.build());
        codec.addField(FieldSpec.builder(
                        ParameterizedTypeName.get(ClassName.get(Codec.DataProperty.class), ClassName.get(type)), name,
                        Modifier.STATIC, Modifier.FINAL, Modifier.PUBLIC)
                .initializer(body.build())
                .build());
        if (binary) {
            var b = CodecBuilder.BinaryCodecBuilder.lookup(where, ctx, type);
            codec.addField(FieldSpec.builder(
                            ParameterizedTypeName.get(ClassName.get(Codec.BinaryProperty.class), ClassName.get(type)), name + CODEC_BINARY_SUFFIX,
                            Modifier.STATIC, Modifier.FINAL, Modifier.PUBLIC)
                    .initializer("new $T<>(b->$L,(b,v)->$L)",
                            Codec.BinaryProperty.I.class,
                            b.read(ctx, CodeBlock.builder(), "b", 0).build()
                            , b.write(ctx, CodeBlock.builder(), "b", "v", 0).build())
                    .build());
        }
    }

    //endregion
    //region Buffer Codec
    public CodecBuilder.BufferCodecBuilder buffer(ExecutableElement where, TypeMirror type) {
        return CodecBuilder.BufferCodecBuilder.lookup(where, ctx, type);
    }

    //endregion
    //region Store Codec
    public CodecBuilder.FieldCodecBuilder lookupField(Element where, TypeMirror f, ExecutableElement getter) {
        var column = AnnotatedValue.of(getter, Column.class).orElse(null);
        if (column != null) {
            if (column.getBoolean("enumName").orElse(false) && ctx.rawAssignableTo(f, Enum.class)) {
                return CodecBuilder.FieldCodecBuilder.ENUM_TEXT_FIELD.apply(TypeName.get(f));
            }
        }
        return CodecBuilder.FieldCodecBuilder.lookup(where, ctx, f);

    }

    //endregion Store Codec
    //region Auditing Codec
    public CodecBuilder.AuditingCodecBuilder lookupAuditing(Element where, TypeMirror f) {
        return CodecBuilder.AuditingCodecBuilder.lookup(where, ctx, f);
    }
    //endregion Auditing Codec
}

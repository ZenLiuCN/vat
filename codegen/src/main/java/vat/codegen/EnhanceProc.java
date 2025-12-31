package vat.codegen;

import com.google.auto.service.AutoService;
import com.palantir.javapoet.*;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Pool;
import vat.api.Data;
import vat.api.Disposable;
import vat.api.DomainError;
import vat.api.implement.Codec;
import vat.api.implement.PubSub;
import vat.api.implement.Web;
import vat.api.meta.Auditing;
import vat.api.meta.Authorized;
import vat.api.meta.Enhance;
import vat.api.meta.EventKind;
import vat.api.store.Dialect;
import vat.api.trait.Accessor;
import vat.api.utils.Fn;
import vat.api.utils.Lazy;
import vat.api.utils.Pointer;
import vat.codegen.utils.*;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static vat.codegen.utils.Builders.*;

///
/// @author Zen.Liu
/// @since 2025-10-27

@AutoService(Proc.class)
public class EnhanceProc implements Proc, MetaProc {
    @Override
    public Set<Class<? extends Annotation>> accept() {
        return Set.of(Enhance.class);
    }

    @Override
    public void accept(Context ctx) {
        var el = ctx.roundEnv().getElementsAnnotatedWith(Enhance.class);
        if (el.isEmpty()) return;

        var codecs = CodecInfo.loadCodecs(ctx);
        var domainTypes = new DomainTypes(ctx);
        var codecMap = el
                .stream()
                .map(x -> x instanceof TypeElement te ? te : null)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(ctx::packageOf))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, x -> new Domain(ctx, codecs, x.getKey().toString(), x.getValue())));

        el.forEach(e -> {
            if (e instanceof TypeElement t) {
                var k = ctx.packageOf(e);
                var codec = codecMap.get(k);
                assert codec != null : "not found codec for " + e;
                var tm = t.asType();
                if (ctx.assignableTo(tm, domainTypes.actorType())) {
                    forActor(t, codec);
                } else if (ctx.assignableTo(tm, domainTypes.abilityType())) {
                    forAbility(t, codec);
                } else if (ctx.assignableTo(tm, domainTypes.recordType())) {
                    forRecord(t, codec);
                } else if (ctx.assignableTo(tm, domainTypes.eventType())) {
                    forEvent(t, codec);
                } else if (
                        ctx.subTypeOf(ctx.erasure(tm), ctx.erasure(domainTypes.domainType()))
                        && ctx.subTypeOf(ctx.erasure(tm), ctx.erasure(domainTypes.activitiesType()))
                ) {
                    forActivitiesDomain(t, codec);
                } else if (ctx.subTypeOf(ctx.erasure(tm), ctx.erasure(domainTypes.activitiesType()))) {
                    forActivities(t, codec);
                } else if (ctx.assignableTo(tm, domainTypes.dataType())) {
                    forData(t, codec);
                } else
                    throw new IllegalArgumentException("Unsupported type: " + tm);
            } else
                throw new IllegalArgumentException("Unsupported element: " + e);
        });
        codecMap.values().forEach(Domain::flush);
    }


    private void forActor(TypeElement e, Domain domain) {
        if (e.getKind() != ElementKind.INTERFACE) throw new IllegalStateException("actor should be a interface: " + e);
        var store = store(e, domain);
        addActor(domain, e, store.data().resolved());
        DdlMaker.ddlMySQL(store);
        DdlMaker.ddlPG(store);
        domain.put(ElementType.ACTOR, store);
    }

    private void forAbility(TypeElement e, Domain domain) {
        if (e.getKind() != ElementKind.INTERFACE)
            throw new IllegalStateException("ability should be a interface: " + e);
        var store = store(e, domain);
        addAbility(domain, e, store.data().resolved());
        DdlMaker.ddlMySQL(store);
        DdlMaker.ddlPG(store);
        domain.put(ElementType.ABILITY, store);

    }


    private void forRecord(TypeElement e, Domain domain) {
        if (e.getKind() != ElementKind.INTERFACE) throw new IllegalStateException("record should be a interface: " + e);
        var store = store(e, domain);
        addRecord(domain, e, store.data().resolved());
        DdlMaker.ddlMySQL(store);
        DdlMaker.ddlPG(store);
        domain.put(ElementType.RECORD, store);

    }


    private void forEvent(TypeElement e, Domain domain) {
        if (e.getKind() != ElementKind.INTERFACE) throw new IllegalStateException("event should be a interface: " + e);
        var data = data(e, domain);
        var pojo = data.pojo();
        var pojoType = pojo == null ? null : ClassName.get(domain.pkg(), pojo.name());
        var dataType = data.type();
        var kind = data.resolved().stream().filter(x -> x.annotation(EventKind.class) || x.name().equals("kind"))
                .findFirst()
                .orElse(null);
        if (kind == null) throw new IllegalArgumentException("No event kind defined");
        var ctx = domain.ctx();
        var kindPrefix = "KIND_";
        var kinds = kind.annotationValues(EventKind.class)
                .map(x -> kind.type()) //! annotated on method
                .map(t ->
                        t.getKind().equals(TypeKind.DECLARED)
                        && ctx.asElement(t) instanceof TypeElement type
                                ? type : null)//! type is declared
                .map(x ->
                        x.getKind().equals(ElementKind.ENUM)
                                ? x.getEnclosedElements().stream()
                                .filter(i -> i.getKind().equals(ElementKind.ENUM_CONSTANT))
                                .map(i -> Map.entry(
                                        CodeBlock.builder()
                                                .add("$T.$L", x.asType(), i.getSimpleName().toString())
                                                .build(),
                                        CaseConv.UPPER_SNAKE_PASCAL.apply(
                                                i.getSimpleName().toString())))
                                .toList() : null)//! fetch ENUM values
                .filter(x -> !x.isEmpty())
                .orElseGet(() -> e.getEnclosedElements().stream()
                        .filter(x ->
                                x.getKind().equals(ElementKind.FIELD)
                                && x.getModifiers().contains(Modifier.PUBLIC)
                                && x.getModifiers().contains(Modifier.FINAL)
                                && x.getModifiers().contains(Modifier.STATIC)
                                && x.getSimpleName().toString()
                                        .startsWith(kindPrefix))
                        .map(x -> Map.entry(
                                CodeBlock.builder()
                                        .add("$T.$L", e.asType(), x.getSimpleName().toString())
                                        .build(),
                                CaseConv.UPPER_SNAKE_PASCAL.apply(
                                        x.getSimpleName().toString().substring(kindPrefix.length()))))
                        .toList()
                );
        if (kinds.isEmpty()) throw new IllegalArgumentException("No event kind found: " + e);
        addEvent(domain, e, data.resolved());
        var pubMethod0 = MethodSpec.methodBuilder("publish")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(Vertx.class, "vertx")
                .addParameter(String.class, "address")
                .returns(ParameterizedTypeName.get(ClassName.get(PubSub.Publish.class),
                        TypeName.get(e.asType())))
                .addStatement("return $T.publish($T.class,address,vertx)", PubSub.class, e.asType())
                .build();
        var pubMethod1 = MethodSpec.methodBuilder("publish")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(Vertx.class, "vertx")
                .returns(ParameterizedTypeName.get(ClassName.get(PubSub.Publish.class),
                        TypeName.get(e.asType())))
                .addStatement("return $T.publish($T.class,null,vertx)", PubSub.class, e.asType())
                .build();
        var subMethod0 = MethodSpec.methodBuilder("subscribe")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(Vertx.class, "vertx")
                .addParameter(String.class, "address")
                .addParameter(ParameterizedTypeName.get(ClassName.get(PubSub.Subscribe.class),
                                TypeName.get(e.asType())),
                        "subscriber")
                .returns(Disposable.class)
                .addStatement("return $T.subscribe($T.class,address,vertx,subscriber)", PubSub.class,
                        e.asType())
                .build();
        var subMethod1 = MethodSpec.methodBuilder("subscribe")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(Vertx.class, "vertx")
                .addParameter(ParameterizedTypeName.get(ClassName.get(PubSub.Subscribe.class),
                                TypeName.get(e.asType())),
                        "subscriber")
                .returns(Disposable.class)
                .addStatement("return $T.subscribe($T.class,null,vertx,subscriber)", PubSub.class,
                        e.asType())
                .build();
        if (pojo != null) {
            pojo.spec().addSuperinterface(PubSub.class)
                    .addMethod(MethodSpec.constructorBuilder()
                            .addModifiers(Modifier.PUBLIC)
                            .addStatement("super()")
                            .build())
                    .addMethod(pubMethod0)
                    .addMethod(pubMethod1)
                    .addMethod(subMethod0)
                    .addMethod(subMethod1)
            ;
        }
        data.spec().addSuperinterface(PubSub.class)
                .addMethod(pubMethod0)
                .addMethod(pubMethod1)
                .addMethod(subMethod0)
                .addMethod(subMethod1)
        ;
        for (var name : kinds) {
            if (pojo != null) pojo.spec()
                    .addMethod(MethodSpec.methodBuilder("ofKind" + name.getValue())
                            .addModifiers(Modifier.PUBLIC)
                            .returns(pojoType)
                            .addStatement("this.$L=$L", kind.name(), name.getKey())
                            .addStatement("return this")
                            .build())
                    .addMethod(MethodSpec.methodBuilder("ofKind" + name.getValue())
                            .addModifiers(Modifier.PUBLIC)
                            .addParameter(
                                    ParameterizedTypeName.get(ClassName.get(Consumer.class), pojoType),
                                    "build")
                            .returns(pojoType)
                            .addStatement("this.$L=$L", kind.name(), name.getKey())
                            .addStatement("build.accept(this)")
                            .addStatement("return this")
                            .build());
            var jsonField = AnnotatedValue.of(e, Enhance.class).orElseThrow(() -> DomainError.System.conflict("missing Enhance annotation")).getBoolean("record")
                    .orElse(true) ? "asJson" : "json";
            data.spec()
                    .addMethod(MethodSpec.methodBuilder("ofKind" + name.getValue())
                            .addModifiers(Modifier.PUBLIC)
                            .returns(dataType)
                            .addStatement("$T.$L.set(this.$L,$S,$L)", kind.codec().holder(),
                                    kind.codec().name(), jsonField, kind.name(), name.getKey())
                            .addStatement("return this")
                            .build())
                    .addMethod(MethodSpec.methodBuilder("ofKind" + name.getValue())
                            .addModifiers(Modifier.PUBLIC)
                            .addParameter(
                                    ParameterizedTypeName.get(ClassName.get(Consumer.class), dataType),
                                    "build")
                            .returns(dataType)
                            .addStatement("$T.$L.set(this.$L,$S,$L)", kind.codec().holder(),
                                    kind.codec().name(), jsonField, kind.name(), name.getKey())
                            .addStatement("build.accept(this)")
                            .addStatement("return this")
                            .build());
        }

        domain.put(ElementType.EVENT, data);
    }


    private void forActivities(TypeElement e, Domain domain) {
        if (e.getKind() != ElementKind.INTERFACE)
            throw new IllegalStateException("activities should be a interface: " + e);
        var dom = activities(domain, e);
        fillDomain(domain, e);
        Fn.Monad.<TypeSpec.Builder>operator()
                .peek(s -> dom.definedActions().forEach(act -> {
                    var method = act.method();
                    var name = method.getSimpleName();
                    var in = act.inType();
                    var inName = act.inName();
                    var inNullable = act.inNullable();
                    var inWrite = in == null ? null : CodeBlock.builder();
                    var outRead = CodeBlock.builder().add("x->");
                    var outCodec = act.outCodec();
                    var outRequired = !act.outNullable();
                    outCodec.apply(outRead, null, "x", outRequired, false, act.outOpt(), false);
                    if (in != null) {
                        var bd = act.inCodec();
                        assert bd != null;
                        bd.apply(null, inWrite, inName, !inNullable, false, false, in.getKind().isPrimitive());
                        s.addMethod(MethodSpec.overriding(method)
                                .addModifiers(Modifier.FINAL)
                                .addStatement("return invoke($S,$L).map($L)", name, inWrite.build(), outRead.build())
                                .build());
                    } else {
                        s.addMethod(MethodSpec.overriding(method)
                                .addModifiers(Modifier.FINAL)
                                .addStatement("return invoke($S,null).map($L)", name, outRead.build())
                                .build());
                    }
                }))
                .apply(dom.spec());

        domain.codec()
                .addField(FieldSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(Codec.ActivityFactory.class),
                                        TypeName.get(e.asType()))
                                , CaseConv.PASCAL_UPPER_SNAKE.apply(e.getSimpleName().toString()) + "_ACTIVITIES"
                        )
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$T::new", dom.type())
                        .build());
        domain.put(ElementType.ACTIVITIES, dom);
    }

    private void forActivitiesDomain(TypeElement e, Domain domain) {
        if (e.getKind() != ElementKind.INTERFACE)
            throw new IllegalStateException("activities domain should be a interface: " + e);
        var ctx = domain.ctx();
        var dom = dominate(domain, e);
        if (dom.haveMonadic())
            monadic(ctx, e, dom, domain);
        else
            noneMonadic(ctx, e, dom, domain);
    }

    private void monadic(Context ctx, TypeElement e, Dominate dom, Domain domain) {
        //! TODO
        noneMonadic(ctx, e, dom, domain);
    }

    private void noneMonadic(Context ctx, TypeElement e, Dominate dom, Domain domain) {
        var web = dom.endpoint();
        var ctor = CodeBlock.builder();
        var ctorInitialize = CodeBlock.builder();
        var fields = new ArrayList<FieldSpec>();
        var haveSQL = new AtomicBoolean();
        var haveConfig = new AtomicBoolean();
        dom.computes()
                .contextual()
                .forEach(cm -> {
                    switch (cm) {
                        case ConfigMethod c -> {
                            var mapping = c.mapping();
                            var pointer = c.pointer();
                            var raw = c.rawType();
                            var read = c.reader();
                            var method = c.method();
                            var opt = c.opt();
                            var fieldName = c.fieldName();
                            var name = method.getSimpleName().toString();
                            if (c.once()) {
                                var mth = MethodSpec.overriding(c.method())
                                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL);
                                if (mapping != null) {
                                    if (opt)
                                        mth.addStatement("return $T.of($S).$L(conf).map($T.$L).or($T.super::$L)"
                                                , mapping.holder(), mapping.field(), Pointer.class, pointer, read,
                                                method.getEnclosingElement().asType(), name);
                                    else
                                        mth.addStatement("return $T.of($S).$L(conf).map($T.$L).orElseGet($T.super::$L)"
                                                , mapping.holder(), mapping.field(), Pointer.class, pointer, read,
                                                method.getEnclosingElement().asType(), name);
                                } else {
                                    if (opt)
                                        mth.addStatement("return $T.of($S).$L(conf).or($T.super::$L)"
                                                , Pointer.class, pointer, read, method.getEnclosingElement().asType(), name);
                                    else
                                        mth.addStatement("return $T.of($S).$L(conf).orElseGet($T.super::$L)"
                                                , Pointer.class, pointer, read, method.getEnclosingElement().asType(), name);
                                }
                                dom.spec().addMethod(mth.build());
                            } else {
                                dom.spec().addField(addList(fields, FieldSpec.builder(
                                                ParameterizedTypeName.get(ClassName.get(Lazy.class), TypeName.get(raw)), fieldName)
                                        .addModifiers(Modifier.PROTECTED, Modifier.FINAL)
                                        .build()));
                                if (mapping != null) {
                                    if (opt)
                                        ctor.addStatement(
                                                "this.$L=$T.of(()->$T.of($S).$L(conf).map($T.$L).or($T.super::$L).orElse(null))"
                                                , fieldName, Lazy.class, Pointer.class, pointer, read,
                                                mapping.holder(), mapping.field(),
                                                method.getEnclosingElement().asType(), name
                                        );
                                    else
                                        ctor.addStatement(
                                                "this.$L=$T.of(()->$T.of($S).$L(conf).map($T.$L).orElseGet($T.super::$L))"
                                                , fieldName, Lazy.class, Pointer.class, pointer, read,
                                                mapping.holder(), mapping.field(),
                                                method.getEnclosingElement().asType(), name
                                        );
                                } else {
                                    if (opt)
                                        ctor.addStatement("this.$L=$T.of(()->$T.of($S).$L(conf).or($T.super::$L).orElse(null))"
                                                , fieldName, Lazy.class, Pointer.class, pointer, read,
                                                method.getEnclosingElement().asType(), name);
                                    else
                                        ctor.addStatement(
                                                "this.$L=$T.of(()->$T.of($S).$L(conf).orElseGet($T.super::$L))"
                                                , fieldName, Lazy.class, Pointer.class, pointer, read,
                                                method.getEnclosingElement().asType(), name);
                                }
                                if (opt)
                                    dom.spec().addMethod(MethodSpec.overriding(method)
                                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                            .addStatement("return $T.ofNullable(this.$L.get())", Optional.class, fieldName)
                                            .build());
                                else
                                    dom.spec().addMethod(MethodSpec.overriding(method)
                                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                            .addStatement("return this.$L.get()", fieldName)
                                            .build());
                            }
                            addConfigEntry(domain, method, pointer, mapping, raw, c.once());
                            haveConfig.set(true);
                        }
                        case ErrorMethod c -> {
                            var fieldName = c.fieldName();
                            var pointer = c.pointer();
                            var method = c.method();
                            var name = method.getSimpleName().toString();
                            var arg = method.getParameters()
                                    .stream()
                                    .map(x -> x.getSimpleName().toString())
                                    .collect(Collectors.joining(","));
                            var create = CodeBlock.builder();
                            var i = 0;
                            for (var parameter : method.getParameters()) {
                                if (i > 0) create.add(",");
                                create.add("($T)v[$L]", parameter.asType(), i);
                                i++;
                            }
                            ctor.addStatement(
                                    "this.$L=$T.of(()->$T.of($S).$L(conf).<$T>map($T::new).orElseGet(()->(v)->$T.super.$L($L)))"
                                    , fieldName, Lazy.class
                                    , Pointer.class, pointer, "getObject", DomainError.ErrorMaker.class,
                                    DomainError.ErrorTuple.class
                                    , method.getEnclosingElement().asType(), name, create.build()
                            );
                            dom.spec()
                                    .addField(addList(fields, FieldSpec.builder(
                                            ParameterizedTypeName.get(ClassName.get(Lazy.class),
                                                    ClassName.get(DomainError.ErrorMaker.class)),
                                            fieldName, Modifier.FINAL, Modifier.PROTECTED).build()))
                                    .addMethod(MethodSpec.overriding(method)
                                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                            .addStatement("return $L.get().make($L)", fieldName, arg)
                                            .build());
                            addErrorEntry(domain, method, pointer);
                            haveConfig.set(true);
                        }
                        case StoreMethod c -> {
                            var method = c.method();
                            var storeType = c.store().storeType();
                            var addr = c.store().schema();
                            var pointer = c.store().pointer();
                            var arg = method.getParameters().isEmpty()
                                    ? null
                                    : method.getParameters().getFirst()
                                    .getSimpleName().toString();
                            var schemaField = c.store().schemaFieldName();
                            haveSQL.set(true);
                            if (pointer || addr != null) {
                                if (pointer) {
                                    haveConfig.set(true);
                                    dom.spec().addField(addList(fields, FieldSpec.builder(ParameterizedTypeName.get(Lazy.class, String.class)
                                            , schemaField
                                            , Modifier.PROTECTED, Modifier.FINAL).build()));
                                    ctor
                                            .addStatement(
                                                    "this.$L=$T.of(()->$T.of($S).getString(conf).orElse(null))"
                                                    , schemaField, Lazy.class, Pointer.class, addr
                                            );
                                    dom.spec()
                                            .addMethod(MethodSpec.overriding(method)
                                                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                                    .returns(storeType)
                                                    .addStatement(arg == null
                                                            ? CodeBlock.builder()
                                                            .add("return $T.STORAGE.apply(sql,dialect,$L.get())"
                                                                    , storeType, schemaField)
                                                            .build()
                                                            : CodeBlock.builder()
                                                            .add("return $T.STORAGE.apply($L==null?sql:$L,dialect,$L.get())"
                                                                    , storeType, arg, arg, schemaField)
                                                            .build()
                                                    )
                                                    .build());
                                } else {
                                    dom.spec().addMethod(MethodSpec.overriding(method)
                                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                            .returns(storeType)
                                            .addStatement(arg == null
                                                    ? CodeBlock.builder()
                                                    .add("return $T.STORAGE.apply(sql,dialect,$S)"
                                                            , storeType, addr)
                                                    .build()
                                                    : CodeBlock.builder()
                                                    .add("return $T.STORAGE.apply($L==null?sql:$L,dialect,$S)"
                                                            , storeType, arg,
                                                            arg, addr)
                                                    .build()
                                            )
                                            .build());
                                }
                            } else
                                dom.spec().addMethod(MethodSpec.overriding(method)
                                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                        .returns(storeType)
                                        .addStatement(arg == null
                                                        ? "return $T.STORAGE.apply(sql,$L,$L)"
                                                        : "return $T.STORAGE.apply($L==null?sql:$L,dialect,null)"
                                                , storeType, arg == null ? "dialect" : arg, arg
                                        )
                                        .build());
                            if (arg != null)
                                dom.spec().addMethod(MethodSpec.methodBuilder(method.getSimpleName().toString())
                                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                        .returns(storeType)
                                        .addStatement("return $L(null)", method.getSimpleName())
                                        .build());

                        }
                        case EventMethod c when !c.subscriber() -> {
                            var method = c.method();
                            var paramName = method.getParameters().getFirst().getSimpleName().toString();
                            var eventDataType = c.eventDataType();
                            var event = c.eventType();
                            var eventName = c.eventName();
                            var address = c.address();
                            dom.spec()
                                    .addMethod(MethodSpec.overriding(method)
                                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                            .addStatement("$LPublish($L::accept)",
                                                    method.getSimpleName().toString(), paramName)
                                            .build())
                                    .addMethod(MethodSpec.methodBuilder(method.getSimpleName().toString() + "Publish")
                                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                            .addParameter(
                                                    ParameterizedTypeName.get(
                                                            ClassName.get(Consumer.class),
                                                            eventDataType)
                                                    , paramName
                                            )
                                            .addStatement("var e=new $T()",
                                                    ClassName.get(domain.pkg(), eventName + "Data"))
                                            .addStatement("$L.accept(e)", paramName)
                                            .addStatement("_$L.accept(e)", eventName + "Publisher")
                                            .build())
                                    .addField(addList(fields, FieldSpec.builder(
                                                    ParameterizedTypeName.get(
                                                            ClassName.get(PubSub.Publish.class),
                                                            TypeName.get(event))
                                                    , "_" + eventName + "Publisher")
                                            .addModifiers(Modifier.PROTECTED, Modifier.FINAL)
                                            .build()));
                            if (c.pointer()) {
                                haveConfig.set(true);
                                ctor.addStatement(
                                        "this._$L=$T.publish($T.class,$T.of($S).getString(conf).orElse(null),vertx)",
                                        eventName + "Publisher", PubSub.class, event, Pointer.class, address);
                            } else if (address != null)
                                ctor.addStatement("this._$L=$T.publish($T.class,$S,vertx)",
                                        eventName + "Publisher", PubSub.class, event, address);
                            else
                                ctor.addStatement("this._$L=$T.publish($T.class,null,vertx)",
                                        eventName + "Publisher", PubSub.class, event);
                            addPublish(domain, method, event, address, c.pointer());
                        }
                        case EventMethod c -> {
                            var method = c.method();
                            var handlerName = method.getSimpleName();
                            var event = c.eventType();
                            var eventName = c.eventName();
                            var address = c.address();
                            dom.spec().addMethod(MethodSpec.overriding(method)
                                    .addModifiers(Modifier.ABSTRACT)
                                    .build());
                            addSubscribe(domain.meta(), method, event, address, c.pointer());
                            if (c.pointer()) {
                                haveConfig.set(true);
                                ctor.addStatement(
                                        "super.services.put($S,$T.subscribe($T.class,$T.of($S).getString(conf).orElse(null),vertx,this::$L))",
                                        eventName + "Subscriber", PubSub.class, event,
                                        Pointer.class, address,
                                        handlerName
                                );
                            } else if (address != null)
                                ctor.addStatement("super.services.put($S,$T.subscribe($T.class,$S,vertx,this::$L))",
                                        eventName + "Subscriber", PubSub.class, event, address, handlerName);
                            else
                                ctor.addStatement("super.services.put($S,$T.subscribe($T.class,null,vertx,this::$L))",
                                        eventName + "Subscriber", PubSub.class, event, handlerName);
                        }
                        case UsesMethod c -> {
                            var method = c.method();
                            var address = c.address();
                            var pointer = c.pointer();
                            var activities = c.domain();
                            var addressName = c.addressFieldName();
                            if (pointer) {
                                haveConfig.set(true);
                                dom.spec()
                                        .addField(
                                                addList(fields, FieldSpec.builder(ParameterizedTypeName.get(Lazy.class, String.class)
                                                        , addressName
                                                        , Modifier.PROTECTED, Modifier.FINAL).build()))
                                        .addMethod(MethodSpec.overriding(method)
                                                .addModifiers(Modifier.FINAL)
                                                .addStatement("return super.activities($T.class,$L.get())",
                                                        activities, addressName)
                                                .build());
                                ctor
                                        .addStatement(
                                                "this.$L=$T.of(()->$T.of($S).getString(conf).orElse(null))"
                                                , addressName, Lazy.class, Pointer.class, address
                                        );

                            } else if (address != null) {
                                dom.spec()
                                        .addMethod(MethodSpec.overriding(method)
                                                .addModifiers(Modifier.FINAL)
                                                .addStatement("return super.activities($T.class,$S)",
                                                        activities, address)
                                                .build());
                            } else {
                                dom.spec()
                                        .addMethod(MethodSpec.overriding(method)
                                                .addModifiers(Modifier.FINAL)
                                                .addStatement("return super.activities($T.class)", activities)
                                                .build());
                            }
                            addUses(domain.meta(), method, activities, address, pointer);
                        }
                    }
                });
        if (haveSQL.get()) {
            dom.spec()
                    .addField(addList(fields, FieldSpec.builder(Pool.class, "sql", Modifier.PROTECTED, Modifier.FINAL).build()))
                    .addField(addList(fields, FieldSpec.builder(Dialect.class, "dialect", Modifier.PROTECTED, Modifier.FINAL).build()));
            ctorInitialize
                    .addStatement("this.sql=sql")
                    .addStatement("this.dialect=dialect");
        }
        if (haveConfig.get()) {
            dom.spec()
                    .addField(addList(fields, FieldSpec.builder(JsonObject.class, "conf", Modifier.PROTECTED, Modifier.FINAL).build()));
            ctorInitialize
                    .addStatement("this.conf=conf")
            ;
        }
        var auditing = new AtomicInteger(0);
        var activities = Objects.requireNonNull(ctx.typeElementOf(dom.activities()));
        var aqn = activities.getQualifiedName().toString();
        dom.actions().stream().filter(x -> !x.storeTrait()).forEach(act -> {
            var method = act.method();
            var name = method.getSimpleName();
            var in = act.inType();
            var inName = act.inName();
            var inNullable = act.inNullable();
            var inRead = in == null ? null : CodeBlock.builder();
            var outType = act.outType();
            var outWrite = CodeBlock.builder().add("x->");
            var outCodec = act.outCodec();
            var outRequired = !act.outNullable();
            outCodec.apply(null, outWrite, "x", outRequired, act.outOpt(), true, false);
            var retTail = outRequired
                    ? CodeBlock.builder().add(".map($T::requireNonNull)", Objects.class).build()
                    : CodeBlock.builder().build();
            if (in != null) {
                var bd = act.inCodec();
                assert bd != null;
                bd.apply(inRead, null, "b", !inNullable, false, false, false);
                ctor.addStatement("handle($S,b->$L($L).map($L)$L)", name, name, inRead.build(), outWrite.build(), retTail);
            } else {
                ctor.addStatement("handle($S,b->$L().map($L)$L)", name, name, outWrite.build(), retTail);
            }
            var audit = act.audit();
            var implName = AUDIT_IMPL + CaseConv.caption(name);
            CodeBlock.Builder auditCode = null;
            CodeBlock.Builder authorized = null;
            if (audit != null) {
                auditing.set(auditing.get() | (1 << audit.mode().ordinal()));
                auditCode = CodeBlock.builder();
            }
            if (AnnotatedValue.find(method, Authorized.class).isPresent()) {
                @SuppressWarnings("OptionalGetWithoutIsPresent") var authorize = AuthorizeInfo.parse(AnnotatedValue.of(method, Authorized.class).get(), domain, e, method);
                authorized = CodeBlock.builder();
                authorize.buildAuthorize(authorized);
            }
            if (authorized != null || auditCode != null) {
                if (authorized != null && auditCode != null) {
                    var authName = AUTH_IMPL + CaseConv.caption(name);
                    processAudit(domain, dom.e(), method, authName, audit, auditing, auditCode);
                    dom.spec()
                            .addMethod(MethodSpec.overriding(method)
                                    .addModifiers(Modifier.FINAL)
                                    .addStatement(auditCode.build())
                                    .build())
                            .addMethod(MethodSpec.methodBuilder(authName)
                                    .addParameters(method.getParameters().stream().map(ParameterSpec::get).toList())
                                    .returns(TypeName.get(method.getReturnType()))
                                    .addModifiers(Modifier.PROTECTED)
                                    .addCode("return $L.applyValidateFuture()", inName)
                                    .addCode(authorized.build())
                                    .addStatement("\n.flatMap(this::$L)", implName)
                                    .build())
                            .addMethod(MethodSpec.methodBuilder(implName)
                                    .addParameters(method.getParameters().stream().map(ParameterSpec::get).toList())
                                    .returns(TypeName.get(method.getReturnType()))
                                    .addModifiers(Modifier.ABSTRACT, Modifier.PROTECTED)
                                    .build())
                    ;
                } else if (authorized != null) {
                    dom.spec()
                            .addMethod(MethodSpec.overriding(method)
                                    .addModifiers(Modifier.FINAL)
                                    .addCode("return $L.applyValidateFuture()", inName)
                                    .addCode(authorized.build())
                                    .addStatement("\n.flatMap(this::$L)", implName)
                                    .build())
                            .addMethod(MethodSpec.methodBuilder(implName)
                                    .addParameters(method.getParameters().stream().map(ParameterSpec::get).toList())
                                    .returns(TypeName.get(method.getReturnType()))
                                    .addModifiers(Modifier.ABSTRACT, Modifier.PROTECTED)
                                    .build());
                } else {
                    processAudit(domain, dom.e(), method, implName, audit, auditing, auditCode);
                    dom.spec()
                            .addMethod(MethodSpec.overriding(method)
                                    .addModifiers(Modifier.FINAL)
                                    .addStatement(auditCode.build())
                                    .build())
                            .addMethod(MethodSpec.methodBuilder(implName)
                                    .addParameters(method.getParameters().stream().map(ParameterSpec::get).toList())
                                    .returns(TypeName.get(method.getReturnType()))
                                    .addModifiers(Modifier.ABSTRACT, Modifier.PROTECTED)
                                    .build());
                }
            }


            addAction(domain, e, method, outRequired, outType);
        });

        dom.stores().forEach((st, c) -> {
            if (!c.generated()) {
                //TODO
                var sql = c.txName() != null;
                var storeName = c.method().getSimpleName().toString();
                //* identity
                {
                    var act = c.identity();
                    if (act != null) {
                        var mth = act.method().method();
                        var inf = CurdInfo.of(ctx, domain, act);
                        if (!inf.retOpt) throw new IllegalStateException("identity method should returns optional");
                        if (inf.nothing) throw new IllegalStateException("identity method should not returns void");
                        var body = CodeBlock.of("return $L($L).maybe($L)", storeName, sql ? "null" : "", inf.inName);
                        var audits = CodeBlock.builder();
                        var aud = act.method().annotationValues(Auditing.class).map(a -> new Audited(a, aqn, mth)).orElse(null);
                        if (aud != null) {
                            processAudit(domain, dom.e(), mth, inf.implName, aud, auditing, audits);
                        } else {
                            audits.add("return $L($L)", inf.implName, inf.inName);
                        }
                        dom.spec()
                                .addMethod(MethodSpec.overriding(mth)
                                        .addStatement(audits.build())
                                        .build())
                                .addMethod(MethodSpec.methodBuilder(inf.implName)
                                        .addParameters(mth.getParameters().stream().map(ParameterSpec::get).toList())
                                        .returns(TypeName.get(mth.getReturnType()))
                                        .addModifiers(Modifier.PROTECTED)
                                        .addStatement(body)
                                        .build());
                    }
                }
                //* authorize
                {
                    var act = c.authorize();
                    if (act != null) {
                        var mth = act.method().method();
                        var inf = CurdInfo.of(ctx, domain, act);
                        if (!inf.retOpt) throw new IllegalStateException("identity method should returns optional");
                        if (inf.nothing) throw new IllegalStateException("identity method should not returns void");
                        var body = CodeBlock.of("return $L($L).one(t->t.user().eq($L))", storeName, sql ? "null" : "", inf.inName);
                        var audits = CodeBlock.builder();
                        var aud = act.method().annotationValues(Auditing.class).map(a -> new Audited(a, aqn, mth)).orElse(null);
                        if (aud != null) {
                            processAudit(domain, dom.e(), mth, inf.implName, aud, auditing, audits);
                        } else {
                            audits.add("return $L($L)", inf.implName, inf.inName);
                        }
                        dom.spec()
                                .addMethod(MethodSpec.overriding(mth)
                                        .addStatement(audits.build())
                                        .build())
                                .addMethod(MethodSpec.methodBuilder(inf.implName)
                                        .addParameters(mth.getParameters().stream().map(ParameterSpec::get).toList())
                                        .returns(TypeName.get(mth.getReturnType()))
                                        .addModifiers(Modifier.PROTECTED)
                                        .addStatement(body)
                                        .build());
                    }
                }
                //* create
                {
                    var act = c.create();
                    if (act != null) {
                        var mth = act.method().method();
                        var inf = CurdInfo.of(ctx, domain, act);
                        if (!ctx.rawAssignableTo(inf.inType, Accessor.Creator.class))
                            throw new IllegalStateException("CRUD create should must have input of CURD.Creator " + mth);
                        if (inf.retOpt)
                            throw new IllegalStateException("CRUD create should not returns Optional " + mth);
                        var body = CodeBlock.builder();
                        body.indent().add("return  $L.applyValidateFuture()", inf.inName).indent();
                        inf.buildAuthorize(body);
                        if (inf.nothing)
                            body.add("\n").add(".flatMap(v->$L($L).justPut(v.actor().orElse(-1L),$T.$L(v).asJson()))",
                                    storeName, sql ? "null" : ""
                                    , inf.entity, inf.copier
                            );
                        else
                            body.add("\n").add(".flatMap(v->$L($L).put(v.actor().orElse(-1L),$T.$L(v).asJson()))",
                                    storeName, sql ? "null" : ""
                                    , inf.entity, inf.copier
                            );

                        var audits = CodeBlock.builder();
                        var aud = act.method().annotationValues(Auditing.class).map(a -> new Audited(a, aqn, mth)).orElse(null);
                        if (aud != null) {
                            processAudit(domain, dom.e(), mth, inf.implName, aud, auditing, audits);
                        } else {
                            audits.add("return $L($L)", inf.implName, inf.inName);
                        }
                        dom.spec()
                                .addMethod(MethodSpec.overriding(mth)
                                        .addStatement(audits.build())
                                        .build())
                                .addMethod(MethodSpec.methodBuilder(inf.implName)
                                        .addParameters(mth.getParameters().stream().map(ParameterSpec::get).toList())
                                        .returns(TypeName.get(mth.getReturnType()))
                                        .addModifiers(Modifier.PROTECTED)
                                        .addStatement(body.unindent().build())
                                        .build());
                    }
                }
                //* remove
                {
                    var act = c.remove();
                    if (act != null) {
                        var mth = act.method().method();
                        var inf = CurdInfo.of(ctx, domain, act);
                        if (!ctx.rawAssignableTo(inf.inType, Accessor.Remover.class))
                            throw new IllegalStateException("CRUD create should must have input of CURD.Remover");
                        if (inf.retOpt)
                            throw new IllegalStateException("CRUD remover should not returns Optional" + mth);
                        if (!act.copierStrategy().isBlank())
                            throw new IllegalStateException("CRUD remover should not have copier: " + mth);
                        if (!inf.nothing) throw new IllegalStateException("CRUD remover should returns Void: " + mth);
                        var body = CodeBlock.builder();
                        body.indent().add("return  $L.applyValidateFuture()", inf.inName).indent();
                        inf.buildAuthorize(body);
                        body.add("\n").add(".flatMap(v->$L($L).remove(v.actor().orElse(-1L),v.id(),v.version()))",
                                storeName, sql ? "null" : ""
                        );
                        var audits = CodeBlock.builder();
                        var aud = act.method().annotationValues(Auditing.class).map(a -> new Audited(a, aqn, mth)).orElse(null);
                        if (aud != null) {
                            processAudit(domain, dom.e(), mth, inf.implName, aud, auditing, audits);
                        } else {
                            audits.add("return $L($L)", inf.implName, inf.inName);
                        }
                        dom.spec()
                                .addMethod(MethodSpec.overriding(mth)
                                        .addStatement(audits.build())
                                        .build())
                                .addMethod(MethodSpec.methodBuilder(inf.implName)
                                        .addParameters(mth.getParameters().stream().map(ParameterSpec::get).toList())
                                        .returns(TypeName.get(mth.getReturnType()))
                                        .addModifiers(Modifier.PROTECTED)
                                        .addStatement(body.unindent().build())
                                        .build());
                    }
                }
                //* update
                {
                    var acts = c.update();
                    if (acts != null && !acts.isEmpty()) {
                        for (var act : acts) {
                            var mth = act.method().method();
                            var inf = CurdInfo.of(ctx, domain, act);
                            if (!ctx.rawAssignableTo(inf.inType, Accessor.Modificator.class))
                                throw new IllegalStateException("CRUD update should must have input of CURD.Modificator");
                            if (inf.retOpt) throw new IllegalStateException("CRUD update should not returns Optional");
                            var body = CodeBlock.builder();
                            body.indent().add("return  $L.applyValidateFuture()", inf.inName).indent();
                            inf.buildAuthorize(body);
                            if (inf.nothing)
                                body.add("\n").add(".flatMap(v->$L($L).justSet(v.actor().orElse(-1L),v.id(),v.version(),$T.$L(v).asJson()))",
                                        storeName, sql ? "null" : ""
                                        , inf.entity, inf.copier);
                            else
                                body.add("\n").add(".flatMap(v->$L($L).set(v.actor().orElse(-1L),v.id(),v.version(),$T.$L(v).asJson()))",
                                        storeName, sql ? "null" : ""
                                        , inf.entity, inf.copier);
                            var audits = CodeBlock.builder();
                            var aud = act.method().annotationValues(Auditing.class).map(a -> new Audited(a, aqn, mth)).orElse(null);
                            if (aud != null) {
                                processAudit(domain, dom.e(), mth, inf.implName, aud, auditing, audits);
                            } else {
                                audits.add("return $L($L)", inf.implName, inf.inName);
                            }
                            dom.spec()
                                    .addMethod(MethodSpec.overriding(mth)
                                            .addStatement(audits.build())
                                            .build())
                                    .addMethod(MethodSpec.methodBuilder(inf.implName)
                                            .addParameters(mth.getParameters().stream().map(ParameterSpec::get).toList())
                                            .returns(TypeName.get(mth.getReturnType()))
                                            .addModifiers(Modifier.PROTECTED)
                                            .addStatement(body.unindent().build())
                                            .build());
                        }
                    }
                }
            }
        });

        if (auditing.get() != 0) {
            var audit = auditing.get();
            if (audit != 0) {
                if ((audit & (1 << Auditing.Mode.INVOKE.ordinal())) != 0 || (audit & (1 << Auditing.Mode.FAILURE.ordinal())) != 0) {
                    var f = FieldSpec.builder(AUDIT_INVOKE, "_AuditInvokeAuditor", Modifier.FINAL, Modifier.PROTECTED).build();
                    fields.add(f);
                    dom.spec().addField(f);
                    ctor.addStatement("_AuditInvokeAuditor=new $T($T.publish($T.class,null,vertx))", AUDIT_INVOKE, PubSub.class, AUDIT_INVOKE_EVENT);
                }
                if ((audit & (1 << Auditing.Mode.REQUEST.ordinal())) != 0) {
                    var f = FieldSpec.builder(AUDIT_REQUEST, "_AuditRequestAuditor", Modifier.FINAL, Modifier.PROTECTED).build();
                    fields.add(f);
                    dom.spec().addField(f);
                    ctor.addStatement("_AuditRequestAuditor=new $T($T.publish($T.class,null,vertx))", AUDIT_REQUEST, PubSub.class, AUDIT_REQUEST_EVENT);
                }
                if ((audit & (1 << Auditing.Mode.RESPONSE.ordinal())) != 0) {
                    var f = FieldSpec.builder(AUDIT_RESPONSE, "_AuditResponseAuditor", Modifier.FINAL, Modifier.PROTECTED).build();
                    fields.add(f);
                    dom.spec().addField(f);
                    ctor.addStatement("_AuditResponseAuditor=new $T($T.publish($T.class,null,vertx))", AUDIT_RESPONSE, PubSub.class, AUDIT_RESPONSE_EVENT);
                }
            }
        }
        dom.spec()
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PROTECTED)
                        .addParameters(dom.parameters())
                        .addStatement("super(vertx,address==null?$T.class.getCanonicalName():address,log)", dom.activities())
                        .addCode(ctorInitialize.build())
                        .addCode(ctor.build())
                        .addCode(web
                                ? CodeBlock.builder().addStatement("this.routing(web.apply(this))").build()
                                : CodeBlock.builder().build())
                        .build())
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PROTECTED)
                        .addStatement("super()")
                        .addCode(fields.stream().reduce(CodeBlock.builder(),
                                (c, p) -> c.addStatement("this.$L=null", p.name()),
                                (c0, c1) -> c0.add(c1.build())).build())
                        .build());
        if (web) {
            dom.spec().addMethod(MethodSpec.methodBuilder("routing")
                    .addJavadoc("@implNote routing method invokes at end of super class initialization")
                    .addParameter(Web.class, "web")
                    .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                    .build());
        }
        domain.put(ElementType.DOMAIN, dom);
    }

    static void processAudit(Domain domain, TypeElement e, ExecutableElement mth, String implName, Audited audit, AtomicInteger auditing, CodeBlock.Builder auditCode) {
        auditing.set(auditing.get() | (1 << audit.mode().ordinal()));
        var ctx = domain.ctx();
        var inParam = Optional.ofNullable(mth.getParameters()).filter(x -> !x.isEmpty()).map(List::getFirst).orElse(null);
        var inName = inParam == null ? null : inParam.getSimpleName().toString();
        var in = inParam == null ? null : inParam.asType();
        var inNullable = ctx.nullable(inParam) || ctx.nullable(in);
        var outType = ctx.futureContent(mth.getReturnType());
        var outNullable = ctx.nullable(outType);
        var outOpt = ctx.maybeOptional(outType) != null;
        if (outOpt) outType = ctx.maybeOptional(outType);
        assert outType != null;
        outNullable = outOpt || outNullable || ctx.nullable(outType);
        var topic = audit.topic();
        var input = CodeBlock.builder();
        if (in != null) {
            var conv = domain.lookupAuditing(e, in);
            conv.input(input, inName, inNullable);
        } else {
            input.add("null");
        }
        if (ctx.isVoid(outType)) outType = null;
        var out = CodeBlock.builder();
        if (outType != null) {
            var conv = domain.lookupAuditing(e, outType);
            conv.output(out, outOpt, outNullable);
        } else {
            out.add("$T.identity()", Function.class);
        }
        var actor = in != null && ctx.rawAssignableTo(in, Data.Request.class) ? CodeBlock.of("$L.actor().orElse(null)", inName) : CodeBlock.of("null");
        switch (audit.mode()) {
            case REQUEST -> auditCode.add("return _AuditRequestAuditor.invoke($S,$L,$L,$L($L))"
                    , topic, actor, input.build(), implName, inName);
            case RESPONSE -> auditCode.add("return _AuditResponseAuditor.invoke($S,$L,$L,$L($L))"
                    , topic, actor, out.build(), implName, inName);
            case INVOKE -> auditCode.add("return _AuditInvokeAuditor.invoke($S,$L,$L,$L,$L($L))"
                    , topic, actor, input.build(), out.build(), implName, inName);
            case FAILURE -> auditCode.add("return _AuditInvokeAuditor.failure($S,$L,$L,$L($L))"
                    , topic, actor, input.build(), implName, inName);
        }
    }

    record CurdInfo(
            String implName,
            String inName,
            TypeMirror inType,
            TypeMirror retType,
            boolean retOpt,
            boolean nothing,
            String copier,
            ClassName entity,
            AuthorizeInfo authorize

    ) {
        static CurdInfo of(Context ctx, Domain domain, StoreCurdMethod act) {
            var mth = act.method().method();
            var implName = AUDIT_IMPL + CaseConv.caption(mth.getSimpleName());
            var inName = mth.getParameters().getFirst().getSimpleName().toString();
            var inType = mth.getParameters().getFirst().asType();
            var ret = mth.getReturnType();
            var rawType = ctx.futureContent(ret);
            var opt = ctx.maybeOptional(rawType) != null;
            var nothing = ctx.isAnyVoid(rawType);
            var copier = inType.getKind().isPrimitive()
                    ? ""
                    : act.copierStrategy().isBlank() ? Domain.COPY_PREFIX + ((DeclaredType) inType).asElement().getSimpleName() : act.copierStrategy();
            var entity = act.entity();
            var et = ctx.typeElementOf(entity);
            if (et == null) throw new IllegalStateException("missing entity: " + entity);
            var entityName = ClassName.get(ctx.packageOf(et).getQualifiedName().toString(), et.getSimpleName() + "Data");
            return new CurdInfo(
                    implName, inName, inType, rawType, opt, nothing, copier, entityName, act.authorize());
        }

        public void buildAuthorize(CodeBlock.Builder body) {
            if (authorize != null) authorize.buildAuthorize(body);
        }
    }


    static <T> T addList(List<T> lst, T t) {
        lst.add(t);
        return t;
    }


    private void forData(TypeElement e, Domain domain) {
        if (e.getKind() == ElementKind.INTERFACE) {
            var u = data(e, domain);
            addObject(domain, e, u.resolved());
            domain.put(ElementType.DATA, u);
        } else if (e.getKind() == ElementKind.RECORD) {
            addData(domain, e, e.getEnclosedElements()
                    .stream()
                    .filter(x -> x.getKind() == ElementKind.RECORD_COMPONENT)
                    .toList()
            );
        } else throw new IllegalStateException("data should be interface or record only:" + e);

    }


}

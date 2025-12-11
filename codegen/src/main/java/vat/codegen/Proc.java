package vat.codegen;

import com.palantir.javapoet.*;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.jetbrains.annotations.Nullable;
import vat.api.Data;
import vat.api.DomainError;
import vat.api.Entity;
import vat.api.implement.Codec;
import vat.api.implement.Stored;
import vat.api.meta.Computed;
import vat.api.store.Field;
import vat.api.store.Model;
import vat.api.utils.Buf;
import vat.api.utils.Fn;
import vat.codegen.utils.*;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static vat.codegen.utils.Domain.StoreFieldType;

/// @author Zen.Liu
/// @since 2025-10-26

public interface Proc {
    default int order() {
        return 0;
    }

    default String name() {
        return this.getClass().getSimpleName();
    }

    Set<Class<? extends Annotation>> accept();


    void accept(Context ctx);


    default Builders.Storage store(TypeElement e, Domain domain) {
        return Builders
                .storage(data(e, domain))
                .accept(s -> {
                    var factory = CodeBlock.builder().add("(m)->new $T<?>[]{\n", Field.class);
                    var methods = new ArrayList<MethodSpec>();
                    s.definedColumns()
                            .forEach(col -> {
                                col.codec().make(factory, "m", col.name(), col.property(), col.interceptor());
                                factory.add(",\n");
                                methods.add(MethodSpec
                                        .methodBuilder(col.property())
                                        .addModifiers(Modifier.PUBLIC)
                                        .returns(col.codec().type())
                                        .addStatement("return field($L)", col.index())
                                        .build());
                            });
                    factory.add("}");
                    var store = s.store();
                    var idType = TypeName.get(store.typedColumns().get(ColumnType.IDENTITY).type());
                    var entityType = ClassName.get(s.data().e().asType());
                    var storeType = store.type();
                    store.spec()
                            .superclass(ParameterizedTypeName.get(ClassName.get(Model.Base.class), idType, entityType, storeType))
                            .addMethods(methods)
                            .addField(FieldSpec.builder(
                                            ParameterizedTypeName.get(ClassName.get(Function.class),
                                                    Domain.StoreModelType,
                                                    ArrayTypeName.of(StoreFieldType))
                                            , "FACTORY")
                                    .addModifiers(Modifier.FINAL, Modifier.STATIC, Modifier.PUBLIC)
                                    .initializer(factory.build())
                                    .build())
                            .addMethod(MethodSpec.constructorBuilder()
                                    .addModifiers(Modifier.PUBLIC)
                                    .addParameter(String.class, "schema")
                                    .addStatement("super(schema,$S,$T::new,$L,$L,$L,$L,$L,$L,$L,$L)",
                                            s.store().table(),
                                            s.data().type(),
                                            s.store().indexOf(ColumnType.IDENTITY),
                                            s.store().indexOf(ColumnType.VERSION),
                                            s.store().indexOf(ColumnType.REMOVED),
                                            s.store().indexOf(ColumnType.CREATOR),
                                            s.store().indexOf(ColumnType.CREATED),
                                            s.store().indexOf(ColumnType.MODIFIER),
                                            s.store().indexOf(ColumnType.MODIFIED),
                                            s.store().indexOf(ColumnType.HISTORY)
                                    )
                                    .build());
                    if (store.schema() == null) {
                        store.spec().addMethod(MethodSpec.constructorBuilder()
                                .addModifiers(Modifier.PUBLIC)
                                .addStatement("this(null)")
                                .build());
                    } else {
                        store.spec().addMethod(MethodSpec.constructorBuilder()
                                .addModifiers(Modifier.PUBLIC)
                                .addStatement("this($S)", store.schema())
                                .build());
                    }
                    store.spec()
                            .addMethod(MethodSpec.methodBuilder("_self")
                                    .addAnnotation(Override.class)
                                    .addModifiers(Modifier.PROTECTED)
                                    .returns(storeType)
                                    .addStatement("return this")
                                    .build())
                            .addMethod(MethodSpec.methodBuilder("buildFields")
                                    .addAnnotation(Override.class)
                                    .addModifiers(Modifier.PROTECTED)
                                    .returns(ArrayTypeName.of(StoreFieldType))
                                    .addStatement("return FACTORY.apply(this)")
                                    .build())
                            .addMethod(MethodSpec.methodBuilder("copy")
                                    .addModifiers(Modifier.PROTECTED)
                                    .addAnnotation(Override.class)
                                    .addParameter(ParameterSpec
                                            .builder(String.class, "schema")
                                            .addAnnotation(Nullable.class)
                                            .build())
                                    .returns(storeType)
                                    .addStatement("return schema==null?new $T(_schema):new $T(schema)",
                                            storeType, storeType)
                                    .build())
                            .addField(FieldSpec.builder(ParameterizedTypeName.get(ClassName.get(Class.class), entityType), "TYPE")
                                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                                    .initializer("$T.class", entityType)
                                    .build())
                            .addField(FieldSpec.builder(storeType, "MODEL")
                                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                                    .initializer("new $T()", storeType)
                                    .build())
                            .addField(FieldSpec.builder(
                                            ParameterizedTypeName.get(ClassName.get(Stored.Storage.class), idType, entityType, storeType),
                                            "STORAGE")
                                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                                    .initializer("(q,d,s)->MODEL.copy(s)._with(q,d)")
                                    .build());
                });
    }


    default Builders.Data data(TypeElement e, Domain domain) {
        var ctx = domain.ctx();
        return Builders.data(domain, e)
                .accept(data -> data
                        .pojo(p -> {
                            p.ctor().addStatement("super()");
                            p.asJson().addStatement("var json=new $T()", JsonObject.class);
                        })
                        .definedProperties(s -> s.forEach(f -> {
                            var validators = f.validators();
                            var interceptors = f.interceptors();
                            var ctorValidator = f.constructValidators();
                            var ctorIntercept = f.constructInterceptors();

                            var rawType = f.rawType();
                            var jsonProperty = data.jsonName();
                            var jsonKey = f.preferName();
                            var opt = f.opt();
                            var holder = f.codec().holder();
                            var codec = f.codec();
                            var holderField = codec.name();
                            var getter = f.method();
                            var getterName = getter.getSimpleName();
                            var getterReturn = f.type();
                            var property = f.name();
                            var prefer = f.prefer();
                            var isJs = f.toJs();
                            var isResolved = f.resolved();
                            var vJson = f.vField();
                            var vJsonKey = f.vKey();
                            //* not virtual
                            data
                                    //* for none-virtual property
                                    .accept(vJson == null, dt -> dt
                                            .pojo(p -> {
                                                p.copy().addStatement("this.$L=other.$L", property, property);
                                                p.ctor().addStatement("this.$L=$T.$L.get(json,$L)", property, holder, holderField, prefer);
                                                ctorValidator.forEach(i -> p.ctor()
                                                        .addStatement("$T.$L.accept(this.$L)", i.type(), i.field(), property));
                                                ctorIntercept.forEach(i -> p.ctor()
                                                        .addStatement("this.$L=$T.$L.apply(this.$L)", property, i.type(), i.field(), property));
                                                p.asJson().addStatement("$T.$L.set(json,$S,this.$L)", holder, holderField, property, property);
                                                CodeBlock validate;
                                                if (validators.isEmpty() && interceptors.isEmpty()) {
                                                    validate = CodeBlock.builder()
                                                            .addStatement("this.$L=v", property)
                                                            .addStatement("return this")
                                                            .build();
                                                } else {
                                                    var c = CodeBlock.builder();
                                                    validators.forEach(i -> c.addStatement("$T.$L.accept(v)", i.type(), i.field()));
                                                    c.addStatement("this.$L=v", property);
                                                    interceptors.forEach(
                                                            i -> c.addStatement("this.$L=$T.$L.apply(this.$L)",
                                                                    property, i.type(), i.field(),
                                                                    property));
                                                    validate = c.addStatement("return this")
                                                            .build();
                                                }
                                                p.spec()
                                                        .addField(TypeName.get(rawType), property, Modifier.PROTECTED)
                                                        .addMethod(
                                                                (isResolved
                                                                        ? MethodSpec.methodBuilder(getterName.toString())
                                                                        .addAnnotation(Override.class)
                                                                        .returns(TypeName.get(getterReturn))
                                                                        : MethodSpec.overriding(getter)
                                                                )
                                                                        .addModifiers(Modifier.PUBLIC)
                                                                        .addCode(opt
                                                                                ? CodeBlock.builder().addStatement("return $T.ofNullable($L)", Optional.class, property).build()
                                                                                : CodeBlock.builder().addStatement("return $L", property).build())
                                                                        .build())
                                                        .addMethod(MethodSpec.methodBuilder(property)
                                                                .addModifiers(Modifier.PUBLIC)
                                                                .addParameter(opt
                                                                        ? ParameterSpec.builder(TypeName.get(rawType), "v").addAnnotation(vat.api.meta.Nullable.class).build()
                                                                        : ParameterSpec.builder(TypeName.get(rawType), "v").build())
                                                                .returns(p.type())
                                                                .addCode(validate)
                                                                .build());
                                            })
                                            .accept(d -> {
                                                if (ctorValidator.size() == 1) {
                                                    ctorValidator.forEach(i -> d.ctor().addStatement("$T.$L.accept($T.$L.get($L,$L))",
                                                            i.type(), i.field(),
                                                            holder, holderField,
                                                            jsonProperty, prefer));
                                                } else if (ctorValidator.size() > 1) {
                                                    d.ctor().add("{\n");
                                                    d.ctor().addStatement("var v=$T.$L.get($L,$L)",
                                                            holder, holderField,
                                                            jsonProperty, prefer);
                                                    ctorValidator.forEach(i -> d.ctor().addStatement("$T.$L.accept(v)", i.type(), i.field()));
                                                    d.ctor().add("}\n");
                                                }
                                                if (ctorIntercept.size() == 1) ctorIntercept.forEach(
                                                        i -> d.ctor().addStatement("$T.$L.set($L,$S,$T.$L.apply($T.$L.get($L,$L)))",
                                                                holder, holderField,
                                                                jsonKey, jsonProperty,
                                                                i.type(), i.field(),
                                                                holder, holderField,
                                                                jsonProperty, prefer
                                                        ));
                                                else if (ctorIntercept.size() > 1) {
                                                    d.ctor().add("{\n");
                                                    d.ctor().addStatement("var v=$T.$L.get($L,$L)",
                                                            holder, holderField,
                                                            jsonProperty, prefer);
                                                    ctorIntercept.forEach(i -> d.ctor().addStatement("v=$T.$L.apply(v)", i.type(), i.field()));
                                                    d.ctor().addStatement("$T.$L.set($L,$S,v)",
                                                            holder, holderField,
                                                            jsonProperty, prefer);
                                                    d.ctor().add("}\n");
                                                }
                                                CodeBlock validate;
                                                if (validators.isEmpty() && interceptors.isEmpty()) {
                                                    validate = CodeBlock.builder()
                                                            .addStatement("$T.$L.set(this.$L,$S,v)",
                                                                    holder, holderField,
                                                                    jsonProperty, jsonKey)
                                                            .addStatement("return this")
                                                            .build();
                                                } else {
                                                    var c = CodeBlock.builder();
                                                    validators.forEach(i -> c.addStatement("$T.$L.accept(v)", i.type(), i.field()));
                                                    interceptors.forEach(i -> c.addStatement("v=$T.$L.apply(v)", i.type(), i.field()));
                                                    validate = c
                                                            .addStatement("$T.$L.set(this.$L,$S,v)", holder, holderField, jsonProperty, jsonKey)
                                                            .addStatement("return this")
                                                            .build();
                                                }
                                                d.spec()
                                                        .addMethod(
                                                                (isResolved
                                                                        ? MethodSpec.methodBuilder(getterName.toString())
                                                                        .addAnnotation(Override.class)
                                                                        .returns(TypeName.get(getterReturn))
                                                                        : MethodSpec.overriding(getter)
                                                                )
                                                                        .addModifiers(Modifier.PUBLIC)
                                                                        .addCode(opt
                                                                                ? CodeBlock.builder().addStatement("return $T.ofNullable($T.$L.get(this.$L,$L))",
                                                                                Optional.class,
                                                                                holder, holderField,
                                                                                jsonProperty, prefer).build()
                                                                                : CodeBlock.builder().addStatement("return $T.$L.get(this.$L,$L)",
                                                                                holder, holderField,
                                                                                jsonProperty, prefer).build())
                                                                        .build())
                                                        .addMethod(MethodSpec.methodBuilder(property)
                                                                .addModifiers(Modifier.PUBLIC)
                                                                .addParameter(opt
                                                                        ? ParameterSpec.builder(TypeName.get(rawType), "v").addAnnotation(vat.api.meta.Nullable.class).build()
                                                                        : ParameterSpec.builder(TypeName.get(rawType), "v").build())
                                                                .returns(d.type())
                                                                .addCode(validate)
                                                                .build());
                                            }))
                                    //* for virtual property
                                    .accept(vJson != null, dt -> dt
                                            .pojo(p -> p.spec()
                                                    .addMethod(MethodSpec.overriding(getter)
                                                            .addModifiers(Modifier.PUBLIC)
                                                            .addStatement("return $T.$L.get(this.$L,$S)", holder, holderField, vJson, vJsonKey)
                                                            .build())
                                                    .addMethod(MethodSpec.methodBuilder(property)
                                                            .addModifiers(Modifier.PUBLIC)
                                                            .addParameter(ParameterSpec.builder(TypeName.get(rawType), "v").build())
                                                            .returns(p.type())
                                                            .addStatement("$T.$L.set(this.$L,$S,v)", holder, holderField, vJson, vJsonKey)
                                                            .addStatement("return this")
                                                            .build()))
                                            .accept(d -> {
                                                dt.spec()
                                                        .addMethod(
                                                                (isResolved
                                                                        ? MethodSpec.methodBuilder(getterName.toString())
                                                                        .addAnnotation(Override.class)
                                                                        .returns(TypeName.get(getterReturn))
                                                                        : MethodSpec.overriding(getter))
                                                                        .addModifiers(Modifier.PUBLIC)
                                                                        .addCode(opt
                                                                                ? CodeBlock.builder()
                                                                                .addStatement("return $T.ofNullable($T.$L.get(this.$L.getJsonObject($S),$S))",
                                                                                        Optional.class, holder, holderField,
                                                                                        jsonProperty, vJson, vJsonKey)
                                                                                .build()
                                                                                : CodeBlock.builder()
                                                                                .addStatement("return $T.$L.get(this.$L.getJsonObject($S),$S)",
                                                                                        holder, holderField,
                                                                                        jsonProperty, vJson, vJsonKey)
                                                                                .build()
                                                                        )
                                                                        .build())
                                                        .addMethod(MethodSpec.methodBuilder(f.name())
                                                                .addModifiers(Modifier.PUBLIC)
                                                                .addParameter(opt
                                                                        ? ParameterSpec.builder(TypeName.get(rawType), "v").addAnnotation(vat.api.meta.Nullable.class).build()
                                                                        : ParameterSpec.builder(TypeName.get(rawType), "v").build())
                                                                .returns(d.type())
                                                                .addCode(CodeBlock.builder()
                                                                        .addStatement(
                                                                                "this.$L.put($S,$T.$L.set(this.$L.getJsonObject($S),$S,v))",
                                                                                jsonProperty, vJson,
                                                                                holder, holderField,
                                                                                jsonProperty, vJson, vJsonKey)
                                                                        .addStatement("return this")
                                                                        .build())
                                                                .build());
                                            }))
                                    //* processor helper
                                    .accept(d -> {
                                        processor(ctx, d.spec(), d.type(), rawType, opt, property);
                                        d.pojo(p -> processor(ctx, p.spec(), p.type(), rawType, opt, property));
                                    })
                                    //* toJS
                                    .accept(isJs, dt -> dt
                                            .pojo(p -> p.asJs((to, from) -> {
                                                to.addStatement("$T.toJs(js,$T::getLong,$L)", Codec.class, JsonObject.class, prefer);
                                                from.addStatement("$T.fromJs(js,$T::getString,$T::parseLong,$L)", Codec.class, JsonObject.class, Long.class, prefer);
                                            }))
                                            .asJs((to, from) -> {
                                                to.addStatement("$T.toJs(js,$T::getLong,$L)", Codec.class, JsonObject.class, prefer);
                                                from.addStatement("$T.fromJs(js,$T::getString,$T::parseLong,$L)", Codec.class, JsonObject.class, Long.class, prefer);
                                            })
                                    )
                                    //* toBuf
                                    .accept(data.binary() && vJson == null, dt -> dt
                                            .pojo(p -> p.asBin((to, from) -> {
                                                assert codec.binaryName() != null : "unsupported binary type: " + codec;
                                                from.addStatement("this.$L=$T.$L.read(buf)", f.name(), codec.holder(), codec.binaryName());
                                                to.addStatement("$T.$L.write(buf,this.$L)", codec.holder(), codec.binaryName(), property);
                                            }))
                                            .asBin((to, from) -> {
                                                assert codec.binaryName() != null : "unsupported binary type: " + f;
                                                from.addStatement("this.$L($T.$L.read(buf))", property, codec.holder(), codec.binaryName());
                                                to.addStatement("$T.$L.write(buf,this.$L())", codec.holder(), codec.binaryName(), property);
                                            })
                                    )
                                    //* compute copier
                                    .accept(!data.copier().isEmpty(), d -> d.computeCopier(f));
                        }))
                        .computedProperties(s -> s.forEach(f -> data.pojo(p -> p
                                        .computes()
                                        .beginControlFlow("if(!json.containsKey($S))", f.preferName())
                                        .addStatement("$T.$L.set(json,$S,$T.super.$L()$L)", f.codec().holder(),
                                                f.codec().name(), f.preferName(), e.asType(),
                                                f.getter().getSimpleName(), f.opt() ? ".orElse(null)" : "")
                                        .endControlFlow())
                                .computes()
                                .beginControlFlow("if(!$L.containsKey($S))", data.jsonName(), f.preferName())
                                .addStatement("$T.$L.set($L,$S,$T.super.$L()$L)", f.codec().holder(),
                                        f.codec().name(), data.jsonName(), f.preferName(), e.asType(),
                                        f.getter().getSimpleName(), f.opt() ? ".orElse(null)" : "")
                                .endControlFlow()))
                        //* constructors
                        .accept(d -> {
                            var record = d.record();
                            var codec = domain.preferCodec(d.internal());
                            var binary = d.binary();
                            var validate = d.request() || d.validation();
                            //* base methods
                            {
                                d.spec()
                                        .addMethod(record
                                                ? MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC)
                                                .addStatement("this(new $T())", JsonObject.class)
                                                .build()
                                                : MethodSpec.constructorBuilder()
                                                .addParameter(ParameterSpec.builder(ClassName.get(JsonObject.class), "j").build())
                                                .addModifiers(Modifier.PUBLIC)
                                                .addStatement("super(j)")
                                                .addCode(d.ctor().build())
                                                .build());
                                if (record && !d.ctor().isEmpty()) d.spec()
                                        .addMethod(MethodSpec.compactConstructorBuilder()
                                                .addModifiers(Modifier.PUBLIC)
                                                .addCode(CodeBlock.builder()
                                                        .beginControlFlow("if(!asJson.isEmpty())")
                                                        .add(d.ctor().build())
                                                        .endControlFlow()
                                                        .build())
                                                .build());
                                d.pojo(p -> {
                                    p.spec()
                                            .addMethod(MethodSpec.constructorBuilder()
                                                    .addParameter(
                                                            ParameterSpec.builder(ClassName.get(JsonObject.class), "json").build())
                                                    .addModifiers(Modifier.PUBLIC)
                                                    .addCode(p.ctor().build())
                                                    .build())
                                            .addMethod(MethodSpec.constructorBuilder()
                                                    .addParameter(p.type(), "other")
                                                    .addModifiers(Modifier.PUBLIC)
                                                    .addCode(p.copy().build())
                                                    .build());
                                    codec.addField(
                                                    FieldSpec.builder(ParameterizedTypeName.get(ClassName.get(Codec.DataCodec.class), p.type(), TypeName.get(e.asType())),
                                                                    Context.PASCAL_UPPER_SNAKE.apply(p.name()))
                                                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                                                            .initializer("$T.closure($T::new,$T.class)", Codec.DataCodec.class, p.type(), p.type())
                                                            .build())
                                            .addField(FieldSpec.builder(ParameterizedTypeName.get(ClassName.get(Codec.JsDecoder.class), p.type()), CaseConv.PASCAL_UPPER_SNAKE.apply(p.name()) + Domain.CODEC_JS_SUFFIX)
                                                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                                                    .initializer("$T::new", p.type())
                                                    .build());
                                });
                                codec
                                        .addField(
                                                FieldSpec.builder(ParameterizedTypeName
                                                                        .get(ClassName.get(Codec.DataCodec.class),
                                                                                d.type(),
                                                                                TypeName.get(e.asType())),
                                                                CaseConv.PASCAL_UPPER_SNAKE.apply(d.name()))
                                                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                                                        .initializer("$T.closure($T::new,$T.class)", Codec.DataCodec.class, d.type(), d.type())
                                                        .build())
                                        .addField(FieldSpec.builder(ParameterizedTypeName.get(ClassName.get(Codec.JsDecoder.class), d.type()),
                                                        CaseConv.PASCAL_UPPER_SNAKE.apply(d.name()) + Domain.CODEC_JS_SUFFIX)
                                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                                                .initializer("$T::new", d.type())
                                                .build());
                            }
                            //* compute and asJson
                            if (!d.computes().isEmpty()) {
                                d.pojo(p -> p.spec().addMethod(MethodSpec.methodBuilder("asJson")
                                                .addAnnotation(Override.class)
                                                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                                .returns(TypeName.get(JsonObject.class))
                                                .addCode(p.asJson().add(p.computes().build()).build())
                                                .addStatement("return json")
                                                .build()
                                        )).spec()
                                        .addMethod(MethodSpec.methodBuilder("asJson")
                                                .addAnnotation(Override.class)
                                                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                                .returns(JsonObject.class)
                                                .addCode(d.computes().build())
                                                .addStatement(record ? "return asJson" : "return this.asJson()")
                                                .build());
                            } else {
                                d.pojo(p -> p.spec().addMethod(MethodSpec.methodBuilder("asJson")
                                        .returns(TypeName.get(JsonObject.class))
                                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                        .addCode(p.asJson().addStatement("return json").build())
                                        .build()));
                            }
                            //* binary
                            if (binary) {
                                d.pojo(p -> {
                                            p.spec()
                                                    .addMethod(MethodSpec.methodBuilder("toBuf")
                                                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                                            .addAnnotation(Override.class)
                                                            .addParameter(Buf.class, "buf")
                                                            .returns(Buf.class)
                                                            .addCode(d.toBin().build())
                                                            .addStatement("return buf")
                                                            .build())
                                                    .addMethod(MethodSpec.constructorBuilder()
                                                            .addModifiers(Modifier.PUBLIC)
                                                            .addParameter(Buf.class, "buf")
                                                            .addStatement("super()")
                                                            .addCode(d.fromBin().build())
                                                            .build());
                                            codec
                                                    .addField(FieldSpec.builder(ParameterizedTypeName.get(ClassName.get(Codec.BinaryCodec.class), p.type())
                                                                    , Context.PASCAL_UPPER_SNAKE.apply(p.name()) + Domain.CODEC_BINARY_SUFFIX)
                                                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                                                            .initializer("$T::new", p.type())
                                                            .build());
                                        })
                                        .spec()
                                        .addMethod(MethodSpec.methodBuilder("toBuf")
                                                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                                .addAnnotation(Override.class)
                                                .addParameter(Buf.class, "buf")
                                                .returns(Buf.class)
                                                .addCode(d.toBin().build())
                                                .addStatement("return buf")
                                                .build())
                                        .addMethod(MethodSpec.constructorBuilder()
                                                .addModifiers(Modifier.PUBLIC)
                                                .addParameter(Buf.class, "buf")
                                                .addStatement(record ? "this(new JsonObject())" : "super()")
                                                .addCode(d.fromBin().build())
                                                .build());
                                codec
                                        .addField(FieldSpec.builder(ParameterizedTypeName.get(ClassName.get(Codec.BinaryCodec.class), d.type())
                                                        , Context.PASCAL_UPPER_SNAKE.apply(d.name()) + Domain.CODEC_BINARY_SUFFIX)
                                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                                                .initializer("$T::new", d.type())
                                                .build());
                            }
                            //* JS
                            if (!d.toJs().isEmpty()) {
                                d.pojo(p -> p.spec()
                                                .addMethod(MethodSpec.methodBuilder("toJS")
                                                        .addAnnotation(Override.class)
                                                        .returns(TypeName.get(JsonObject.class))
                                                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                                        .addStatement("var js=toJson()")
                                                        .addCode(p.toJs().build())
                                                        .addStatement("return js")
                                                        .build())
                                                .addMethod(MethodSpec.methodBuilder("fromJS")
                                                        .addParameter(JsonObject.class, "js")
                                                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                                        .returns(JsonObject.class)
                                                        .addCode(p.fromJs().build())
                                                        .addStatement("return js")
                                                        .build())
                                                .addMethod(MethodSpec.constructorBuilder()
                                                        .addParameter(JsonObject.class, "js")
                                                        .addParameter(Void.class, "ignore")
                                                        .addModifiers(Modifier.PUBLIC)
                                                        .addStatement("this($T.fromJS(js))", p.type())
                                                        .build()))
                                        .spec()
                                        .addMethod(MethodSpec.methodBuilder("toJS")
                                                .addAnnotation(Override.class)
                                                .returns(JsonObject.class)
                                                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                                .addStatement("var js=toJson()")
                                                .addCode(d.toJs().build())
                                                .addStatement("return js")
                                                .build())
                                        .addMethod(MethodSpec.methodBuilder("fromJS")
                                                .addParameter(JsonObject.class, "js")
                                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                                .returns(JsonObject.class)
                                                .addCode(d.fromJs().build())
                                                .addStatement("return js")
                                                .build())
                                        .addMethod(MethodSpec.constructorBuilder()
                                                .addParameter(JsonObject.class, "js")
                                                .addParameter(Void.class, "ignore")
                                                .addModifiers(Modifier.PUBLIC)
                                                .addStatement("this($T.fromJS(js))", d.type())
                                                .build());
                            } else {
                                d.pojo(p -> p.spec()
                                                .addMethod(MethodSpec.constructorBuilder()
                                                        .addParameter(JsonObject.class, "js")
                                                        .addParameter(Void.class, "ignore")
                                                        .addModifiers(Modifier.PUBLIC)
                                                        .addStatement("this(js)")
                                                        .build()))
                                        .spec()
                                        .addMethod(MethodSpec.constructorBuilder()
                                                .addParameter(JsonObject.class, "js")
                                                .addParameter(Void.class, "ignore")
                                                .addModifiers(Modifier.PUBLIC)
                                                .addStatement("this(js)")
                                                .build());
                            }
                            //* validate
                            if (validate) {
                                d.pojo(p -> p.spec()
                                                .addMethod(MethodSpec.methodBuilder("applyValidateFuture")
                                                        .returns(ParameterizedTypeName.get(ClassName.get(Future.class), TypeName.get(e.asType())))
                                                        .addModifiers(Modifier.PUBLIC)
                                                        .addAnnotation(Override.class)
                                                        .addStatement("return $T.future(p->{doValidate();p.complete(this);})")
                                                        .build())
                                                .addMethod(MethodSpec.methodBuilder("applyValidate")
                                                        .returns(TypeName.get(e.asType()))
                                                        .addModifiers(Modifier.PUBLIC)
                                                        .addAnnotation(Override.class)
                                                        .addStatement("doValidate()")
                                                        .addStatement("return this")
                                                        .build()))
                                        .spec()
                                        .addMethod(MethodSpec.methodBuilder("applyValidateFuture")
                                                .returns(ParameterizedTypeName.get(ClassName.get(Future.class), TypeName.get(e.asType())))
                                                .addModifiers(Modifier.PUBLIC)
                                                .addAnnotation(Override.class)
                                                .addStatement("return $T.future(p->{doValidate();p.complete(this);})", Future.class)
                                                .build())
                                        .addMethod(MethodSpec.methodBuilder("applyValidate")
                                                .returns(TypeName.get(e.asType()))
                                                .addModifiers(Modifier.PUBLIC)
                                                .addAnnotation(Override.class)
                                                .addStatement("doValidate()")
                                                .addStatement("return this")
                                                .build());
                            }
                            //* object accessor
                            {
                                accessor(ctx, e, d.spec(), d.type(), record);
                                d.pojo(p -> accessor(ctx, e, p.spec(), p.type(), false));
                            }
                        }))
                //* copier
                .accept(d -> {
                    if (d.copier().isEmpty()) return;
                    for (var entry : d.copier().entrySet()) {
                        var name = entry.getKey();
                        var define = entry.getValue();
                        var input = define.input();
                        var inputElement = Objects.requireNonNull(ctx.typeElementOf(input), () -> "missing input type " + input);
                        if (name.isBlank()) name = Domain.COPY_PREFIX + inputElement.getSimpleName();
                        var targets = d.resolved().stream()
                                //* filter out Entity Base fields.
                                .filter(x->!ctx.rawAssignableTo(x.method().getEnclosingElement().asType(), Entity.Base.class))
                                .collect(Collectors.toMap(ResolvedField::preferName, Function.identity()));
                        var sources = Domain.resolveDataFields(domain, input)
                                //* filter out Entity Base fields.
                                .filter(x->!ctx.rawAssignableTo(x.method().getEnclosingElement().asType(), Entity.Base.class))
                                .collect(Collectors.toMap(ResolvedField::name, Function.identity()));
                        var strategies = define.values()
                                .values()
                                .stream()
                                .collect(Collectors.toMap(Builders.CopyProcInfo::from, Function.identity()));
                        var code = CodeBlock.builder()
                                .addStatement("var in = i.asJson()")
                                .addStatement("var out = new $T(new $T<>(in.size()))", JsonObject.class, LinkedHashMap.class);
                                    /*
                                        O copyFromType(I i){
                                            var in=i.asJson();
                                            var out=JsonObject.of();
                                            //property copy and processing
                                            return new O(out);
                                        }
                                     */
                        for (var target : targets.values()) {
                            if (AnnotatedValue.find(target.method(), Computed.class).isPresent()) continue;
                            var strategy = strategies.get(target.name());
                            if (strategy == null) {
                                var src = Objects.requireNonNull(sources.get(target.name()), () -> "missing target field for " + target);
                                if (target.required())
                                    code.addStatement("out.put($S,$T.noneNull(in.getValue($S),()->$T.System.badRequest(\"missing value\")))",
                                            target.preferName(),
                                            Fn.class,
                                            src.preferName(),
                                            DomainError.class
                                    );
                                else
                                    code.addStatement("out.put($S,in.getValue($S))",
                                            target.preferName(),
                                            src.preferName());

                            } else {
                                var src = sources.get(strategy.from().isBlank() ? target.name() : strategy.from());
                                var def = strategy.withDefault();
                                var provide = strategy.provide();
                                var validator = strategy.validate();
                                var convert = strategy.convert();
                                if(src==null){
                                    if((!def)&&provide==null) throw new IllegalStateException("missing source field of "+target+",Provide with 'withDefault' or 'provide' to provide value for missing field");
                                    if(validator!=null||convert!=null) throw new IllegalStateException("missing source field of "+target+" not support Validate or Convert");
                                    if(provide!=null){
                                        provide.functionInfo(ctx).filter(x ->
                                                ctx.rawAssignableTo(x.v1, input) &&
                                                ctx.rawAssignableTo(x.v2, target.rawType())
                                        ).orElseThrow(() -> DomainError.System.badRequest("invalid provide function: {}", target));
                                        code.addStatement("$T.$L.set(out,$S,$T.$L.apply(i))",
                                                target.codec().holder(),target.codec().name(),
                                               target.preferName(),
                                                provide.holder(), provide.field()
                                        );
                                        continue;
                                    }
                                    code.addStatement("out.put($S,$T.empty()$L)",
                                            target.preferName(),
                                            Optional.class,
                                            defaultValue(target.rawType(), ctx));
                                    continue;
                                }
                                if (provide == null
                                    && validator == null
                                    && convert == null) {
                                    if (target.required())
                                        if (def) {
                                            code.addStatement("out.put($S,$T.ofNullable(in.getValue($S))$L)",
                                                    target.preferName(),
                                                    Optional.class,
                                                    src.preferName(),
                                                    defaultValue(target.rawType(), ctx));
                                        } else {
                                            code.addStatement("out.put($S,$T.noneNull(in.getValue($S),()->$T.System.badRequest(\"missing value\")))",
                                                    target.preferName(),
                                                    Fn.class,
                                                    src.preferName(),
                                                    DomainError.class
                                            );
                                        }

                                } else {
                                    code.add("{\n");
                                    if (provide != null) {
                                        provide.functionInfo(ctx).filter(x ->
                                                ctx.rawAssignableTo(x.v1, input) &&
                                                ctx.rawAssignableTo(x.v2, target.rawType())
                                        ).orElseThrow(() -> DomainError.System.badRequest("invalid provide function: {}", target));
                                        code.addStatement("var v=$T.ofNullable($T.$L.get(in,$S)).orElseGet(()->$T.$L.apply(i))",
                                                Optional.class,
                                                src.codec().holder(), src.codec().name(),
                                                src.preferName(),
                                                provide.holder(), provide.field()
                                        );
                                    } else
                                        code.addStatement("var v=$T.$L.get(in,$S)", src.codec().holder(), src.codec().name(), src.preferName());
                                    if (convert != null) {
                                        convert.biFunctionInfo(ctx).filter(x ->
                                                ctx.rawAssignableTo(x.v1, input) &&
                                                ctx.rawAssignableTo(x.v2, src.rawType()) &&
                                                ctx.rawAssignableTo(x.v3, target.rawType())
                                        ).orElseThrow(() -> DomainError.System.badRequest("invalid converter function: {}", target));
                                        code.addStatement("var v1=$T.$L.apply(i,v)", convert.holder(), convert.field());
                                    }
                                    if (validator != null) {
                                        validator.predicateInfo(ctx)
                                                .filter(x -> ctx.rawAssignableTo(x, target.rawType()))
                                                .orElseThrow(() -> DomainError.System.badRequest("invalid converter function: {}", target));
                                        code.addStatement("$T.$L.accept($L)", validator.holder(), validator.field(), convert != null ? "v1" : "v");
                                    }
                                    code.addStatement("$T.$L.set(out,$S,$L)",
                                            target.codec().holder(), target.codec().name(), target.preferName(), convert != null ? "v1" : "v"
                                    );
                                    code.add("}\n");
                                }
                            }
                        }
                        d.spec().addMethod(MethodSpec.methodBuilder(name)
                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                .addParameter(TypeName.get(input), "i")
                                .returns(d.type())
                                .addStatement("if(i==null) return null")
                                .addCode(code.addStatement("return new $T(out)", d.type()).build())
                                .build());

                    }
                })
                ;


    }

    Map<TypeName, CodeBlock> DEFAULTS = new ConcurrentHashMap<>();

    private CodeBlock defaultValue(TypeMirror t, Context ctx) {
        return DEFAULTS.computeIfAbsent(TypeName.get(t), x -> {
            if (x.isPrimitive()) return switch (t.getKind()) {
                case BOOLEAN -> CodeBlock.of(".orElse(false)");
                case BYTE -> CodeBlock.of(".orElse((byte)0)");
                case SHORT -> CodeBlock.of(".orElse((short)0)");
                case INT -> CodeBlock.of(".orElse(0)");
                case LONG -> CodeBlock.of(".orElse(0l)");
                case CHAR -> CodeBlock.of(".orElse('\\0')");
                case FLOAT -> CodeBlock.of(".orElse(0f)");
                case DOUBLE -> CodeBlock.of(".orElse(0.0)");
                default -> throw new IllegalStateException("unsupported primitive type " + t);
            };
            if (ctx.rawAssignableTo(t, JsonObject.class)) return CodeBlock.of(".orElseGet($T::of)", JsonObject.class);
            if (ctx.rawAssignableTo(t, JsonArray.class)) return CodeBlock.of(".orElseGet($T::of)", JsonArray.class);
            return CodeBlock.of(".orElse(null)");
        });
    }

    //endregion


    static void processor(Context ctx, TypeSpec.Builder type, TypeName typeType, TypeMirror fieldMirror, boolean opt, String name) {
        if (fieldMirror instanceof DeclaredType) {
            if (ctx.rawAssignableTo(fieldMirror, JsonObject.class))
                type.addMethod(MethodSpec.methodBuilder(name + "Do")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(ParameterizedTypeName.get(ClassName.get(Consumer.class),
                                TypeName.get(fieldMirror)), "act")
                        .returns(typeType)
                        .addCode(opt
                                ? CodeBlock.builder().addStatement("var x=$L().orElseGet($T::new)", name, JsonObject.class).build()
                                : CodeBlock.builder().addStatement("var x=$T.ofNullable($L()).orElseGet($T::new)", Optional.class, name, JsonObject.class).build()
                        )
                        .addStatement("act.accept(x)")
                        .addStatement("$L(x)", name)
                        .addStatement("return this")
                        .build());
            else if (ctx.rawAssignableTo(fieldMirror, JsonArray.class))
                type.addMethod(MethodSpec.methodBuilder(name + "Do")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(ParameterizedTypeName.get(ClassName.get(Consumer.class),
                                TypeName.get(fieldMirror)), "act")
                        .returns(typeType)
                        .addCode(opt
                                ? CodeBlock.builder().addStatement("var x=$L().orElseGet($T::new)", name, JsonArray.class).build()
                                : CodeBlock.builder().addStatement("var x=$T.ofNullable($L()).orElseGet($T::new)", Optional.class, name, JsonArray.class).build()
                        )

                        .addStatement("act.accept(x)")
                        .addStatement("$L(x)", name)
                        .addStatement("return this")
                        .build());
            else if (ctx.rawAssignableTo(fieldMirror, List.class))
                type.addMethod(MethodSpec.methodBuilder(name + "Do")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(ParameterizedTypeName.get(ClassName.get(Consumer.class),
                                TypeName.get(fieldMirror)), "act")
                        .returns(typeType)
                        .addCode(opt
                                ? CodeBlock.builder().addStatement("var x=$L().orElseGet($T::new)", name, ArrayList.class).build()
                                : CodeBlock.builder().addStatement("var x=$T.ofNullable($L()).orElseGet($T::new)", Optional.class,
                                name, ArrayList.class).build()
                        )
                        .addStatement("act.accept(x)")
                        .addStatement("$L(x)", name)
                        .addStatement("return this")
                        .build());
            else if (ctx.rawAssignableTo(fieldMirror, Set.class))
                type.addMethod(MethodSpec.methodBuilder(name + "Do")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(ParameterizedTypeName.get(ClassName.get(Consumer.class),
                                TypeName.get(fieldMirror)), "act")
                        .returns(typeType)
                        .addCode(opt
                                ? CodeBlock.builder().addStatement("var x=$L().orElseGet($T::new)", name, HashSet.class).build()
                                : CodeBlock.builder().addStatement("var x=$T.ofNullable($L()).orElseGet($T::new)", Optional.class,
                                name, HashSet.class).build()
                        )
                        .addStatement("act.accept(x)")
                        .addStatement("$L(x)", name)
                        .addStatement("return this")
                        .build());
            else if (ctx.rawAssignableTo(fieldMirror, Map.class))
                type.addMethod(MethodSpec.methodBuilder(name + "Do")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(ParameterizedTypeName.get(ClassName.get(Consumer.class),
                                TypeName.get(fieldMirror)), "act")
                        .returns(typeType)
                        .addCode(opt
                                ? CodeBlock.builder().addStatement("var x=$L().orElseGet($T::new)", name, HashMap.class).build()
                                : CodeBlock.builder().addStatement("var x=$T.ofNullable($L()).orElseGet($T::new)", Optional.class,
                                name, HashMap.class).build()
                        )
                        .addStatement("act.accept(x)")
                        .addStatement("$L(x)", name)
                        .addStatement("return this")
                        .build());
            else if (ctx.rawAssignableTo(fieldMirror, Data.class)) {
                type.addMethod(MethodSpec.methodBuilder(name + "Do")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(ParameterizedTypeName.get(ClassName.get(Consumer.class),
                                        TypeName.get(fieldMirror).annotated(
                                                AnnotationSpec.builder(
                                                                vat.api.meta.Nullable.class)
                                                        .build())),
                                "act")
                        .returns(typeType)
                        .addCode(opt
                                ? CodeBlock.builder().addStatement("var x=$L().orElse(null)", name).build()
                                : CodeBlock.builder().addStatement("var x=$L()", name).build()
                        )
                        .addStatement("act.accept(x)")
                        .addStatement("return $L(x)", name)
                        .build());
                type.addMethod(MethodSpec.methodBuilder(name + "Apply")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(ParameterizedTypeName.get(ClassName.get(UnaryOperator.class),
                                        TypeName.get(fieldMirror).annotated(
                                                AnnotationSpec.builder(
                                                                vat.api.meta.Nullable.class)
                                                        .build())),
                                "act")
                        .returns(typeType)
                        .addCode(opt
                                ? CodeBlock.builder().addStatement("var x=act.apply($L().orElse(null))", name).build()
                                : CodeBlock.builder().addStatement("var x=act.apply($L())", name).build()
                        )
                        .addStatement("return $L(x)", name)
                        .build());
            }


        }
    }

    static void accessor(Context ctx, TypeElement proto, TypeSpec.Builder type, TypeName typeType, boolean record) {
        if (record) {
            var tr = TypeVariableName.get("R");
            type.addMethod(MethodSpec.methodBuilder("apply")
                    .addModifiers(Modifier.PUBLIC)
                    .addTypeVariable(TypeVariableName.get("R"))
                    .addParameter(
                            ParameterizedTypeName.get(ClassName.get(Function.class), typeType, tr),
                            "m")
                    .returns(tr)
                    .addStatement("return m.apply(this)")
                    .build());

            type.addMethod(MethodSpec.methodBuilder("accept")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ParameterizedTypeName.get(ClassName.get(Consumer.class), typeType),
                            "m")
                    .returns(typeType)
                    .addStatement("m.accept(this)")
                    .addStatement("return this")
                    .build());

            type.addMethod(MethodSpec.methodBuilder("test")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ParameterizedTypeName.get(ClassName.get(Predicate.class), typeType),
                            "m")
                    .returns(boolean.class)
                    .addStatement("return m.test(this)")
                    .build());

            type.addMethod(MethodSpec.methodBuilder("applyFuture")
                    .addModifiers(Modifier.PUBLIC)
                    .addTypeVariable(TypeVariableName.get("R"))
                    .addParameter(ParameterizedTypeName.get(ClassName.get(Function.class)
                                    , typeType
                                    , ParameterizedTypeName.get(ClassName.get(Future.class), tr))
                            , "m")
                    .returns(ParameterizedTypeName.get(ClassName.get(Future.class), tr))
                    .addStatement("return $T.succeededFuture(this).flatMap(m)", Future.class)
                    .build());

            type.addMethod(MethodSpec.methodBuilder("acceptFuture")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ParameterizedTypeName.get(ClassName.get(Function.class)
                                    , typeType
                                    , ParameterizedTypeName.get(Future.class, Void.class))
                            , "m")
                    .returns(ParameterizedTypeName.get(ClassName.get(Future.class), typeType))
                    .addStatement("return $T.succeededFuture(this).flatMap(m).map(this)",
                            Future.class)
                    .build());

            type.addMethod(MethodSpec.methodBuilder("testFuture")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ParameterizedTypeName.get(ClassName.get(Function.class)
                                    , typeType
                                    , ParameterizedTypeName.get(Future.class, Boolean.class))
                            , "m")
                    .returns(ParameterizedTypeName.get(Future.class, Boolean.class))
                    .addStatement("return $T.succeededFuture(this).flatMap(m)", Future.class)
                    .build());
        }
        type.addMethod(MethodSpec.methodBuilder("from").addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(TypeName.get(proto.asType()), "t")
                .returns(typeType)
                .addStatement("return t == null ? null : t instanceof $T u ? u : new $T(t.asJson())", typeType, typeType)
                .build());
        type.addMethod(MethodSpec.methodBuilder("copy")
                .addModifiers(Modifier.PUBLIC)
                .returns(typeType)
                .addStatement("return new $T(toJson())", typeType)
                .build());
    }


}

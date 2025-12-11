package vat.codegen.utils;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.data.Numeric;
import org.jetbrains.annotations.Nullable;
import vat.api.Activities;
import vat.api.Data;
import vat.api.DomainError;
import vat.api.meta.*;
import vat.api.metadata.MetaData;
import vat.api.utils.ITimes;

import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.*;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

///
/// @author Zen.Liu
/// @since 2025-11-07


public interface MetaProc {
    static String identity(ExecutableElement el) {
        return ((TypeElement) el.getEnclosingElement()).getQualifiedName().toString() + "::" + el.getSimpleName();
    }

    static String identity(TypeElement el) {
        return el.getQualifiedName().toString();
    }

    record Describer(@Nullable AnnotatedValue value, @Nullable AnnotatedValue enhance) {
        public Describer(
                AnnotatedConstruct ele
        ) {
            this(
                    AnnotatedValue.of(ele, Describe.class).orElse(null),
                    AnnotatedValue.of(ele, Enhance.class).orElse(null)
            );
        }


        public <T extends MetaData> T fill(T v, boolean binary, CharSequence name, CharSequence identity) {
            return fill(v, binary, name, identity, null);
        }

        @SuppressWarnings("unchecked")
        public <T extends MetaData> T fill(T v, boolean binary, CharSequence name, CharSequence identity, @Nullable BiConsumer<Describer, T> post) {
            if (value == null) return (T) v
                    .name(name.toString())
                    .identity(identity.toString())
                    .description("");
            var o = (T) v
                    .name(value.getString("value").filter(Predicate.not(String::isBlank))
                            .map(I18N::get)
                            .orElseGet(name::toString))
                    .identity(identity.toString())
                    .description(value.getString("desc").filter(Predicate.not(String::isBlank))
                            .map(I18N::get)
                            .orElse(""));
            if (v instanceof MetaData.Properties p) {
                if (binary) p.binary(true);
            } else if (v instanceof MetaData.Entity ex) {
                var identityName = value.getString("identity").orElse("identity");
                var domain = value.getType("domain").orElse(null);
                if (domain != null) {
                    if (TypeName.get(domain).equals(ClassName.get(Activities.class))) {
                        ex.identify(identityName);
                    } else {
                        ex.identify(TypeName.get(domain).toString() + "::" + identityName);
                    }
                }
                if (binary) ex.binary(true);
            }
            if (post != null) post.accept(this, o);
            return o;
        }
    }

    default void fillDomain(Domain domain, TypeElement e) {
        assert domain.meta().name() == null;
        new Describer(e).fill(domain.meta(), false, e.getSimpleName().toString(), e.getQualifiedName().toString());
    }

    default void fill(Domain domain, MetaData.Config.Meta meta) {
        meta.name("config").identity(domain.meta().identity() + "::Config").description("");
    }

    default void addConfigEntry(Domain domain, ExecutableElement el, String pointer,
                               FuncField mapping, TypeMirror raw, Boolean once) {

        domain.meta().configDo(c -> (c instanceof MetaData.Config.Meta m?m:new MetaData.Config.Meta())
                .propertiesDo(l -> l.add(
                        new Describer(el).fill(new MetaData.ValueEntry.Meta(), false, i18n(el.getSimpleName()), identity(el))
                                .path(pointer)
                                .type(resolveMetaType(domain.ctx(), raw)))
                ));
    }

    default void addErrorEntry(Domain domain, ExecutableElement el, String pointer) {
        domain.meta().configDo(c -> (c instanceof MetaData.Config.Meta m?m:new MetaData.Config.Meta())
                .propertiesDo(l -> l.add(new Describer(el)
                .fill(new MetaData.ErrorEntry.Meta(), false,
                        i18n(el.getSimpleName()),
                        identity(el))
                .path(pointer))))
        ;
    }

    default void addPublish(Domain domain, ExecutableElement el, TypeMirror evt, String addr, boolean pointer) {
        domain.meta().publishDo(l -> l.add(new Describer(el)
                .fill(new MetaData.Publish.Meta(), false, i18n(el.getSimpleName()),
                        identity(el))
                .accept(e -> {
                    var dt = ((TypeElement) ((DeclaredType) evt).asElement());
                    (pointer ? e.configPath(addr) : e.address(addr))
                            .type(new MetaData.ReferenceType.Meta()
                                    .identity(
                                            dt.getQualifiedName().toString())
                                    .name(dt.getSimpleName().toString())
                            );
                })
        ))
        ;
    }

    default void addSubscribe( MetaData.Domain.Meta meta, ExecutableElement el, TypeMirror evt, String addr,
                              boolean pointer) {
        meta.subscribeDo(l -> l.add(new Describer(el)
                .fill(new MetaData.Subscribe.Meta(), false, i18n(el.getSimpleName()), identity(el))
                .accept(e -> {
                    var dt = ((TypeElement) ((DeclaredType) evt).asElement());
                    (pointer ? e.configPath(addr) : e.address(addr))
                            .type(new MetaData.ReferenceType.Meta()
                                    .identity(dt.getQualifiedName().toString())
                                    .name(dt.getSimpleName().toString())
                            );
                })
        ))
        ;
    }

    default void addUses(MetaData.Domain.Meta meta, ExecutableElement el, TypeMirror evt, String addr,
                         boolean pointer) {
        meta.usesDo(l -> l.add(new Describer(el)
                .fill(new MetaData.Uses.Meta(), false, i18n(el.getSimpleName()), identity(el))
                .accept(e -> {
                    var dt = ((TypeElement) ((DeclaredType) evt).asElement());
                    (pointer ? e.configPath(addr) : e.address(addr))
                            .type(new MetaData.ReferenceType.Meta()
                                    .identity(dt.getQualifiedName().toString())
                                    .name(dt.getSimpleName().toString())
                            );
                })
        ))
        ;
    }

    default void addAction(Domain domain, TypeElement where, ExecutableElement e, boolean retRequired,
                           TypeMirror rt) {
        domain.meta()
                .actionsDo(l -> l.add(new Describer(e)
                        .fill(new MetaData.Action.Meta(), false, i18n(e.getSimpleName()),
                                identity(where) + "::" + e.getSimpleName())
                        .input(e.getParameters().isEmpty() ? MetaData.VOID
                                : resolveMetaType( domain.ctx(), e.getParameters().getFirst().asType(),
                                AnnotatedValue.of(e.getParameters().getFirst(), Describe.class)
                                        .or(() ->AnnotatedValue.of(e.getParameters().getFirst().asType(), Describe.class))
                                        .orElse(null)))
                        .output(retRequired ? new MetaData.OptionalType(
                                resolveMetaType( domain.ctx(), rt)) : resolveMetaType(domain.ctx(), rt))
                ));
    }

    default void addEvent(Domain domain, TypeElement e, List<ResolvedField> fields) {
        domain.meta().eventsDo(l -> l.add(
                new Describer(e).fill(new MetaData.Event.Meta(), domain.ctx().isBinary(e), i18n(e.getSimpleName()), identity(e))
                        .kinds(kind(domain, e, fields))
                        .properties(fields.stream().map(x -> toProperty(domain, e, x)).toList())
        ));
    }

    default void addObject(Domain domain, TypeElement e, List<ResolvedField> fields) {
        domain.meta().dataDo(l -> l.add(
                new Describer(e).fill(new MetaData.Object.Meta(), domain.ctx().isBinary(e), i18n(e.getSimpleName()), identity(e))
                        .accept(d -> {
                            if (e.getKind() != ElementKind.INTERFACE) {
                                d.identity(e.getQualifiedName().toString());
                            }
                        })
                        .properties(fields.stream().map(x -> toProperty(domain, e, x)).toList())
        ));
    }

    default void addData(Domain domain, TypeElement e, List<? extends Element> fields) {
        domain.meta().dataDo(l -> l.add(
                new Describer(e).fill(new MetaData.Object.Meta(), domain.ctx().isBinary(e), i18n(e.getSimpleName()), identity(e))
                        .accept(d -> {
                            if (e.getKind() != ElementKind.INTERFACE) {
                                d.identity(e.getQualifiedName().toString());
                            }
                        })
                        .properties(fields.stream().map(x -> toProperty(domain, e, x))
                                .toList())
        ));
    }

    default MetaData.Property toProperty(Domain codec, TypeElement e, Element field) {
        return new Describer(field).fill(new MetaData.Property.Meta(), codec.ctx().isBinary(e), i18n(field.getSimpleName()),
                        identity(e) + "::" + field.getSimpleName())
                .accept(d -> {
                    var ret = field.asType();
                    d.optional(
                                    codec.ctx().sameType(ret, Optional.class) || codec.ctx().nullable(ret))
                            .product(resolveMetaType(codec.ctx(), ret))
                    ;
                    //TODO interceptor
                });
    }

    default void addRecord(Domain domain, TypeElement e, List<ResolvedField> fields) {
        domain.meta().recordsDo(l -> l.add(
                new Describer(e).fill(new MetaData.Record.Meta(), domain.ctx().isBinary(e), i18n(e.getSimpleName()), identity(e))
                        .columns(fields.stream().map(x -> toColumn(domain, e, x)).toList())
                        .table(AnnotatedValue.of(e, Table.class).flatMap(x -> x.getString("value"))
                                .orElseGet(
                                        () -> CaseConv.PASCAL_SNAKE.apply(e.getSimpleName().toString())))
        ));
    }

    default void addActor(Domain domain, TypeElement e, List<ResolvedField> fields) {
        domain.meta().actorsDo(l -> l.add(
                new Describer(e).fill(new MetaData.Actor.Meta(), domain.ctx().isBinary(e), i18n(e.getSimpleName()), identity(e))
                        .columns(fields.stream().map(x -> toColumn(domain, e, x)).toList())
                        .table(AnnotatedValue.of(e, Table.class).flatMap(x -> x.getString("value"))
                                .orElseGet(
                                        () -> CaseConv.PASCAL_SNAKE.apply(e.getSimpleName().toString())))
        ));
    }

    default void addAbility(Domain domain, TypeElement e, List<ResolvedField> fields) {
        domain.meta().abilitiesDo(l -> l.add(
                new Describer(e).fill(new MetaData.Ability.Meta(), domain.ctx().isBinary(e), i18n(e.getSimpleName()), identity(e))
                        .columns(fields.stream().map(x -> toColumn(domain, e, x)).toList())
                        .table(AnnotatedValue.of(e, Table.class).flatMap(x -> x.getString("value"))
                                .orElseGet(
                                        () -> CaseConv.PASCAL_SNAKE.apply(e.getSimpleName().toString())))

        ));
    }

    default List<MetaData.EventKind> kind(Domain codec, TypeElement e, List<ResolvedField> fields) {
        var ctx = codec.ctx();
        var kind = fields.stream().filter(x -> x.annotation(EventKind.class) || x.name().equals("kind"))
                .findFirst()
                .orElse(null);
        if (kind == null) throw new IllegalArgumentException("No event kind defined");
        var kindPrefix = "KIND_";
        var ord = new AtomicInteger(0);
        return kind.annotationValues(EventKind.class)
                .map(x -> kind.type()) //! annotated on method
                .map(t ->
                        t.getKind().equals(TypeKind.DECLARED)
                        && ctx.asElement(t) instanceof TypeElement type
                                ? type : null)//! type is declared
                .map(x ->
                        x.getKind().equals(ElementKind.ENUM)
                                ? x.getEnclosedElements().stream()
                                .filter(i -> i.getKind().equals(ElementKind.ENUM_CONSTANT))
                                .map(i -> (MetaData.EventKind) new Describer(i)
                                        .fill(new MetaData.EventKind.Meta(), codec.ctx().isBinary(e), i18n(i.getSimpleName()),
                                                x.getQualifiedName() + "#" + i.getSimpleName())
                                        .ordinal(ord.getAndIncrement())
                                        .text(i.getSimpleName().toString())
                                )
                                .toList()
                                : null)//! fetch ENUM values
                .filter(x -> !x.isEmpty())
                .orElseGet(() -> e.getEnclosedElements().stream()
                        .filter(x ->
                                x.getKind().equals(ElementKind.FIELD)
                                && x.getModifiers().contains(Modifier.PUBLIC)
                                && x.getModifiers().contains(Modifier.FINAL)
                                && x.getModifiers().contains(Modifier.STATIC)
                                && x.getSimpleName().toString()
                                        .startsWith(kindPrefix))
                        .map(i -> (MetaData.EventKind) new Describer(i)
                                .fill(new MetaData.EventKind.Meta(), codec.ctx().isBinary(e), i.getSimpleName(),
                                        e.getQualifiedName() + "#" + i.getSimpleName())
                                .ordinal(Long.parseLong(
                                        ((VariableElement) i).getConstantValue().toString()))
                                .text(i.getSimpleName().toString().substring(i.getSimpleName().toString()
                                                                                     .indexOf(
                                                                                             kindPrefix) + kindPrefix.length() + 1)))
                        .toList()
                );
    }

    default MetaData.Property toProperty(Domain domain, TypeElement e, ResolvedField field) {
        return new Describer(field.getter())
                .fill(new MetaData.Property.Meta(), domain.ctx().isBinary(e), i18n(field.method().getSimpleName()),
                        identity(e) + "#" + field.method().getSimpleName())
                .accept(d -> {
                    var ret = field.resolvedType(e);
                    d
                            .optional(domain.ctx().sameType(ret, Optional.class) || domain.ctx().nullable(ret))
                            .product(resolveMetaType(domain.ctx(), ret))
                    ;
                    interceptor(d, domain, e, field);
                });
    }

    default MetaData.Column toColumn(Domain domain, TypeElement e, ResolvedField field) {
        return new Describer(field.getter()).fill(new MetaData.Column.Meta(), domain.ctx().isBinary(e), field.method().getSimpleName(),
                        identity(e) + "#" + field.method().getSimpleName())
                .accept(d -> {
                    var ret = field.resolvedType(e);
                    var anno = AnnotatedValue.of(field.method(), Column.class);
                    d
                            .column(anno
                                    .flatMap(x -> x.getString("value"))
                                    .filter(VALID_NAME)
                                    .orElseGet(() -> CaseConv.CAMEL_SNAKE.apply(
                                            field.name())))

                            .size(anno
                                    .flatMap(x -> x.getInteger("size"))
                                    .orElse(null))
                            .max(anno
                                    .flatMap(x -> x.getInteger("max"))
                                    .orElse(null))
                            .min(anno
                                    .flatMap(x -> x.getInteger("min"))
                                    .orElse(null))
                            .precision(anno
                                    .flatMap(x -> x.getInteger("precision"))
                                    .orElse(null))
                            .scale(anno
                                    .flatMap(x -> x.getInteger("scale"))
                                    .orElse(null))
                            .enumName(anno
                                    .flatMap(x -> x.getBoolean("enumName"))
                                    .orElseGet(() -> field.annotation(EnumName.class)))
                            .optional(
                                    domain.ctx().sameType(ret, Optional.class) || domain.ctx()
                                            .nullable(
                                                    ret))
                            .interceptor(anno
                                    .flatMap(
                                            x -> x.getString("interceptField"))
                                    .filter(VALID_NAME)
                                    .map(x ->
                                            new MetaData.Functor.Meta()
                                                    .identity(anno.get()
                                                                      .getType(
                                                                              "interceptHolder")
                                                                      .map(TypeName::get)
                                                                      .orElseGet(
                                                                              () -> TypeName.get(
                                                                                      e.asType()))
                                                                      .toString() + "#" + x)
                                                    .construct(false)


                                    )
                                    .orElse(null)
                            )
                            .product(resolveMetaType(domain.ctx(), domain.ctx().orOptional(
                                    field.resolvedType(e))))
                    ;
                });
    }

    default void interceptor(MetaData.Property.Meta d, Domain codec, TypeElement e, ResolvedField f) {
        var validators = f.validators();
        var interceptors = f.interceptors();
        if (!validators.isEmpty()) d.validators(validators.stream().map(x -> functor(codec, e, f, x)).toList());
        if (!interceptors.isEmpty()) d.interceptors(interceptors.stream().map(x -> functor(codec, e, f, x)).toList());

    }

    default MetaData.Functor functor(Domain codec, TypeElement e, ResolvedField f, Validator x) {
        return new Describer(codec.ctx().find(x.type(), x.field()))
                .fill(new MetaData.Functor.Meta(), codec.ctx().isBinary(e),
                        e.getSimpleName() + "#" + f.name() + '%' + I18N.get("Validator")
                        , x.type().toString() + '#' + x.field())
                .construct(x.construct());
    }

    default MetaData.Functor functor(Domain codec, TypeElement e, ResolvedField f, Interceptor x) {
        return new Describer(codec.ctx().find(x.type(), x.field()))
                .fill(new MetaData.Functor.Meta(), codec.ctx().isBinary(e), e.getSimpleName() + "#" + f.name() + '%' + I18N.get("Validator")
                        , x.type().toString() + '#' + x.field())
                .construct(x.construct());
    }

    Map<MetaData.Type, MetaData.Type> i18n = new ConcurrentHashMap<>();

    static String i18n(CharSequence v) {
        return I18N.get(v.toString());
    }

    static MetaData.Type i18n(MetaData.NormalType v) {
        return i18n.computeIfAbsent(v, i -> new MetaData.NormalType(I18N.get(i.name()),
                I18N.get(i.identity()),
                I18N.get(i.description())));
    }

    static MetaData.Type i18n(MetaData.NumericType v) {
        return i18n.computeIfAbsent(v, i -> new MetaData.NumericType(((MetaData.NumericType) i).bits(),
                ((MetaData.NumericType) i).floatingPoint(),
                I18N.get(i.name()),
                I18N.get(i.identity()),
                I18N.get(i.description())));
    }

    Map<TypeMirror, MetaData.Type> resolved = new ConcurrentHashMap<>();

    static MetaData.Type resolveMetaType(Context ctx, TypeMirror type) {
        return resolveMetaType(ctx, type, null);
    }

   private static MetaData.Type annotation(MetaData.Type x, AnnotatedValue describer) {
        if (describer == null) return x;
        if (x instanceof MetaData.ReferenceType md) {
            var name = describer.getString("value")
                    .filter(Predicate.not(String::isBlank))
                    .or(() -> describer.getString("desc"))
                    .orElse("");
            if (!name.isEmpty()) return (MetaData.Type) md.description(name);
        }
        return x;
    }

    static MetaData.Type resolveMetaType(Context ctx, TypeMirror type, @Nullable AnnotatedValue describer) {
        if (resolved.containsKey(type)) {
            return annotation(resolved.get(type), describer);
        }
        var x = switch (type.getKind()) {
            case BOOLEAN -> i18n(MetaData.BOOLEAN);
            case BYTE -> i18n(MetaData.BYTE);
            case SHORT -> i18n(MetaData.SHORT);
            case INT -> i18n(MetaData.INT);
            case LONG -> i18n(MetaData.LONG);
            case CHAR -> i18n(MetaData.CHAR);
            case FLOAT -> i18n(MetaData.FLOAT);
            case DOUBLE -> i18n(MetaData.DOUBLE);
            case VOID, NULL -> i18n(MetaData.VOID);
            case ARRAY -> {
                var at = (ArrayType) type;
                yield switch (at.getKind()) {
                    case BYTE -> i18n(MetaData.BINARY);
                    case BOOLEAN,
                         SHORT,
                         INT,
                         LONG,
                         CHAR,
                         FLOAT,
                         DOUBLE,
                         ARRAY,
                         DECLARED ->
                            new Describer(at).fill(new MetaData.ArrayType.Meta(), ctx.isBinary(type), "array", "")
                                    .element(resolveMetaType(ctx, at.getComponentType(), null));
                    default -> throw new IllegalStateException("invalid meta data array type: " + type);
                };
            }
            case DECLARED -> {
                var dt = (DeclaredType) type;
                {
                    var o = ctx.orOptional(dt);
                    if (o != null) dt = (DeclaredType) o;
                }
                var opt = ctx.nullable(dt);
                var unbox = ctx.unbox(dt);
                if (unbox.isPresent()) {
                    yield switch (unbox.get().getKind()) {
                        case BOOLEAN ->
                                opt ? new MetaData.OptionalType(i18n(MetaData.BOOLEAN)) : i18n(MetaData.BOOLEAN);
                        case BYTE -> opt ? new MetaData.OptionalType(i18n(MetaData.BYTE)) : i18n(MetaData.BYTE);
                        case SHORT -> opt ? new MetaData.OptionalType(i18n(MetaData.SHORT)) : i18n(MetaData.SHORT);
                        case INT -> opt ? new MetaData.OptionalType(i18n(MetaData.INT)) : i18n(MetaData.INT);
                        case LONG -> opt ? new MetaData.OptionalType(i18n(MetaData.LONG)) : i18n(MetaData.LONG);
                        case CHAR -> opt ? new MetaData.OptionalType(i18n(MetaData.CHAR)) : i18n(MetaData.CHAR);
                        case FLOAT -> opt ? new MetaData.OptionalType(i18n(MetaData.FLOAT)) : i18n(MetaData.FLOAT);
                        case DOUBLE -> opt ? new MetaData.OptionalType(i18n(MetaData.DOUBLE)) : i18n(MetaData.DOUBLE);
                        case VOID, NULL -> opt ? new MetaData.OptionalType(i18n(MetaData.VOID)) : i18n(MetaData.VOID);
                        default -> throw new IllegalStateException("invalid meta data boxed type: " + type);
                    };
                }
                if (ctx.sameType(dt, Void.class)) yield i18n(MetaData.VOID);

                if (ctx.sameType(dt, ITimes.IDatetime.class))
                    yield opt ? new MetaData.OptionalType(i18n(MetaData.LONG_DATETIME)) : i18n(MetaData.LONG_DATETIME);
                if (ctx.sameType(dt, ITimes.IDate.class))
                    yield opt ? new MetaData.OptionalType(i18n(MetaData.INTEGER_DATE)) : i18n(MetaData.INTEGER_DATE);
                if (ctx.sameType(dt, ITimes.ITime.class))
                    yield opt ? new MetaData.OptionalType(i18n(MetaData.INTEGER_TIME)) : i18n(MetaData.INTEGER_TIME);

                if (ctx.sameType(dt, JsonObject.class))
                    yield opt ? new MetaData.OptionalType(i18n(MetaData.JSON_OBJECT)) : i18n(MetaData.JSON_OBJECT);
                if (ctx.sameType(dt, JsonArray.class))
                    yield opt ? new MetaData.OptionalType(i18n(MetaData.JSON_ARRAY)) : i18n(MetaData.JSON_ARRAY);
                if (ctx.sameType(dt, Period.class))
                    yield opt ? new MetaData.OptionalType(i18n(MetaData.PERIOD)) : i18n(MetaData.PERIOD);
                if (ctx.sameType(dt, Duration.class))
                    yield opt ? new MetaData.OptionalType(i18n(MetaData.DURATION)) : i18n(MetaData.DURATION);
                if (ctx.sameType(dt, Instant.class))
                    yield opt ? new MetaData.OptionalType(i18n(MetaData.INSTANT)) : i18n(MetaData.INSTANT);
                if (ctx.sameType(dt, BigDecimal.class))
                    yield opt ? new MetaData.OptionalType(i18n(MetaData.DECIMAL)) : i18n(MetaData.DECIMAL);
                if (ctx.sameType(dt, OffsetDateTime.class))
                    yield opt ? new MetaData.OptionalType(i18n(MetaData.DATETIME_TZ)) : i18n(MetaData.DATETIME_TZ);
                if (ctx.sameType(dt, OffsetTime.class))
                    yield opt ? new MetaData.OptionalType(i18n(MetaData.TIME_TZ)) : i18n(MetaData.TIME_TZ);
                if (ctx.sameType(dt, LocalTime.class))
                    yield opt ? new MetaData.OptionalType(i18n(MetaData.TIME)) : i18n(MetaData.TIME);
                if (ctx.sameType(dt, LocalDate.class))
                    yield opt ? new MetaData.OptionalType(i18n(MetaData.DATE)) : i18n(MetaData.DATE);
                if (ctx.sameType(dt, LocalDateTime.class))
                    yield opt ? new MetaData.OptionalType(i18n(MetaData.DATETIME)) : i18n(MetaData.DATETIME);
                if (ctx.sameType(dt, String.class))
                    yield opt ? new MetaData.OptionalType(i18n(MetaData.STRING)) : i18n(MetaData.STRING);
                if (ctx.sameType(dt, Buffer.class))
                    yield opt ? new MetaData.OptionalType(i18n(MetaData.BUFFER)) : i18n(MetaData.BUFFER);
                if (ctx.sameType(dt, Numeric.class))
                    yield opt ? new MetaData.OptionalType(i18n(MetaData.NUMERIC)) : i18n(MetaData.NUMERIC);
                if (ctx.sameType(dt, UUID.class))
                    yield opt ? new MetaData.OptionalType(i18n(MetaData.UUID)) : i18n(MetaData.UUID);
                if (ctx.rawSameType(dt, Class.class)) {
                    var te = (TypeElement) dt.asElement();
                    var t = new MetaData.ReferenceType.Meta()
                            .name(I18N.get(te.getSimpleName().toString()))
                            .identity(te.getQualifiedName().toString());
                    yield opt ? new MetaData.OptionalType(t) : t;
                }
                if (ctx.rawAssignableTo(dt, Enum.class)) {
                    var te = ((TypeElement) dt.asElement());
                    var t = new MetaData.EnumerationType.Meta()
                            .name(I18N.get(te.getSimpleName().toString()))
                            .identity(te.getQualifiedName().toString())
                            .candidates(toEnumEntry(ctx, te));
                    yield opt ? new MetaData.OptionalType(t) : t;
                }
                if (ctx.rawAssignableTo(dt, Data.class)) {
                    var te = ((TypeElement) dt.asElement());
                    var t = new MetaData.ReferenceType.Meta()
                            .name(I18N.get(te.getSimpleName().toString()))
                            .identity(te.getQualifiedName().toString());
                    yield opt ? new MetaData.OptionalType(t) : t;
                }
                if (ctx.rawAssignableTo(dt, DomainError.class)) yield i18n(MetaData.ERROR);
                if (ctx.rawAssignableTo(dt, Collection.class)) {
                    var in = resolveMetaType(ctx, dt.getTypeArguments().getFirst(), null);
                    var t = (ctx.rawAssignableTo(dt, Set.class)) ?
                            new Describer(dt).fill(new MetaData.ListType.Meta(), false, dt.asElement().getSimpleName(), "")
                                    .unique(true).element(in)
                            : new Describer(dt).fill(new MetaData.ListType.Meta(), false, dt.asElement().getSimpleName(), "")
                            .unique(false).element(in);
                    yield opt ? new MetaData.OptionalType(t) : t;
                }
                if (ctx.rawAssignableTo(dt, Map.class)) {
                    var keyType = dt.getTypeArguments().getFirst();
                    if (ctx.sameType(keyType, String.class)) yield MetaData.JSON_OBJECT;
                    var key = resolveMetaType(ctx, keyType, null);
                    var value = resolveMetaType(ctx, dt.getTypeArguments().getLast(), null);
                    var t = new Describer(dt).fill(new MetaData.ProjectionType.Meta(), false, dt.asElement().getSimpleName(),
                            "").key(key).value(value);
                    yield opt ? new MetaData.OptionalType(t) : t;
                }

                throw new IllegalStateException("invalid declared data type: " + type);
            }
            default -> throw new IllegalStateException("invalid meta data type: " + type);
        };
        resolved.put(type, x);
        if (describer != null) {
            return annotation(x, describer);
        }
        return x;
    }

    static List<MetaData.EnumerationEntry> toEnumEntry(Context ctx, TypeElement e) {
        var ord = new AtomicInteger();
        return e.getEnclosedElements().
                stream()
                .filter(x -> x.getKind() == ElementKind.ENUM_CONSTANT)
                .map(x -> (MetaData.EnumerationEntry) new Describer(x)
                        .fill(new MetaData.EnumerationEntry.Meta(), ctx.isBinary(e), x.getSimpleName(),
                                identity(e) + '#' + x.getSimpleName())
                        .ordinal(ord.getAndIncrement())
                        .text(x.getSimpleName().toString())
                )
                .toList();
    }

    Predicate<String> VALID_NAME = Predicate.not(String::isBlank);
}

package vat.api.metadata;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vat.api.Data;
import vat.api.implement.Codec.CombineProperty;
import vat.api.implement.Codec.DataCodec;
import vat.api.implement.Codec.DataProperty;
import vat.api.implement.CommonCodec;
import vat.api.meta.Computed;
import vat.api.meta.Enhance;
import vat.api.meta.Nullable;
import vat.api.trait.Applicative;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.StreamSupport;

import static vat.api.implement.Codec.data;
import static vat.api.implement.Codec.list;


///
/// @author Zen.Liu
/// @since 2025-11-05


public interface MetaData {
    Logger log = LoggerFactory.getLogger(MetaData.class);

    String identity();

    String name();

    String description();

    MetaData identity(String v);

    MetaData name(String v);

    MetaData description(String v);


    interface Type extends Data, MetaData {

    }


    record NumericType(int bits, boolean floatingPoint, String name, String identity,
                       String description) implements Type {
        public NumericType(JsonObject j) {
            this(j.getInteger("bits")
                    , j.getBoolean("floatingPoint", false)
                    , j.getString("name")
                    , j.getString("identity")
                    , j.getString("description"));
        }

        @Override
        public JsonObject asJson() {
            return JsonObject.of(
                    "name", name,
                    "identity", identity,
                    "description", description,
                    "bits", bits,
                    "floatingPoint", floatingPoint
            );
        }

        @Override
        public MetaData identity(String v) {
            throw new UnsupportedOperationException("not supported");
        }

        @Override
        public MetaData name(String v) {
            throw new UnsupportedOperationException("not supported");
        }

        @Override
        public MetaData description(String v) {
            throw new UnsupportedOperationException("not supported");
        }
    }

    NumericType BYTE = new NumericType(8, false, "byte", "byte", "");
    NumericType SHORT = new NumericType(16, false, "short", "short", "");
    NumericType INT = new NumericType(32, false, "int", "int", "");
    NumericType CHAR = new NumericType(32, false, "char", "char", "");
    NumericType LONG = new NumericType(64, false, "long", "long", "");
    NumericType FLOAT = new NumericType(32, true, "float", "float", "");
    NumericType DOUBLE = new NumericType(64, true, "double", "double", "");

    record NormalType(String name, String identity, String description) implements Type {
        public NormalType(JsonObject j) {
            this(
                    j.getString("name")
                    , j.getString("identity")
                    , j.getString("description"));
        }

        @Override
        public JsonObject asJson() {
            return JsonObject.of(
                    "name", name,
                    "identity", identity,
                    "description", description
            );
        }

        @Override
        public MetaData identity(String v) {
            throw new UnsupportedOperationException("not supported");
        }

        @Override
        public MetaData name(String v) {
            throw new UnsupportedOperationException("not supported");
        }

        @Override
        public MetaData description(String v) {
            throw new UnsupportedOperationException("not supported");
        }
    }

    NormalType VOID = new NormalType("Void", "Void", "nothing");
    NormalType ERROR = new NormalType("Error", "Error", "domain error");

    NormalType BOOLEAN = new NormalType("boolean", "boolean", "");
    NormalType STRING = new NormalType("String", "String", "");
    NormalType BINARY = new NormalType("Binary", "Binary", "small binary data");
    NormalType BUFFER = new NormalType("Buffer", "Buffer", "big binary data");

    NormalType JSON_OBJECT = new NormalType("JsonObject", "JsonObject", "json style object");
    NormalType JSON_ARRAY = new NormalType("JsonArray", "JsonArray", "json style array");

    NormalType UUID = new NormalType("UUID", "UUID", "Universally unique identifier");
    NormalType TIME = new NormalType("Time", "Time", "time");
    NormalType DATE = new NormalType("Date", "Date", "date");
    NormalType DATETIME = new NormalType("DateTime", "DateTime", "date and time");
    NormalType TIME_TZ = new NormalType("TimeTZ", "TimeTZ", "time with time-zone");
    NormalType DATETIME_TZ = new NormalType("DateTimeTZ", "DateTimeTZ", "datetime with time-zone");
    NormalType INSTANT = new NormalType("Instant", "Instant", "instant");

    NormalType DECIMAL = new NormalType("Decimal", "Decimal", "big decimal value");
    NormalType NUMERIC = new NormalType("Numeric", "Numeric", "numeric value almost same as big decimal");

    NormalType DURATION = new NormalType("Duration", "Duration", "time interval of maximum of days");
    NormalType PERIOD = new NormalType("Period", "Period", "time interval of minimal of days");

    NormalType INTEGER_DATE = new NormalType("IDate", "IDate", "Date present as literal date integer.Eg: 20250101");
    NormalType INTEGER_TIME = new NormalType("ITime", "ITime", "Time present as literal time integer.Eg: 121201");
    NormalType LONG_DATETIME = new NormalType("IDateTime", "IDateTime", "datetime present as literal datetime integer.Eg: 20240101120101");

    record OptionalType(Type type) implements Type {
        public OptionalType(JsonObject j) {
            this((Type) vat.api.implement.Codec.data(j, null));
        }

        @Override
        public JsonObject asJson() {
            return JsonObject.of(
                    "optional", true,
                    "type", type.asJson().put("$type", type.getClass().getName())
            );
        }

        @Override
        public String identity() {
            return type.identity();
        }

        @Override
        public String name() {
            return type.name();
        }

        @Override
        public String description() {
            return type.description();
        }

        @Override
        public OptionalType identity(String v) {
            type.identity(v);
            return this;
        }

        @Override
        public OptionalType name(String v) {
            type.name(v);
            return this;
        }

        @Override
        public OptionalType description(String v) {
            type.description(v);
            return this;
        }
    }

    interface GenericType extends Type {

    }


    interface RepeatType extends GenericType {
        Type element();

        boolean nullable();
    }

    @Enhance
    interface ListType extends RepeatType {
        boolean unique();

        record Meta(JsonObject asJson) implements MetaData.ListType, Applicative<Meta> {
            @Override
            public Meta _this() {
                return this;
            }

            @Override
            public Future<Meta> acceptFuture(Function<Meta, Future<Void>> m) {
                return Future.succeededFuture(this).flatMap(m).map(this);
            }

            @Override
            public <R> Future<R> applyFuture(Function<Meta, Future<R>> m) {
                return Future.succeededFuture(this).flatMap(m);
            }

            @Override
            public Future<Boolean> testFuture(Function<Meta, Future<Boolean>> m) {
                return Future.succeededFuture(this).flatMap(m);
            }

            public Meta() {
                this(new JsonObject());
            }

            @Override
            public Class<MetaData.ListType> domainIdentity() {
                return MetaData.ListType.class;
            }

            @Override
            public MetaData.Type element() {
                return Codec.ACTIVATE_API_METADATA_METADATA__TYPE.get(this.asJson, "element");
            }

            public Meta element(MetaData.Type v) {
                Codec.ACTIVATE_API_METADATA_METADATA__TYPE.set(this.asJson, "element", v);
                return this;
            }

            public Meta elementDo(UnaryOperator<MetaData.@Nullable Type> act) {
                var x = act.apply(element());
                return element(x);
            }

            @Override
            public boolean nullable() {
                return vat.api.implement.Codec.BOOLEAN.get(this.asJson, "nullable");
            }

            public Meta nullable(boolean v) {
                vat.api.implement.Codec.BOOLEAN.set(this.asJson, "nullable", v);
                return this;
            }

            @Override
            public String identity() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "identity");
            }

            public Meta identity(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "identity", v);
                return this;
            }

            @Override
            public String name() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "name");
            }

            public Meta name(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "name", v);
                return this;
            }

            @Override
            public String description() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "description");
            }

            public Meta description(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "description", v);
                return this;
            }

            @Override
            public boolean unique() {
                return vat.api.implement.Codec.BOOLEAN.get(this.asJson, "unique");
            }

            public Meta unique(boolean v) {
                vat.api.implement.Codec.BOOLEAN.set(this.asJson, "unique", v);
                return this;
            }

            public Meta copy() {
                return new Meta(asJson());
            }


        }


    }

    @Enhance
    interface ArrayType extends RepeatType {
        record Meta(JsonObject asJson) implements MetaData.ArrayType, Applicative<Meta> {
            @Override
            public Meta _this() {
                return this;
            }


            public Meta() {
                this(new JsonObject());
            }

            @Override
            public Class<MetaData.ArrayType> domainIdentity() {
                return MetaData.ArrayType.class;
            }

            @Override
            public MetaData.Type element() {
                return Codec.ACTIVATE_API_METADATA_METADATA__TYPE.get(this.asJson, "element");
            }

            public Meta element(MetaData.Type v) {
                Codec.ACTIVATE_API_METADATA_METADATA__TYPE.set(this.asJson, "element", v);
                return this;
            }

            public Meta elementDo(UnaryOperator<MetaData.@Nullable Type> act) {
                var x = act.apply(element());
                return element(x);
            }

            @Override
            public boolean nullable() {
                return vat.api.implement.Codec.BOOLEAN.get(this.asJson, "nullable");
            }

            public Meta nullable(boolean v) {
                vat.api.implement.Codec.BOOLEAN.set(this.asJson, "nullable", v);
                return this;
            }

            @Override
            public String identity() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "identity");
            }

            public Meta identity(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "identity", v);
                return this;
            }

            @Override
            public String name() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "name");
            }

            public Meta name(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "name", v);
                return this;
            }

            @Override
            public String description() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "description");
            }

            public Meta description(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "description", v);
                return this;
            }

            public Meta copy() {
                return new Meta(asJson());
            }


        }


    }

    @Enhance
    interface ProjectionType extends GenericType {
        Type key();

        Type value();

        record Meta(JsonObject asJson) implements MetaData.ProjectionType, Applicative<Meta> {
            @Override
            public Meta _this() {
                return this;
            }


            public Meta() {
                this(new JsonObject());
            }

            @Override
            public Class<MetaData.ProjectionType> domainIdentity() {
                return MetaData.ProjectionType.class;
            }

            @Override
            public String identity() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "identity");
            }

            public Meta identity(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "identity", v);
                return this;
            }

            @Override
            public String name() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "name");
            }

            public Meta name(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "name", v);
                return this;
            }

            @Override
            public String description() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "description");
            }

            public Meta description(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "description", v);
                return this;
            }

            @Override
            public MetaData.Type key() {
                return Codec.ACTIVATE_API_METADATA_METADATA__TYPE.get(this.asJson, "key");
            }

            public Meta key(MetaData.Type v) {
                Codec.ACTIVATE_API_METADATA_METADATA__TYPE.set(this.asJson, "key", v);
                return this;
            }

            public Meta keyDo(UnaryOperator<MetaData.@Nullable Type> act) {
                var x = act.apply(key());
                return key(x);
            }

            @Override
            public MetaData.Type value() {
                return Codec.ACTIVATE_API_METADATA_METADATA__TYPE.get(this.asJson, "value");
            }

            public Meta value(MetaData.Type v) {
                Codec.ACTIVATE_API_METADATA_METADATA__TYPE.set(this.asJson, "value", v);
                return this;
            }

            public Meta valueDo(UnaryOperator<MetaData.@Nullable Type> act) {
                var x = act.apply(value());
                return value(x);
            }

            public Meta copy() {
                return new Meta(asJson());
            }

        }

    }

    @Enhance
    interface ReferenceType extends Data,MetaData, Type {
        String provider();

        record Meta(JsonObject asJson) implements MetaData.ReferenceType, Applicative<Meta> {
            @Override
            public Meta _this() {
                return this;
            }


            public Meta() {
                this(new JsonObject());
            }

            @Override
            public Class<MetaData.ReferenceType> domainIdentity() {
                return MetaData.ReferenceType.class;
            }

            @Override
            public String identity() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "identity");
            }

            public Meta identity(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "identity", v);
                return this;
            }

            @Override
            public String name() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "name");
            }

            public Meta name(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "name", v);
                return this;
            }

            @Override
            public String description() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "description");
            }

            public Meta description(String v) {
                var copy=asJson.copy();
                vat.api.implement.Codec.STRING.set(copy, "description", v);
                return new Meta(copy);
            }

            @Override
            public String provider() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "provider");
            }

            public Meta provider(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "provider", v);
                return this;
            }

            public Meta copy() {
                return new Meta(asJson());
            }


        }


    }

    @Enhance
    interface EnumerationEntry extends Data, MetaData {
        long ordinal();

        String text();

        record Meta(JsonObject asJson) implements MetaData.EnumerationEntry, Applicative<Meta> {
            @Override
            public Meta _this() {
                return this;
            }


            public Meta() {
                this(new JsonObject());
            }

            @Override
            public String text() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "text");
            }

            public Meta text(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "text", v);
                return this;
            }

            @Override
            public long ordinal() {
                return vat.api.implement.Codec.LONG.get(this.asJson, "ordinal");
            }

            public Meta ordinal(long v) {
                vat.api.implement.Codec.LONG.set(this.asJson, "ordinal", v);
                return this;
            }

            @Override
            public Class<MetaData.EnumerationEntry> domainIdentity() {
                return MetaData.EnumerationEntry.class;
            }

            @Override
            public String identity() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "identity");
            }

            public Meta identity(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "identity", v);
                return this;
            }

            @Override
            public String name() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "name");
            }

            public Meta name(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "name", v);
                return this;
            }

            @Override
            public String description() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "description");
            }

            public Meta description(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "description", v);
                return this;
            }

            public Meta copy() {
                return new Meta(asJson());
            }


        }


    }

    @Enhance
    interface EnumerationType extends Data,Type  {
        List<EnumerationEntry> candidates();

        record Meta(JsonObject asJson) implements MetaData.EnumerationType, Applicative<Meta> {
            @Override
            public Meta _this() {
                return this;
            }


            public Meta() {
                this(new JsonObject());
            }

            @Override
            public Class<MetaData.EnumerationType> domainIdentity() {
                return MetaData.EnumerationType.class;
            }

            @Override
            public String identity() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "identity");
            }

            public Meta identity(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "identity", v);
                return this;
            }

            @Override
            public String name() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "name");
            }

            public Meta name(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "name", v);
                return this;
            }

            @Override
            public String description() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "description");
            }

            public Meta description(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "description", v);
                return this;
            }

            @Override
            public List<MetaData.EnumerationEntry> candidates() {
                return Codec.LIST_$$ACTIVATE_API_METADATA_METADATA__ENUMERATION_ENTRY.get(this.asJson, "candidates");
            }

            public Meta candidates(List<MetaData.EnumerationEntry> v) {
                Codec.LIST_$$ACTIVATE_API_METADATA_METADATA__ENUMERATION_ENTRY.set(this.asJson, "candidates", v);
                return this;
            }

            public Meta candidatesDo(Consumer<List<MetaData.EnumerationEntry>> act) {
                var x = Optional.ofNullable(candidates()).orElseGet(ArrayList::new);
                act.accept(x);
                candidates(x);
                return this;
            }

            public Meta copy() {
                return new Meta(asJson());
            }

        }


    }

    @Enhance
    interface Functor extends Data, MetaData {
        boolean construct();

        record Meta(JsonObject asJson) implements MetaData.Functor, Applicative<Meta> {
            @Override
            public Meta _this() {
                return this;
            }


            public Meta() {
                this(new JsonObject());
            }

            @Override
            public Class<MetaData.Functor> domainIdentity() {
                return MetaData.Functor.class;
            }

            @Override
            public String identity() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "identity");
            }

            public Meta identity(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "identity", v);
                return this;
            }

            @Override
            public String name() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "name");
            }

            public Meta name(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "name", v);
                return this;
            }

            @Override
            public String description() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "description");
            }

            public Meta description(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "description", v);
                return this;
            }

            @Override
            public boolean construct() {
                return vat.api.implement.Codec.BOOLEAN.get(this.asJson, "construct");
            }

            public Meta construct(boolean v) {
                vat.api.implement.Codec.BOOLEAN.set(this.asJson, "construct", v);
                return this;
            }

            public Meta copy() {
                return new Meta(asJson());
            }


        }


    }

    @Enhance
    interface Property extends Data, MetaData {
        List<Functor> interceptors();

        List<Functor> validators();

        String mappings();

        Type product();

        boolean optional();

        record Meta(JsonObject asJson) implements MetaData.Property, Applicative<Meta> {
            @Override
            public Meta _this() {
                return this;
            }


            public Meta() {
                this(new JsonObject());
            }

            @Override
            public Class<MetaData.Property> domainIdentity() {
                return MetaData.Property.class;
            }

            @Override
            public String identity() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "identity");
            }

            public Meta identity(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "identity", v);
                return this;
            }

            @Override
            public String name() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "name");
            }

            public Meta name(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "name", v);
                return this;
            }

            @Override
            public String description() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "description");
            }

            public Meta description(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "description", v);
                return this;
            }

            @Override
            public List<MetaData.Functor> interceptors() {
                return Codec.LIST_$$ACTIVATE_API_METADATA_METADATA__FUNCTOR.get(this.asJson, "interceptors");
            }

            public Meta interceptors(List<MetaData.Functor> v) {
                Codec.LIST_$$ACTIVATE_API_METADATA_METADATA__FUNCTOR.set(this.asJson, "interceptors", v);
                return this;
            }

            public Meta interceptorsDo(Consumer<List<MetaData.Functor>> act) {
                var x = Optional.ofNullable(interceptors()).orElseGet(ArrayList::new);
                act.accept(x);
                interceptors(x);
                return this;
            }

            @Override
            public List<MetaData.Functor> validators() {
                return Codec.LIST_$$ACTIVATE_API_METADATA_METADATA__FUNCTOR.get(this.asJson, "validators");
            }

            public Meta validators(List<MetaData.Functor> v) {
                Codec.LIST_$$ACTIVATE_API_METADATA_METADATA__FUNCTOR.set(this.asJson, "validators", v);
                return this;
            }

            public Meta validatorsDo(Consumer<List<MetaData.Functor>> act) {
                var x = Optional.ofNullable(validators()).orElseGet(ArrayList::new);
                act.accept(x);
                validators(x);
                return this;
            }

            @Override
            public String mappings() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "mappings");
            }

            public Meta mappings(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "mappings", v);
                return this;
            }

            @Override
            public MetaData.Type product() {
                return Codec.ACTIVATE_API_METADATA_METADATA__TYPE.get(this.asJson, "product");
            }

            public Meta product(MetaData.Type v) {
                Codec.ACTIVATE_API_METADATA_METADATA__TYPE.set(this.asJson, "product", v);
                return this;
            }

            public Meta productDo(UnaryOperator<MetaData.@Nullable Type> act) {
                var x = act.apply(product());
                return product(x);
            }

            public boolean optional() {
                return vat.api.implement.Codec.BOOLEAN.get(asJson, "optional");
            }

            public Meta optional(boolean v) {
                vat.api.implement.Codec.BOOLEAN.set(asJson, "optional", v);
                return this;
            }

            public Meta copy() {
                return new Meta(asJson());
            }


        }


    }

    @Enhance
    interface Column extends Data, MetaData {
        String column();

        boolean optional();

        List<String> unique();

        List<String> index();

        Integer size();

        Integer max();

        Integer min();

        Integer precision();

        Integer scale();

        Boolean enumName();

        @Nullable
        Functor interceptor();

        Type product();

        record Meta(JsonObject asJson) implements MetaData.Column, Applicative<Meta> {
            @Override
            public Meta _this() {
                return this;
            }


            public Meta() {
                this(new JsonObject());
            }

            @Override
            public Class<MetaData.Column> domainIdentity() {
                return MetaData.Column.class;
            }

            @Override
            public String identity() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "identity");
            }

            public Meta identity(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "identity", v);
                return this;
            }

            @Override
            public String name() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "name");
            }

            public Meta name(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "name", v);
                return this;
            }

            @Override
            public String description() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "description");
            }

            public Meta description(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "description", v);
                return this;
            }

            @Override
            public String column() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "column");
            }

            public Meta column(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "column", v);
                return this;
            }

            @Override
            public List<String> unique() {
                return CommonCodec.LIST_$$STRING.get(this.asJson, "unique");
            }

            public Meta unique(List<String> v) {
                CommonCodec.LIST_$$STRING.set(this.asJson, "unique", v);
                return this;
            }

            public Meta uniqueDo(Consumer<List<String>> act) {
                var x = Optional.ofNullable(unique()).orElseGet(ArrayList::new);
                act.accept(x);
                unique(x);
                return this;
            }

            @Override
            public List<String> index() {
                return CommonCodec.LIST_$$STRING.get(this.asJson, "index");
            }

            public Meta index(List<String> v) {
                CommonCodec.LIST_$$STRING.set(this.asJson, "index", v);
                return this;
            }

            public Meta indexDo(Consumer<List<String>> act) {
                var x = Optional.ofNullable(index()).orElseGet(ArrayList::new);
                act.accept(x);
                index(x);
                return this;
            }

            @Override
            public Integer size() {
                return vat.api.implement.Codec.INTEGER.get(this.asJson, "size");
            }

            public Meta size(Integer v) {
                vat.api.implement.Codec.INTEGER.set(this.asJson, "size", v);
                return this;
            }

            @Override
            public Integer precision() {
                return vat.api.implement.Codec.INTEGER.get(this.asJson, "precision");
            }

            public Meta precision(Integer v) {
                vat.api.implement.Codec.INTEGER.set(this.asJson, "precision", v);
                return this;
            }

            @Override
            public Integer scale() {
                return vat.api.implement.Codec.INTEGER.get(this.asJson, "scale");
            }

            public Meta scale(Integer v) {
                vat.api.implement.Codec.INTEGER.set(this.asJson, "scale", v);
                return this;
            }

            @Override
            public Integer max() {
                return vat.api.implement.Codec.INTEGER.get(this.asJson, "max");
            }

            public Meta max(Integer v) {
                vat.api.implement.Codec.INTEGER.set(this.asJson, "max", v);
                return this;
            }

            @Override
            public Integer min() {
                return vat.api.implement.Codec.INTEGER.get(this.asJson, "min");
            }

            public Meta min(Integer v) {
                vat.api.implement.Codec.INTEGER.set(this.asJson, "min", v);
                return this;
            }

            public boolean optional() {
                return vat.api.implement.Codec.BOOLEAN.get(asJson, "optional");
            }

            public Meta optional(boolean v) {
                vat.api.implement.Codec.BOOLEAN.set(asJson, "optional", v);
                return this;
            }

            @Override
            public Functor interceptor() {
                return Codec.FUNCTOR_DATA.get(this.asJson.getJsonObject("interceptor"));
            }

            public Meta interceptor(Functor v) {
                asJson.put("interceptor", Codec.FUNCTOR_DATA.set(v));
                return this;
            }

            @Override
            public Boolean enumName() {
                return vat.api.implement.Codec.BOOLEAN.get(this.asJson, "enumName");
            }

            public Meta enumName(Boolean v) {
                vat.api.implement.Codec.BOOLEAN.set(this.asJson, "enumName", v);
                return this;
            }

            @Override
            public MetaData.Type product() {
                return Codec.ACTIVATE_API_METADATA_METADATA__TYPE.get(this.asJson, "product");
            }

            public Meta product(MetaData.Type v) {
                Codec.ACTIVATE_API_METADATA_METADATA__TYPE.set(this.asJson, "product", v);
                return this;
            }

            public Meta productDo(UnaryOperator<MetaData.@Nullable Type> act) {
                var x = act.apply(product());
                return product(x);
            }

            public Meta copy() {
                return new Meta(asJson());
            }


        }


    }

    interface Properties {

        boolean binary();
        Properties binary( boolean v);

        List<Property> properties();
    }

    interface Entity extends MetaData {
        List<Column> columns();

        String table();

        String identify();

        Entity identify(String id);

        boolean binary();
        Entity binary( boolean v);

    }

    @Enhance
    interface Actor extends Data, Entity {
        @Computed
        default String role() {
            return "actor";
        }

        record Meta(JsonObject asJson) implements MetaData.Actor, Applicative<Meta> {
            @Override
            public Meta _this() {
                return this;
            }


            @Override
            public boolean binary() {
                return Optional.ofNullable(vat.api.implement.Codec.BOOLEAN.get(asJson, "binary")).orElse(false);
            }

            public Meta binary(boolean b) {
                vat.api.implement.Codec.BOOLEAN.set(asJson, "binary", b);
                return this;
            }

            public Meta() {
                this(new JsonObject());
            }

            @Override
            public String table() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "table");
            }

            public Meta table(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "table", v);
                return this;
            }

            @Override
            public Class<MetaData.Actor> domainIdentity() {
                return MetaData.Actor.class;
            }

            @Override
            public List<MetaData.Column> columns() {
                return Codec.LIST_$$ACTIVATE_API_METADATA_METADATA__COLUMN.get(this.asJson, "columns");
            }

            public Meta columns(List<MetaData.Column> v) {
                Codec.LIST_$$ACTIVATE_API_METADATA_METADATA__COLUMN.set(this.asJson, "columns", v);
                return this;
            }

            public Meta columnsDo(Consumer<List<MetaData.Column>> act) {
                var x = Optional.ofNullable(columns()).orElseGet(ArrayList::new);
                act.accept(x);
                columns(x);
                return this;
            }

            @Override
            public String identity() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "identity");
            }

            public Meta identity(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "identity", v);
                return this;
            }

            @Override
            public String name() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "name");
            }

            public Meta name(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "name", v);
                return this;
            }

            @Override
            public String description() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "description");
            }

            public Meta description(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "description", v);
                return this;
            }


            @Override
            public String identify() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "identify");
            }

            public Meta identify(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "identify", v);
                return this;
            }

            @Override
            public String role() {
                return this.asJson.containsKey("role") ? vat.api.implement.Codec.STRING.get(this.asJson,
                        "role") : MetaData.Actor.super.role();
            }

            public Meta role(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "role", v);
                return this;
            }

            @Override
            public JsonObject asJson() {
                if (!this.asJson.containsKey("role")) {
                    vat.api.implement.Codec.STRING.set(this.asJson, "role", MetaData.Actor.super.role());
                }
                return asJson;
            }

            public Meta copy() {
                return new Meta(asJson());
            }


        }


    }

    @Enhance
    interface Ability extends Data, Entity {
        @Computed
        default String role() {
            return "ability";
        }

        record Meta(JsonObject asJson) implements MetaData.Ability, Applicative<Meta> {
            @Override
            public Meta _this() {
                return this;
            }
            @Override
            public boolean binary() {
                return Optional.ofNullable(vat.api.implement.Codec.BOOLEAN.get(asJson, "binary")).orElse(false);
            }

            public Meta binary(boolean b) {
                vat.api.implement.Codec.BOOLEAN.set(asJson, "binary", b);
                return this;
            }


            public Meta() {
                this(new JsonObject());
            }

            @Override
            public String identify() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "identify");
            }

            public Meta identify(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "identify", v);
                return this;
            }

            @Override
            public String table() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "table");
            }

            public Meta table(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "table", v);
                return this;
            }

            @Override
            public Class<MetaData.Ability> domainIdentity() {
                return MetaData.Ability.class;
            }

            @Override
            public List<MetaData.Column> columns() {
                return Codec.LIST_$$ACTIVATE_API_METADATA_METADATA__COLUMN.get(this.asJson, "columns");
            }

            public Meta columns(List<MetaData.Column> v) {
                Codec.LIST_$$ACTIVATE_API_METADATA_METADATA__COLUMN.set(this.asJson, "columns", v);
                return this;
            }

            public Meta columnsDo(Consumer<List<MetaData.Column>> act) {
                var x = Optional.ofNullable(columns()).orElseGet(ArrayList::new);
                act.accept(x);
                columns(x);
                return this;
            }

            @Override
            public String identity() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "identity");
            }

            public Meta identity(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "identity", v);
                return this;
            }

            @Override
            public String name() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "name");
            }

            public Meta name(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "name", v);
                return this;
            }

            @Override
            public String description() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "description");
            }

            public Meta description(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "description", v);
                return this;
            }

            @Override
            public String role() {
                return this.asJson.containsKey("role") ? vat.api.implement.Codec.STRING.get(this.asJson,
                        "role") : MetaData.Ability.super.role();
            }

            public Meta role(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "role", v);
                return this;
            }

            @Override
            public JsonObject asJson() {
                if (!this.asJson.containsKey("role")) {
                    vat.api.implement.Codec.STRING.set(this.asJson, "role", MetaData.Ability.super.role());
                }
                return asJson;
            }

            public Meta copy() {
                return new Meta(asJson());
            }


        }


    }

    @Enhance
    interface Record extends Data, Entity {
        @Computed
        default String role() {
            return "record";
        }

        record Meta(JsonObject asJson) implements MetaData.Record, Applicative<Meta> {
            @Override
            public Meta _this() {
                return this;
            }
            public Meta() {
                this(new JsonObject());
            }
            @Override
            public boolean binary() {
                return Optional.ofNullable(vat.api.implement.Codec.BOOLEAN.get(asJson, "binary")).orElse(false);
            }

            public Meta binary(boolean b) {
                vat.api.implement.Codec.BOOLEAN.set(asJson, "binary", b);
                return this;
            }
            @Override
            public String identify() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "identify");
            }

            public Meta identify(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "identify", v);
                return this;
            }

            @Override
            public Class<MetaData.Record> domainIdentity() {
                return MetaData.Record.class;
            }

            @Override
            public List<MetaData.Column> columns() {
                return Codec.LIST_$$ACTIVATE_API_METADATA_METADATA__COLUMN.get(this.asJson, "columns");
            }

            public Meta columns(List<MetaData.Column> v) {
                Codec.LIST_$$ACTIVATE_API_METADATA_METADATA__COLUMN.set(this.asJson, "columns", v);
                return this;
            }

            public Meta columnsDo(Consumer<List<MetaData.Column>> act) {
                var x = Optional.ofNullable(columns()).orElseGet(ArrayList::new);
                act.accept(x);
                columns(x);
                return this;
            }

            @Override
            public String identity() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "identity");
            }

            public Meta identity(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "identity", v);
                return this;
            }

            @Override
            public String name() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "name");
            }

            public Meta name(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "name", v);
                return this;
            }

            @Override
            public String description() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "description");
            }

            public Meta description(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "description", v);
                return this;
            }

            @Override
            public String role() {
                return this.asJson.containsKey("role") ? vat.api.implement.Codec.STRING.get(this.asJson,
                        "role") : MetaData.Record.super.role();
            }

            public Meta role(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "role", v);
                return this;
            }

            @Override
            public String table() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "table");
            }

            public Meta table(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "table", v);
                return this;
            }


            @Override
            public JsonObject asJson() {
                if (!this.asJson.containsKey("role")) {
                    vat.api.implement.Codec.STRING.set(this.asJson, "role", MetaData.Record.super.role());
                }
                return asJson;
            }

            public Meta copy() {
                return new Meta(asJson());
            }

        }


    }

    @Enhance
    interface Object extends Data, MetaData, Properties {
        @Computed
        default String role() {
            return "data";
        }
        boolean binary();
        record Meta(JsonObject asJson) implements MetaData.Object, Applicative<Meta> {
            @Override
            public Meta _this() {
                return this;
            }
            @Override
            public boolean binary() {
                return Optional.ofNullable(vat.api.implement.Codec.BOOLEAN.get(asJson, "binary")).orElse(false);
            }

            public Meta binary(boolean b) {
                vat.api.implement.Codec.BOOLEAN.set(asJson, "binary", b);
                return this;
            }


            public Meta() {
                this(new JsonObject());
            }

            @Override
            public Class<MetaData.Object> domainIdentity() {
                return MetaData.Object.class;
            }

            @Override
            public String role() {
                return this.asJson.containsKey("role") ? vat.api.implement.Codec.STRING.get(this.asJson,
                        "role") : MetaData.Object.super.role();
            }

            public Meta role(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "role", v);
                return this;
            }

            @Override
            public List<MetaData.Property> properties() {
                return Codec.LIST_$$ACTIVATE_API_METADATA_METADATA__PROPERTY.get(this.asJson, "properties");
            }

            public Meta properties(List<MetaData.Property> v) {
                Codec.LIST_$$ACTIVATE_API_METADATA_METADATA__PROPERTY.set(this.asJson, "properties", v);
                return this;
            }

            public Meta propertiesDo(Consumer<List<Property>> act) {
                var x = Optional.ofNullable(properties()).orElseGet(ArrayList::new);
                act.accept(x);
                properties(x);
                return this;
            }

            @Override
            public String identity() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "identity");
            }

            public Meta identity(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "identity", v);
                return this;
            }

            @Override
            public String name() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "name");
            }

            public Meta name(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "name", v);
                return this;
            }

            @Override
            public String description() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "description");
            }

            public Meta description(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "description", v);
                return this;
            }

            public Meta copy() {
                return new Meta(asJson());
            }

        }
    }

    @Enhance
    interface EventKind extends Data, MetaData {

        long ordinal();

        String text();

        record Meta(JsonObject asJson) implements MetaData.EventKind, Applicative<Meta> {
            @Override
            public Meta _this() {
                return this;
            }


            public Meta() {
                this(new JsonObject());
            }

            @Override
            public Class<MetaData.EventKind> domainIdentity() {
                return MetaData.EventKind.class;
            }

            @Override
            public String identity() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "identity");
            }

            public Meta identity(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "identity", v);
                return this;
            }

            @Override
            public String name() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "name");
            }

            public Meta name(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "name", v);
                return this;
            }

            @Override
            public String description() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "description");
            }

            public Meta description(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "description", v);
                return this;
            }

            @Override
            public String text() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "text");
            }

            public Meta text(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "text", v);
                return this;
            }

            @Override
            public long ordinal() {
                return vat.api.implement.Codec.LONG.get(this.asJson, "ordinal");
            }

            public Meta ordinal(long v) {
                vat.api.implement.Codec.LONG.set(this.asJson, "ordinal", v);
                return this;
            }

            public Meta copy() {
                return new Meta(asJson());
            }

        }

    }

    @Enhance
    interface Event extends Data, MetaData, Properties {
        @Computed
        default String role() {
            return "event";
        }

        List<EventKind> kinds();

        record Meta(JsonObject asJson) implements MetaData.Event, Applicative<Meta> {
            @Override
            public Meta _this() {
                return this;
            }
             @Override
            public boolean binary() {
                return Optional.ofNullable(vat.api.implement.Codec.BOOLEAN.get(asJson, "binary")).orElse(false);
            }

            public Meta binary(boolean b) {
                vat.api.implement.Codec.BOOLEAN.set(asJson, "binary", b);
                return this;
            }


            public Meta() {
                this(new JsonObject());
            }

            @Override
            public Class<MetaData.Event> domainIdentity() {
                return MetaData.Event.class;
            }

            @Override
            public List<MetaData.Property> properties() {
                return Codec.LIST_$$ACTIVATE_API_METADATA_METADATA__PROPERTY.get(this.asJson, "properties");
            }

            public Meta properties(List<MetaData.Property> v) {
                Codec.LIST_$$ACTIVATE_API_METADATA_METADATA__PROPERTY.set(this.asJson, "properties", v);
                return this;
            }

            public Meta propertiesDo(Consumer<List<MetaData.Property>> act) {
                var x = Optional.ofNullable(properties()).orElseGet(ArrayList::new);
                act.accept(x);
                properties(x);
                return this;
            }

            @Override
            public String identity() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "identity");
            }

            public Meta identity(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "identity", v);
                return this;
            }

            @Override
            public String name() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "name");
            }

            public Meta name(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "name", v);
                return this;
            }

            @Override
            public String description() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "description");
            }

            public Meta description(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "description", v);
                return this;
            }

            @Override
            public List<MetaData.EventKind> kinds() {
                return Codec.LIST_$$ACTIVATE_API_METADATA_METADATA__EVENT_KIND.get(this.asJson, "kinds");
            }

            public Meta kinds(List<MetaData.EventKind> v) {
                Codec.LIST_$$ACTIVATE_API_METADATA_METADATA__EVENT_KIND.set(this.asJson, "kinds", v);
                return this;
            }

            public Meta kindsDo(Consumer<List<MetaData.EventKind>> act) {
                var x = Optional.ofNullable(kinds()).orElseGet(ArrayList::new);
                act.accept(x);
                kinds(x);
                return this;
            }

            @Override
            public String role() {
                return this.asJson.containsKey("role") ? vat.api.implement.Codec.STRING.get(this.asJson,
                        "role") : MetaData.Event.super.role();
            }

            public Meta role(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "role", v);
                return this;
            }

            @Override
            public JsonObject asJson() {
                if (!this.asJson.containsKey("role")) {
                    vat.api.implement.Codec.STRING.set(this.asJson, "role", MetaData.Event.super.role());
                }
                return asJson;
            }

            public Meta copy() {
                return new Meta(asJson());
            }

        }


    }

    @Enhance
    interface Action extends Data, MetaData {
        Type input();

        Type output();

        record Meta(JsonObject asJson) implements MetaData.Action, Applicative<Meta> {
            @Override
            public Meta _this() {
                return this;
            }


            public Meta() {
                this(new JsonObject());
            }

            @Override
            public Class<MetaData.Action> domainIdentity() {
                return MetaData.Action.class;
            }

            @Override
            public String identity() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "identity");
            }

            public Meta identity(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "identity", v);
                return this;
            }

            @Override
            public String name() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "name");
            }

            public Meta name(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "name", v);
                return this;
            }

            @Override
            public String description() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "description");
            }

            public Meta description(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "description", v);
                return this;
            }

            @Override
            public MetaData.Type input() {
                return Codec.ACTIVATE_API_METADATA_METADATA__TYPE.get(this.asJson, "input");
            }

            public Meta input(MetaData.Type v) {
                Codec.ACTIVATE_API_METADATA_METADATA__TYPE.set(this.asJson, "input", v);
                return this;
            }

            public Meta inputDo(UnaryOperator<MetaData.@Nullable Type> act) {
                var x = act.apply(input());
                return input(x);
            }

            @Override
            public MetaData.Type output() {
                return Codec.ACTIVATE_API_METADATA_METADATA__TYPE.get(this.asJson, "output");
            }

            public Meta output(MetaData.Type v) {
                Codec.ACTIVATE_API_METADATA_METADATA__TYPE.set(this.asJson, "output", v);
                return this;
            }

            public Meta outputDo(UnaryOperator<MetaData.@Nullable Type> act) {
                var x = act.apply(output());
                return output(x);
            }

            public Meta copy() {
                return new Meta(asJson());
            }

        }


    }


    interface ConfigEntry extends Data, MetaData {
        String path();

    }

    @Enhance
    interface ErrorEntry extends Data, ConfigEntry {
        default String codePath() {
            return path() + "/code";
        }

        default String userPath() {
            return path() + "/user";
        }

        default String modePath() {
            return path() + "/mode";
        }

        default String systemPath() {
            return path() + "/system";
        }

        List<Type> parameters();

        record Meta(JsonObject asJson) implements MetaData.ErrorEntry, Applicative<Meta> {
            @Override
            public Meta _this() {
                return this;
            }


            public Meta() {
                this(new JsonObject());
            }

            @Override
            public Class<MetaData.ErrorEntry> domainIdentity() {
                return MetaData.ErrorEntry.class;
            }

            @Override
            public String path() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "path");
            }

            public Meta path(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "path", v);
                return this;
            }

            @Override
            public String identity() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "identity");
            }

            public Meta identity(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "identity", v);
                return this;
            }

            @Override
            public String name() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "name");
            }

            public Meta name(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "name", v);
                return this;
            }

            @Override
            public String description() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "description");
            }

            public Meta description(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "description", v);
                return this;
            }

            @Override
            public List<MetaData.Type> parameters() {
                return Codec.LIST_$$ACTIVATE_API_METADATA_METADATA__TYPE.get(this.asJson, "parameters");
            }

            public Meta parameters(List<MetaData.Type> v) {
                Codec.LIST_$$ACTIVATE_API_METADATA_METADATA__TYPE.set(this.asJson, "parameters", v);
                return this;
            }

            public Meta parametersDo(Consumer<List<MetaData.Type>> act) {
                var x = Optional.ofNullable(parameters()).orElseGet(ArrayList::new);
                act.accept(x);
                parameters(x);
                return this;
            }

            public Meta copy() {
                return new Meta(asJson());
            }


        }


    }

    @Enhance
    interface ValueEntry extends Data, ConfigEntry {
        Type type();

        record Meta(JsonObject asJson) implements MetaData.ValueEntry, Applicative<Meta> {
            @Override
            public Meta _this() {
                return this;
            }


            public Meta() {
                this(new JsonObject());
            }

            @Override
            public Class<MetaData.ValueEntry> domainIdentity() {
                return MetaData.ValueEntry.class;
            }

            @Override
            public String path() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "path");
            }

            public Meta path(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "path", v);
                return this;
            }

            @Override
            public String identity() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "identity");
            }

            public Meta identity(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "identity", v);
                return this;
            }

            @Override
            public String name() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "name");
            }

            public Meta name(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "name", v);
                return this;
            }

            @Override
            public String description() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "description");
            }

            public Meta description(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "description", v);
                return this;
            }

            @Override
            public MetaData.Type type() {
                return Codec.ACTIVATE_API_METADATA_METADATA__TYPE.get(this.asJson, "type");
            }

            public Meta type(MetaData.Type v) {
                Codec.ACTIVATE_API_METADATA_METADATA__TYPE.set(this.asJson, "type", v);
                return this;
            }

            public Meta typeDo(UnaryOperator<@Nullable Type> act) {
                var x = act.apply(type());
                return type(x);
            }

            public Meta copy() {
                return new Meta(asJson());
            }


        }


    }


    @Enhance
    interface Config extends Data, MetaData {
        List<ConfigEntry> properties();

        record Meta(JsonObject asJson) implements MetaData.Config, Applicative<Meta> {
            @Override
            public Meta _this() {
                return this;
            }


            public Meta() {
                this(new JsonObject());
            }

            @Override
            public Class<MetaData.Config> domainIdentity() {
                return MetaData.Config.class;
            }

            @Override
            public String identity() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "identity");
            }

            public Meta identity(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "identity", v);
                return this;
            }

            @Override
            public String name() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "name");
            }

            public Meta name(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "name", v);
                return this;
            }

            @Override
            public String description() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "description");
            }

            public Meta description(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "description", v);
                return this;
            }

            @Override
            public List<MetaData.ConfigEntry> properties() {
                return Codec.LIST_$$ACTIVATE_API_METADATA_METADATA__CONFIG_ENTRY.get(this.asJson, "properties");
            }

            public Meta properties(List<MetaData.ConfigEntry> v) {
                Codec.LIST_$$ACTIVATE_API_METADATA_METADATA__CONFIG_ENTRY.set(this.asJson, "properties", v);
                return this;
            }

            public Meta propertiesDo(Consumer<List<MetaData.ConfigEntry>> act) {
                var x = Optional.ofNullable(properties()).orElseGet(ArrayList::new);
                act.accept(x);
                properties(x);
                return this;
            }

            public Meta copy() {
                return new Meta(asJson());
            }

        }


    }

    @Enhance
    interface Uses extends Data, MetaData {
        String address();

        String configPath();

        ReferenceType type();

        record Meta(JsonObject asJson) implements MetaData.Uses, Applicative<Meta> {
            @Override
            public Meta _this() {
                return this;
            }


            public Meta() {
                this(new JsonObject());
            }

            @Override
            public Class<MetaData.Uses> domainIdentity() {
                return MetaData.Uses.class;
            }

            @Override
            public String identity() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "identity");
            }

            public Meta identity(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "identity", v);
                return this;
            }

            @Override
            public String name() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "name");
            }

            public Meta name(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "name", v);
                return this;
            }

            @Override
            public String description() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "description");
            }

            public Meta description(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "description", v);
                return this;
            }

            @Override
            public String address() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "address");
            }

            public Meta address(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "address", v);
                return this;
            }

            @Override
            public String configPath() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "configPath");
            }

            public Meta configPath(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "configPath", v);
                return this;
            }

            @Override
            public ReferenceType type() {
                return Codec.REFERENCE_TYPE_DATA.get(asJson.getJsonObject("type"));
            }

            public Meta type(ReferenceType type) {
                asJson.put("type", Codec.REFERENCE_TYPE_DATA.set(type));
                return this;
            }

            public Meta copy() {
                return new Meta(asJson());
            }


        }


    }

    @Enhance
    interface Publish extends Data, MetaData {
        String address();

        String configPath();

        ReferenceType type();

        record Meta(JsonObject asJson) implements MetaData.Publish, Applicative<Meta> {
            @Override
            public Meta _this() {
                return this;
            }


            public Meta() {
                this(new JsonObject());
            }

            @Override
            public Class<MetaData.Publish> domainIdentity() {
                return MetaData.Publish.class;
            }

            @Override
            public String identity() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "identity");
            }

            public Meta identity(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "identity", v);
                return this;
            }

            @Override
            public String name() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "name");
            }

            public Meta name(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "name", v);
                return this;
            }

            @Override
            public String description() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "description");
            }

            public Meta description(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "description", v);
                return this;
            }

            @Override
            public String address() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "address");
            }

            public Meta address(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "address", v);
                return this;
            }

            @Override
            public String configPath() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "configPath");
            }

            @Override
            public ReferenceType type() {
                return Codec.REFERENCE_TYPE_DATA.get(asJson.getJsonObject("type"));
            }

            public Meta type(ReferenceType type) {
                asJson.put("type", Codec.REFERENCE_TYPE_DATA.set(type));
                return this;
            }

            public Meta configPath(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "configPath", v);
                return this;
            }

            public Meta copy() {
                return new Meta(asJson());
            }


        }


    }

    @Enhance
    interface Subscribe extends Data, MetaData {
        String address();

        String configPath();

        ReferenceType type();

        record Meta(JsonObject asJson) implements MetaData.Subscribe, Applicative<Meta> {
            @Override
            public Meta _this() {
                return this;
            }


            public Meta() {
                this(new JsonObject());
            }

            @Override
            public Class<MetaData.Subscribe> domainIdentity() {
                return MetaData.Subscribe.class;
            }

            @Override
            public String identity() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "identity");
            }

            public Meta identity(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "identity", v);
                return this;
            }

            @Override
            public String name() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "name");
            }

            public Meta name(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "name", v);
                return this;
            }

            @Override
            public String description() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "description");
            }

            public Meta description(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "description", v);
                return this;
            }

            @Override
            public String address() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "address");
            }

            public Meta address(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "address", v);
                return this;
            }

            @Override
            public String configPath() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "configPath");
            }

            public Meta configPath(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "configPath", v);
                return this;
            }

            @Override
            public ReferenceType type() {
                return Codec.REFERENCE_TYPE_DATA.get(asJson.getJsonObject("type"));
            }

            public Meta type(ReferenceType type) {
                asJson.put("type", Codec.REFERENCE_TYPE_DATA.set(type));
                return this;
            }

            public Meta copy() {
                return new Meta(asJson());
            }

        }


    }

    @Enhance
    interface Domain extends Data, MetaData {
        List<Actor> actors();

        List<Ability> abilities();

        List<Record> records();

        List<Event> events();

        List<Object> data();

        List<Action> actions();

        List<Publish> publish();

        List<Subscribe> subscribe();

        List<Uses> uses();

        Config config();

        record Meta(JsonObject asJson) implements MetaData.Domain, Applicative<Meta> {
            @Override
            public Meta _this() {
                return this;
            }


            public Meta() {
                this(new JsonObject());
            }

            @Override
            public Class<MetaData.Domain> domainIdentity() {
                return MetaData.Domain.class;
            }

            @Override
            public String identity() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "identity");
            }

            public Meta identity(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "identity", v);
                return this;
            }

            @Override
            public String name() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "name");
            }

            public Meta name(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "name", v);
                return this;
            }

            @Override
            public String description() {
                return vat.api.implement.Codec.STRING.get(this.asJson, "description");
            }

            public Meta description(String v) {
                vat.api.implement.Codec.STRING.set(this.asJson, "description", v);
                return this;
            }

            @Override
            public List<MetaData.Actor> actors() {
                return Codec.LIST_$$ACTIVATE_API_METADATA_METADATA__ACTOR.get(this.asJson, "actors");
            }

            public Meta actors(List<MetaData.Actor> v) {
                Codec.LIST_$$ACTIVATE_API_METADATA_METADATA__ACTOR.set(this.asJson, "actors", v);
                return this;
            }

            public Meta actorsDo(Consumer<List<MetaData.Actor>> act) {
                var x = Optional.ofNullable(actors()).orElseGet(ArrayList::new);
                act.accept(x);
                actors(x);
                return this;
            }

            @Override
            public List<MetaData.Ability> abilities() {
                return Codec.LIST_$$ACTIVATE_API_METADATA_METADATA__ABILITY.get(this.asJson, "abilities");
            }

            public Meta abilities(List<MetaData.Ability> v) {
                Codec.LIST_$$ACTIVATE_API_METADATA_METADATA__ABILITY.set(this.asJson, "abilities", v);
                return this;
            }

            public Meta abilitiesDo(Consumer<List<MetaData.Ability>> act) {
                var x = Optional.ofNullable(abilities()).orElseGet(ArrayList::new);
                act.accept(x);
                abilities(x);
                return this;
            }

            @Override
            public List<MetaData.Record> records() {
                return Codec.LIST_$$ACTIVATE_API_METADATA_METADATA__RECORD.get(this.asJson, "records");
            }

            public Meta records(List<MetaData.Record> v) {
                Codec.LIST_$$ACTIVATE_API_METADATA_METADATA__RECORD.set(this.asJson, "records", v);
                return this;
            }

            public Meta recordsDo(Consumer<List<MetaData.Record>> act) {
                var x = Optional.ofNullable(records()).orElseGet(ArrayList::new);
                act.accept(x);
                records(x);
                return this;
            }

            @Override
            public List<MetaData.Event> events() {
                return Codec.LIST_$$ACTIVATE_API_METADATA_METADATA__EVENT.get(this.asJson, "events");
            }

            public Meta events(List<MetaData.Event> v) {
                Codec.LIST_$$ACTIVATE_API_METADATA_METADATA__EVENT.set(this.asJson, "events", v);
                return this;
            }

            public Meta eventsDo(Consumer<List<MetaData.Event>> act) {
                var x = Optional.ofNullable(events()).orElseGet(ArrayList::new);
                act.accept(x);
                events(x);
                return this;
            }

            @Override
            public List<MetaData.Object> data() {
                return Codec.LIST_$$ACTIVATE_API_METADATA_METADATA__OBJECT.get(this.asJson, "data");
            }

            public Meta data(List<MetaData.Object> v) {
                Codec.LIST_$$ACTIVATE_API_METADATA_METADATA__OBJECT.set(this.asJson, "data", v);
                return this;
            }

            public Meta dataDo(Consumer<List<MetaData.Object>> act) {
                var x = Optional.ofNullable(data()).orElseGet(ArrayList::new);
                act.accept(x);
                data(x);
                return this;
            }

            @Override
            public List<MetaData.Action> actions() {
                return Codec.LIST_$$ACTIVATE_API_METADATA_METADATA__ACTION.get(this.asJson, "actions");
            }

            public Meta actions(List<MetaData.Action> v) {
                Codec.LIST_$$ACTIVATE_API_METADATA_METADATA__ACTION.set(this.asJson, "actions", v);
                return this;
            }

            public Meta actionsDo(Consumer<List<MetaData.Action>> act) {
                var x = Optional.ofNullable(actions()).orElseGet(ArrayList::new);
                act.accept(x);
                actions(x);
                return this;
            }

            @Override
            public List<MetaData.Publish> publish() {
                return Codec.LIST_$$ACTIVATE_API_METADATA_METADATA__PUBLISH.get(this.asJson, "publish");
            }

            public Meta publish(List<MetaData.Publish> v) {
                Codec.LIST_$$ACTIVATE_API_METADATA_METADATA__PUBLISH.set(this.asJson, "publish", v);
                return this;
            }

            public Meta publishDo(Consumer<List<MetaData.Publish>> act) {
                var x = Optional.ofNullable(publish()).orElseGet(ArrayList::new);
                act.accept(x);
                publish(x);
                return this;
            }

            @Override
            public List<MetaData.Subscribe> subscribe() {
                return Codec.LIST_$$ACTIVATE_API_METADATA_METADATA__SUBSCRIBE.get(this.asJson, "subscribe");
            }

            public Meta subscribe(List<MetaData.Subscribe> v) {
                Codec.LIST_$$ACTIVATE_API_METADATA_METADATA__SUBSCRIBE.set(this.asJson, "subscribe", v);
                return this;
            }

            public Meta subscribeDo(Consumer<List<MetaData.Subscribe>> act) {
                var x = Optional.ofNullable(subscribe()).orElseGet(ArrayList::new);
                act.accept(x);
                subscribe(x);
                return this;
            }

            @Override
            public List<MetaData.Uses> uses() {
                return Codec.LIST_$$ACTIVATE_API_METADATA_METADATA__USES.get(this.asJson, "uses");
            }

            public Meta uses(List<MetaData.Uses> v) {
                Codec.LIST_$$ACTIVATE_API_METADATA_METADATA__USES.set(this.asJson, "uses", v);
                return this;
            }

            public Meta usesDo(Consumer<List<MetaData.Uses>> act) {
                var x = Optional.ofNullable(uses()).orElseGet(ArrayList::new);
                act.accept(x);
                uses(x);
                return this;
            }

            @Override
            public MetaData.Config config() {
                return Codec.ACTIVATE_API_METADATA_METADATA__CONFIG.get(this.asJson, "config");
            }

            public Meta config(MetaData.Config v) {
                Codec.ACTIVATE_API_METADATA_METADATA__CONFIG.set(this.asJson, "config", v);
                return this;
            }

            public Meta configDo(UnaryOperator<MetaData.@Nullable Config> act) {
                var x = act.apply(config());
                return config(x);
            }

            public Meta copy() {
                return new Meta(asJson());
            }

        }


    }

    interface Codec {
        DataProperty<Type> ACTIVATE_API_METADATA_METADATA__TYPE = new CombineProperty<>(
                (o, k) -> data(o.getJsonObject(k), null),
                (o, k, v) -> o.put(k, v == null ? null : v.asJson().put("$type", v.getClass().getName())));

        DataCodec<ListType.Meta, MetaData.ListType> LIST_TYPE_DATA = DataCodec.closure(ListType.Meta::new,
                ListType.Meta.class);

        DataCodec<ArrayType.Meta, MetaData.ArrayType> ARRAY_TYPE_DATA = DataCodec.closure(ArrayType.Meta::new,
                ArrayType.Meta.class);

        DataCodec<ProjectionType.Meta, MetaData.ProjectionType> PROJECTION_TYPE_DATA = DataCodec.closure(
                MetaData.ProjectionType.Meta::new, ProjectionType.Meta.class);

        DataCodec<ReferenceType.Meta, MetaData.ReferenceType> REFERENCE_TYPE_DATA = DataCodec.closure(
                ReferenceType.Meta::new, ReferenceType.Meta.class);

        DataCodec<EnumerationEntry.Meta, MetaData.EnumerationEntry> ENUMERATION_ENTRY_DATA = DataCodec.closure(
                EnumerationEntry.Meta::new, EnumerationEntry.Meta.class);

        DataProperty<List<MetaData.EnumerationEntry>> LIST_$$ACTIVATE_API_METADATA_METADATA__ENUMERATION_ENTRY = new CombineProperty<>(
                (o, k) -> list(o.getJsonArray(k), ArrayList::new,
                        (p_k, r_k) -> Codec.ENUMERATION_ENTRY_DATA.get(r_k.getJsonObject(p_k))),
                (o, k, v) -> o.put(k, list(v, (p_k, r_k, v_k) -> r_k.set(p_k, v_k == null ? null : v_k.asJson()))));

        DataCodec<EnumerationType.Meta, MetaData.EnumerationType> ENUMERATION_TYPE_DATA = DataCodec.closure(
                EnumerationType.Meta::new, EnumerationType.Meta.class);

        DataCodec<Functor.Meta, MetaData.Functor> FUNCTOR_DATA = DataCodec.closure(Functor.Meta::new,
                Functor.Meta.class);

        DataProperty<List<MetaData.Functor>> LIST_$$ACTIVATE_API_METADATA_METADATA__FUNCTOR = new CombineProperty<>(
                (o, k) -> list(o.getJsonArray(k), ArrayList::new,
                        (p_k, r_k) -> Codec.FUNCTOR_DATA.get(r_k.getJsonObject(p_k))),
                (o, k, v) -> o.put(k, list(v, (p_k, r_k, v_k) -> r_k.set(p_k, v_k == null ? null : v_k.asJson()))));

        DataCodec<Property.Meta, MetaData.Property> PROPERTY_DATA = DataCodec.closure(Property.Meta::new,
                Property.Meta.class);

        DataCodec<Column.Meta, MetaData.Column> COLUMN_DATA = DataCodec.closure(Column.Meta::new, Column.Meta.class);

        DataProperty<List<MetaData.Column>> LIST_$$ACTIVATE_API_METADATA_METADATA__COLUMN = new CombineProperty<>(
                (o, k) -> list(o.getJsonArray(k), ArrayList::new,
                        (p_k, r_k) -> Codec.COLUMN_DATA.get(r_k.getJsonObject(p_k))),
                (o, k, v) -> o.put(k, list(v, (p_k, r_k, v_k) -> r_k.set(p_k, v_k == null ? null : v_k.asJson()))));

        DataCodec<Actor.Meta, MetaData.Actor> ACTOR_DATA = DataCodec.closure(Actor.Meta::new, Actor.Meta.class);

        DataCodec<Ability.Meta, MetaData.Ability> ABILITY_DATA = DataCodec.closure(Ability.Meta::new,
                Ability.Meta.class);

        DataCodec<Record.Meta, MetaData.Record> RECORD_DATA = DataCodec.closure(Record.Meta::new, Record.Meta.class);

        DataProperty<List<MetaData.Property>> LIST_$$ACTIVATE_API_METADATA_METADATA__PROPERTY = new CombineProperty<>(
                (o, k) -> list(o.getJsonArray(k), ArrayList::new,
                        (p_k, r_k) -> Codec.PROPERTY_DATA.get(r_k.getJsonObject(p_k))),
                (o, k, v) -> o.put(k, list(v, (p_k, r_k, v_k) -> r_k.set(p_k, v_k == null ? null : v_k.asJson()))));

        DataCodec<Object.Meta, MetaData.Object> OBJECT_DATA = DataCodec.closure(Object.Meta::new, Object.Meta.class);

        DataCodec<EventKind.Meta, MetaData.EventKind> EVENT_KIND_DATA = DataCodec.closure(EventKind.Meta::new,
                EventKind.Meta.class);

        DataProperty<List<MetaData.EventKind>> LIST_$$ACTIVATE_API_METADATA_METADATA__EVENT_KIND = new CombineProperty<>(
                (o, k) -> list(o.getJsonArray(k), ArrayList::new,
                        (p_k, r_k) -> Codec.EVENT_KIND_DATA.get(r_k.getJsonObject(p_k))),
                (o, k, v) -> o.put(k, list(v, (p_k, r_k, v_k) -> r_k.set(p_k, v_k == null ? null : v_k.asJson()))));

        DataCodec<Event.Meta, MetaData.Event> EVENT_DATA = DataCodec.closure(Event.Meta::new, Event.Meta.class);

        DataCodec<Action.Meta, MetaData.Action> ACTION_DATA = DataCodec.closure(Action.Meta::new, Action.Meta.class);

        DataProperty<List<MetaData.Type>> LIST_$$ACTIVATE_API_METADATA_METADATA__TYPE = new CombineProperty<>(
                (o, k) -> list(o.getJsonArray(k), ArrayList::new, (p_k, r_k) -> data(r_k.getJsonObject(p_k), null)),
                (o, k, v) -> o.put(k, list(v, (p_k, r_k, v_k) -> r_k.set(p_k, v_k == null ? null : v_k.asJson()
                        .put("$type",
                                v_k.getClass()
                                        .getName())))));

        DataCodec<ErrorEntry.Meta, MetaData.ErrorEntry> ERROR_ENTRY_DATA = DataCodec.closure(ErrorEntry.Meta::new,
                ErrorEntry.Meta.class);

        DataCodec<ValueEntry.Meta, MetaData.ValueEntry> VALUE_ENTRY_DATA = DataCodec.closure(ValueEntry.Meta::new,
                ValueEntry.Meta.class);

        DataProperty<List<MetaData.ConfigEntry>> LIST_$$ACTIVATE_API_METADATA_METADATA__CONFIG_ENTRY = new CombineProperty<>(
                (o, k) -> list(o.getJsonArray(k), ArrayList::new, (p_k, r_k) -> data(r_k.getJsonObject(p_k), null)),
                (o, k, v) -> o.put(k, list(v, (p_k, r_k, v_k) -> r_k.set(p_k, v_k == null ? null : v_k.asJson()
                        .put("$type",
                                v_k.getClass()
                                        .getName())))));

        DataCodec<Config.Meta, MetaData.Config> CONFIG_DATA = DataCodec.closure(Config.Meta::new, Config.Meta.class);

        DataCodec<Uses.Meta, MetaData.Uses> USES_DATA = DataCodec.closure(Uses.Meta::new, Uses.Meta.class);

        DataCodec<Publish.Meta, MetaData.Publish> PUBLISH_DATA = DataCodec.closure(Publish.Meta::new,
                Publish.Meta.class);

        DataCodec<Subscribe.Meta, MetaData.Subscribe> SUBSCRIBE_DATA = DataCodec.closure(Subscribe.Meta::new,
                Subscribe.Meta.class);

        DataProperty<List<MetaData.Actor>> LIST_$$ACTIVATE_API_METADATA_METADATA__ACTOR = new CombineProperty<>(
                (o, k) -> list(o.getJsonArray(k), ArrayList::new,
                        (p_k, r_k) -> Codec.ACTOR_DATA.get(r_k.getJsonObject(p_k))),
                (o, k, v) -> o.put(k, list(v, (p_k, r_k, v_k) -> r_k.set(p_k, v_k == null ? null : v_k.asJson()))));

        DataProperty<List<MetaData.Ability>> LIST_$$ACTIVATE_API_METADATA_METADATA__ABILITY = new CombineProperty<>(
                (o, k) -> list(o.getJsonArray(k), ArrayList::new,
                        (p_k, r_k) -> Codec.ABILITY_DATA.get(r_k.getJsonObject(p_k))),
                (o, k, v) -> o.put(k, list(v, (p_k, r_k, v_k) -> r_k.set(p_k, v_k == null ? null : v_k.asJson()))));

        DataProperty<List<MetaData.Record>> LIST_$$ACTIVATE_API_METADATA_METADATA__RECORD = new CombineProperty<>(
                (o, k) -> list(o.getJsonArray(k), ArrayList::new,
                        (p_k, r_k) -> Codec.RECORD_DATA.get(r_k.getJsonObject(p_k))),
                (o, k, v) -> o.put(k, list(v, (p_k, r_k, v_k) -> r_k.set(p_k, v_k == null ? null : v_k.asJson()))));

        DataProperty<List<MetaData.Event>> LIST_$$ACTIVATE_API_METADATA_METADATA__EVENT = new CombineProperty<>(
                (o, k) -> list(o.getJsonArray(k), ArrayList::new,
                        (p_k, r_k) -> Codec.EVENT_DATA.get(r_k.getJsonObject(p_k))),
                (o, k, v) -> o.put(k, list(v, (p_k, r_k, v_k) -> r_k.set(p_k, v_k == null ? null : v_k.asJson()))));

        DataProperty<List<MetaData.Object>> LIST_$$ACTIVATE_API_METADATA_METADATA__OBJECT = new CombineProperty<>(
                (o, k) -> list(o.getJsonArray(k), ArrayList::new,
                        (p_k, r_k) -> Codec.OBJECT_DATA.get(r_k.getJsonObject(p_k))),
                (o, k, v) -> o.put(k, list(v, (p_k, r_k, v_k) -> r_k.set(p_k, v_k == null ? null : v_k.asJson()))));

        DataProperty<List<MetaData.Action>> LIST_$$ACTIVATE_API_METADATA_METADATA__ACTION = new CombineProperty<>(
                (o, k) -> list(o.getJsonArray(k), ArrayList::new,
                        (p_k, r_k) -> Codec.ACTION_DATA.get(r_k.getJsonObject(p_k))),
                (o, k, v) -> o.put(k, list(v, (p_k, r_k, v_k) -> r_k.set(p_k, v_k == null ? null : v_k.asJson()))));

        DataProperty<List<MetaData.Publish>> LIST_$$ACTIVATE_API_METADATA_METADATA__PUBLISH = new CombineProperty<>(
                (o, k) -> list(o.getJsonArray(k), ArrayList::new,
                        (p_k, r_k) -> Codec.PUBLISH_DATA.get(r_k.getJsonObject(p_k))),
                (o, k, v) -> o.put(k, list(v, (p_k, r_k, v_k) -> r_k.set(p_k, v_k == null ? null : v_k.asJson()))));

        DataProperty<List<MetaData.Subscribe>> LIST_$$ACTIVATE_API_METADATA_METADATA__SUBSCRIBE = new CombineProperty<>(
                (o, k) -> list(o.getJsonArray(k), ArrayList::new,
                        (p_k, r_k) -> Codec.SUBSCRIBE_DATA.get(r_k.getJsonObject(p_k))),
                (o, k, v) -> o.put(k, list(v, (p_k, r_k, v_k) -> r_k.set(p_k, v_k == null ? null : v_k.asJson()))));

        DataProperty<List<MetaData.Uses>> LIST_$$ACTIVATE_API_METADATA_METADATA__USES = new CombineProperty<>(
                (o, k) -> list(o.getJsonArray(k), ArrayList::new,
                        (p_k, r_k) -> Codec.USES_DATA.get(r_k.getJsonObject(p_k))),
                (o, k, v) -> o.put(k, list(v, (p_k, r_k, v_k) -> r_k.set(p_k, v_k == null ? null : v_k.asJson()))));

        DataProperty<MetaData.Config> ACTIVATE_API_METADATA_METADATA__CONFIG = new CombineProperty<>(
                (o, k) -> Codec.CONFIG_DATA.get(o.getJsonObject(k)),
                (o, k, v) -> o.put(k, v == null ? null : v.asJson()));

        DataCodec<Domain.Meta, MetaData.Domain> DOMAIN_DATA = DataCodec.closure(Domain.Meta::new, Domain.Meta.class);
    }

    static Map<String, Domain> load() {
        var out = new HashMap<String, Domain>();
        try {
            var res = Thread.currentThread().getContextClassLoader().getResources("META-INF/meta/");
            StreamSupport.stream(
                            Spliterators.spliteratorUnknownSize(res.asIterator(), Spliterator.ORDERED | Spliterator.NONNULL),
                            false)
                    .forEach(p -> {
                        if (p == null) return;
                        try {
                            var uri = p.toURI();
                            if (uri.getScheme().equals("jar")) {
                                var fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
                                try (fileSystem; var s = Files.list(fileSystem.getPath("META-INF/meta/"))) {
                                    s.forEach(v -> {
                                        try {
                                            var jo = new JsonObject(Files.readString(v));
                                            var dom = Codec.DOMAIN_DATA.get(jo);
                                            out.put(dom.identity(), dom);
                                        } catch (IOException e) {
                                            log.error("loading {} fail", v, e);
                                        }

                                    });

                                }
                            } else {
                                try (var s = Files.list(Paths.get(p.toURI()))) {
                                    s.forEach(v -> {
                                        try {
                                            var jo = new JsonObject(Files.readString(v));
                                            var dom = Codec.DOMAIN_DATA.get(jo);
                                            out.put(dom.identity(), dom);
                                        } catch (IOException e) {
                                            log.error("loading {} fail", v, e);
                                        }

                                    });
                                }
                            }
                        } catch (Exception ex) {
                            log.error("process {} fail", p, ex);
                        }
                    });
        } catch (IOException e) {
            log.error("load meta resources fail", e);
        }
        return out;
    }
}

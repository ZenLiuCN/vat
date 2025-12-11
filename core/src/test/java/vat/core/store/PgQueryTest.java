package vat.core.store;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.pgclient.PgBuilder;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.templates.SqlTemplate;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.ExtendWith;
import vat.api.Actor;
import vat.api.Data;
import vat.api.Record;
import vat.api.implement.Codec;
import vat.api.implement.Stored;
import vat.api.meta.*;
import vat.api.store.Dialect;
import vat.api.store.Field;
import vat.api.store.Model;
import vat.api.trait.Applicative;
import vat.api.trait.History;
import vat.api.utils.Fn;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;

///
/// @author Zen.Liu
/// @since 2025-11-14

@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledIfSystemProperty(
        named = "PG_CONN",
        matches = ".+",  // 匹配任何非空值
        disabledReason = "PG_CONN system property is not set or empty"
)
@Slf4j
public class PgQueryTest {

    @Enhance
    @Describe(value = "_USERS_USER")
    @Table("foundation_users_user")
    public interface User extends Actor.Base {
        @Describe("_USERS_USER_PROFILE")
        JsonObject profile();
    }

    public record UserData(JsonObject asJson) implements Applicative<UserData>, User {
        public UserData() {
            this(new JsonObject());
        }

        public UserData(JsonObject js, Void ignore) {
            this(UserData.fromJS(js));
        }

        public static UserData from(User t) {
            return t == null ? null : t instanceof UserData u ? u : new UserData(t.asJson());
        }

        @Override
        public final Class<User> domainIdentity() {
            return User.class;
        }

        @Override
        public Long id() {
            return Codec.LONG.get(this.asJson, "id");
        }

        public UserData id(Long v) {
            Codec.LONG.set(this.asJson, "id", v);
            return this;
        }

        @Override
        public int version() {
            return Codec.INT.get(this.asJson, "version");
        }

        public UserData version(int v) {
            Codec.INT.set(this.asJson, "version", v);
            return this;
        }

        @Override
        public boolean removed() {
            return Codec.BOOLEAN.get(this.asJson, "removed");
        }

        public UserData removed(boolean v) {
            Codec.BOOLEAN.set(this.asJson, "removed", v);
            return this;
        }

        @Override
        public long creator() {
            return Codec.LONG.get(this.asJson, "creator");
        }

        public UserData creator(long v) {
            Codec.LONG.set(this.asJson, "creator", v);
            return this;
        }

        @Override
        public Instant createdAt() {
            return Codec.INSTANT.get(this.asJson, "createdAt");
        }

        public UserData createdAt(Instant v) {
            Codec.INSTANT.set(this.asJson, "createdAt", v);
            return this;
        }

        @Override
        public long modifier() {
            return Codec.LONG.get(this.asJson, "modifier");
        }

        public UserData modifier(long v) {
            Codec.LONG.set(this.asJson, "modifier", v);
            return this;
        }

        @Override
        public Instant modifiedAt() {
            return Codec.INSTANT.get(this.asJson, "modifiedAt");
        }

        public UserData modifiedAt(Instant v) {
            Codec.INSTANT.set(this.asJson, "modifiedAt", v);
            return this;
        }

        @Override
        public JsonObject profile() {
            return Codec.JSON_OBJECT.get(this.asJson, "profile");
        }

        public UserData profile(JsonObject v) {
            Codec.JSON_OBJECT.set(this.asJson, "profile", v);
            return this;
        }

        public UserData profileDo(Consumer<JsonObject> act) {
            var x = Optional.ofNullable(profile()).orElseGet(JsonObject::new);
            act.accept(x);
            profile(x);
            return this;
        }

        public <R> R apply(Function<UserData, R> m) {
            return m.apply(this);
        }

        @Override
        public UserData _this() {
            return this;
        }


        public UserData copy() {
            return new UserData(toJson());
        }

        @Override
        public final JsonObject toJS() {
            var js = toJson();
            Codec.toJs(js, JsonObject::getLong, "id");
            Codec.toJs(js, JsonObject::getLong, "creator");
            Codec.toJs(js, JsonObject::getLong, "modifier");
            return js;
        }

        public static JsonObject fromJS(JsonObject js) {
            Codec.fromJs(js, JsonObject::getString, Long::parseLong, "id");
            Codec.fromJs(js, JsonObject::getString, Long::parseLong, "creator");
            Codec.fromJs(js, JsonObject::getString, Long::parseLong, "modifier");
            return js;
        }
    }


    public static final class UserStore extends Model.Base<Long, User, UserStore>  {
        public static final Function<Model<?>, Field<?>[]> FACTORY = (m) -> new Field<?>[]{new Field.LongField("id", "id", m),
                new Field.IntegerField("version", "version", m),
                new Field.BooleanField("removed", "removed", m),
                new Field.LongField("creator", "creator", m),
                new Field.InstantField("created_at", "createdAt", m),
                new Field.LongField("modifier", "modifier", m),
                new Field.InstantField("modified_at", "modifiedAt", m),
                new Field.JsonObjectField("profile", "profile", m),
        };

        public static final Class<User> TYPE = User.class;

        public static final UserStore MODEL = new UserStore();

        public static final Storage<Long, User, UserStore> STORAGE = (q, d, s) -> MODEL.copy(s)._with(q, d);

        public UserStore(String schema) {
            super(schema, "foundation_users_user", UserData::new, 0, 1, 2, 3, 4, 5, 6, -1);
        }

        public UserStore() {
            this(null);
        }

        public Field.LongField id() {
            return field(0);
        }

        public Field.IntegerField version() {
            return field(1);
        }

        public Field.BooleanField removed() {
            return field(2);
        }

        public Field.LongField creator() {
            return field(3);
        }

        public Field.InstantField createdAt() {
            return field(4);
        }

        public Field.LongField modifier() {
            return field(5);
        }

        public Field.InstantField modifiedAt() {
            return field(6);
        }

        public Field.JsonObjectField profile() {
            return field(7);
        }

        @Override
        protected UserStore _self() {
            return this;
        }

        @Override
        protected Field<?>[] buildFields() {
            return FACTORY.apply(this);
        }

        @Override
        protected UserStore copy(@Nullable String schema) {
            return schema == null ? new UserStore(_schema) : new UserStore(schema);
        }
    }

    @Enhance
    @Describe(value = "_USERS_CERTIFICATE", identity = "certIdentity")
    @Table("foundation_users_certificate")
    public interface Certificate extends Record.Base, History {
        @Describe("_USERS_CERTIFICATE_USER")
        @Identity.Reference(value = User.class)
        long user();

        @Describe("_USERS_CERTIFICATE_KIND")
        int kind();

        @Describe("_USERS_CERTIFICATE_IDENTIFIER")
        @Column(size = 128, unique = {"kind", "identifier"})
        String identifier();

        @Describe("_USERS_CERTIFICATE_CERTIFICATE")
        JsonObject certificate();


    }


    public record CertificateData(JsonObject asJson) implements Applicative<CertificateData>, Certificate {
        @Override
        public CertificateData _this() {
            return this;
        }

        public CertificateData() {
            this(new JsonObject());
        }

        public CertificateData(JsonObject js, Void ignore) {
            this(fromJS(js));
        }

        public final Class<Certificate> domainIdentity() {
            return Certificate.class;
        }

        public Long id() {
            return (Long) Codec.LONG.get(this.asJson, "id");
        }

        public CertificateData id(Long v) {
            Codec.LONG.set(this.asJson, "id", v);
            return this;
        }

        public int version() {
            return (Integer) Codec.INT.get(this.asJson, "version");
        }

        public CertificateData version(int v) {
            Codec.INT.set(this.asJson, "version", v);
            return this;
        }

        public boolean removed() {
            return (Boolean) Codec.BOOLEAN.get(this.asJson, "removed");
        }

        public CertificateData removed(boolean v) {
            Codec.BOOLEAN.set(this.asJson, "removed", v);
            return this;
        }

        public long creator() {
            return (Long) Codec.LONG.get(this.asJson, "creator");
        }

        public CertificateData creator(long v) {
            Codec.LONG.set(this.asJson, "creator", v);
            return this;
        }

        public Instant createdAt() {
            return (Instant) Codec.INSTANT.get(this.asJson, "createdAt");
        }

        public CertificateData createdAt(Instant v) {
            Codec.INSTANT.set(this.asJson, "createdAt", v);
            return this;
        }

        public long modifier() {
            return (Long) Codec.LONG.get(this.asJson, "modifier");
        }

        public CertificateData modifier(long v) {
            Codec.LONG.set(this.asJson, "modifier", v);
            return this;
        }

        public Instant modifiedAt() {
            return (Instant) Codec.INSTANT.get(this.asJson, "modifiedAt");
        }

        public CertificateData modifiedAt(Instant v) {
            Codec.INSTANT.set(this.asJson, "modifiedAt", v);
            return this;
        }

        public JsonObject history() {
            return (JsonObject) Codec.JSON_OBJECT.get(this.asJson, "history");
        }

        public CertificateData history(JsonObject v) {
            Codec.JSON_OBJECT.set(this.asJson, "history", v);
            return this;
        }

        public CertificateData historyDo(Consumer<JsonObject> act) {
            JsonObject x = (JsonObject) Optional.ofNullable(this.history()).orElseGet(JsonObject::new);
            act.accept(x);
            this.history(x);
            return this;
        }

        public long user() {
            return (Long) Codec.LONG.get(this.asJson, "user");
        }

        public CertificateData user(long v) {
            Codec.LONG.set(this.asJson, "user", v);
            return this;
        }

        public int kind() {
            return (Integer) Codec.INT.get(this.asJson, "kind");
        }

        public CertificateData kind(int v) {
            Codec.INT.set(this.asJson, "kind", v);
            return this;
        }

        public String identifier() {
            return (String) Codec.STRING.get(this.asJson, "identifier");
        }

        public CertificateData identifier(String v) {
            Codec.STRING.set(this.asJson, "identifier", v);
            return this;
        }

        public JsonObject certificate() {
            return (JsonObject) Codec.JSON_OBJECT.get(this.asJson, "certificate");
        }

        public CertificateData certificate(JsonObject v) {
            Codec.JSON_OBJECT.set(this.asJson, "certificate", v);
            return this;
        }

        public CertificateData certificateDo(Consumer<JsonObject> act) {
            JsonObject x = (JsonObject) Optional.ofNullable(this.certificate()).orElseGet(JsonObject::new);
            act.accept(x);
            this.certificate(x);
            return this;
        }

        public static CertificateData from(Certificate t) {
            CertificateData var10000;
            if (t == null) {
                var10000 = null;
            } else if (t instanceof CertificateData) {
                CertificateData u = (CertificateData) t;
                var10000 = u;
            } else {
                var10000 = new CertificateData(t.asJson());
            }

            return var10000;
        }

        public CertificateData copy() {
            return new CertificateData(this.toJson());
        }

        public final JsonObject toJS() {
            JsonObject js = this.toJson();
            Codec.toJs(js, JsonObject::getLong, new String[]{"id"});
            Codec.toJs(js, JsonObject::getLong, new String[]{"creator"});
            Codec.toJs(js, JsonObject::getLong, new String[]{"modifier"});
            Codec.toJs(js, JsonObject::getLong, new String[]{"user"});
            return js;
        }

        public static JsonObject fromJS(JsonObject js) {
            Codec.fromJs(js, JsonObject::getString, Long::parseLong, new String[]{"id"});
            Codec.fromJs(js, JsonObject::getString, Long::parseLong, new String[]{"creator"});
            Codec.fromJs(js, JsonObject::getString, Long::parseLong, new String[]{"modifier"});
            Codec.fromJs(js, JsonObject::getString, Long::parseLong, new String[]{"user"});
            return js;
        }
    }


    public static final class CertificateStore extends Model.Base<Long, Certificate, CertificateStore>  {
        public static final Function<Model<?>, Field<?>[]> FACTORY = (m) -> new Field[]{
                new Field.LongField("id", "id", m),
                new Field.IntegerField("version", "version", m), new Field.BooleanField("removed", "removed", m),
                new Field.LongField("creator", "creator", m),
                new Field.InstantField("created_at", "createdAt", m),
                new Field.LongField("modifier", "modifier", m),
                new Field.InstantField("modified_at", "modifiedAt", m),
                new Field.JsonObjectField("history", "history", m),
                new Field.LongField("user", "user", m),
                new Field.IntegerField("kind", "kind", m),
                new Field.StringField("identifier", "identifier", m),
                new Field.JsonObjectField("certificate", "certificate", m)
        };
        public static final Class<Certificate> TYPE = Certificate.class;
        public static final CertificateStore MODEL = new CertificateStore();
        public static final Stored.Storage<Long, Certificate, CertificateStore> STORAGE = (q, d, s) -> (CertificateStore) MODEL.copy(s)._with(q, d);

        public CertificateStore(String schema) {
            super(schema, "foundation_users_certificate", CertificateData::new, 0, 1, 2, 3, 4, 5, 6, 7);
        }

        public CertificateStore() {
            this((String) null);
        }

        public Field.LongField id() {
            return (Field.LongField) this.field(0);
        }

        public Field.IntegerField version() {
            return (Field.IntegerField) this.field(1);
        }

        public Field.BooleanField removed() {
            return (Field.BooleanField) this.field(2);
        }

        public Field.LongField creator() {
            return (Field.LongField) this.field(3);
        }

        public Field.InstantField createdAt() {
            return (Field.InstantField) this.field(4);
        }

        public Field.LongField modifier() {
            return (Field.LongField) this.field(5);
        }

        public Field.InstantField modifiedAt() {
            return (Field.InstantField) this.field(6);
        }

        public Field.JsonObjectField history() {
            return (Field.JsonObjectField) this.field(7);
        }

        public Field.LongField user() {
            return (Field.LongField) this.field(8);
        }

        public Field.IntegerField kind() {
            return (Field.IntegerField) this.field(9);
        }

        public Field.StringField identifier() {
            return (Field.StringField) this.field(10);
        }

        public Field.JsonObjectField certificate() {
            return (Field.JsonObjectField) this.field(11);
        }

        protected CertificateStore _self() {
            return this;
        }

        protected Field<?>[] buildFields() {
            return (Field[]) FACTORY.apply(this);
        }

        protected CertificateStore copy(@Nullable String schema) {
            return schema == null ? new CertificateStore(this._schema) : new CertificateStore(schema);
        }
    }


    static Pool sql;
    static Dialect dialect;
    static UserStore store;
    static CertificateStore certificateStore;

    @Test
    @Order(0)
    void put(Vertx vertx, VertxTestContext vtc) {
        store.put(null, JsonObject.of("profile", JsonObject.of("c", JsonObject.of("a", 1))))
                .map(Fn.peek(t -> {
                    assertNotNull(t);
                    log.info("{}", t.asJson().encodePrettily());
                }))
                .onComplete(vtc.succeedingThenComplete());
    }

    @Test
    @Order(1)
    void putMany(Vertx vertx, VertxTestContext vtc) {
        store.putMany(null, List.of(
                        JsonObject.of("profile", JsonObject.of()),
                        JsonObject.of("profile", JsonObject.of()),
                        JsonObject.of("profile", JsonObject.of())
                ))
                .map(Fn.peek(t -> {
                    assertNotNull(t);
                    log.info("{}", t.stream().map(x -> x.asJson().encodePrettily()).collect(Collectors.joining("\n")));
                }))
                .onComplete(vtc.succeedingThenComplete());
    }


    @Test
    @Order(2)
    void putGetID(Vertx vertx, VertxTestContext vtc) {
        store.putGetIdentity(null, JsonObject.of("profile", JsonObject.of()))
                .map(Fn.peek(t -> {
                    assertNotNull(t);
                    log.info("{}", t);
                }))
                .onComplete(vtc.succeedingThenComplete());
    }

    @Test
    @Order(3)
    void putManyGetID(Vertx vertx, VertxTestContext vtc) {
        store.putManyGetIdentity(null, List.of(
                        JsonObject.of("profile", JsonObject.of()),
                        JsonObject.of("profile", JsonObject.of()),
                        JsonObject.of("profile", JsonObject.of("a", 1))
                ))
                .map(Fn.peek(t -> {
                    assertNotNull(t);
                    log.info("{}", t);
                }))
                .onComplete(vtc.succeedingThenComplete());
    }

    @Test
    @Order(4)
    void one(Vertx vertx, VertxTestContext vtc) {
        store.one(d -> d.id().eq(1L))
                .map(Fn.peek(v -> log.info("{}", v.orElseThrow().asJson().encodePrettily())))
                .onComplete(vtc.succeedingThenComplete());
    }


    @Test
    @Order(5)
    void oneProfile(Vertx vertx, VertxTestContext vtc) {
        store.any(d -> d.id().eq(1L), (t, q) -> q.pick(t.profile().objectAt("c")).maybe())
                .map(Fn.peek(v -> log.info("{}", v.orElseThrow().encodePrettily())))
                .onComplete(vtc.succeedingThenComplete());
    }

    @Test
    @Order(6)
    void oneProfileInt(Vertx vertx, VertxTestContext vtc) {
        store.any(d -> d.id().eq(1L), (t, q) -> q.pick(t.profile().objectAt("c").integerAt("a")).maybe())
                .map(Fn.peek(v -> log.info("{}", v.orElseThrow())))
                .onComplete(vtc.succeedingThenComplete());
    }

    @Test
    @Order(7)
    void pickAny(Vertx vertx, VertxTestContext vtc) {
        store.any(d -> d.id().eq(1L), (t, q) -> q.pick(t.id(), t.profile().objectAt("c").integerAt("a")).maybe())
                .map(Fn.peek(v -> log.info("{}", v.orElseThrow())))
                .onComplete(vtc.succeedingThenComplete());
    }

    @Test
    @Order(8)
    void update(Vertx vertx, VertxTestContext vtc) {
        store.set(null, 1L, t -> List.of(t.profile().setAt(2, "c", "a")))
                .map(Fn.peek(v -> log.info("{}", v.asJson().encodePrettily())))
                .onComplete(vtc.succeedingThenComplete());
    }

    @Test
    @Order(9)
    void updateMay(Vertx vertx, VertxTestContext vtc) {
        store.setAny(null, t -> t.profile().objectAt("c").isNull(), t -> List.of(t.profile().setAt(JsonObject.of("x", 1), "c")))
                .map(Fn.peek(v -> log.info("{}", v)))
                .onComplete(vtc.succeedingThenComplete());
    }

    @Test
    @Order(10)
    void any(Vertx vertx, VertxTestContext vtc) {
        store.any(d -> d.id().gte(1L))
                .map(Fn.peek(v -> log.info("{}", v.stream().map(Data::asJson).map(JsonObject::encodePrettily).collect(Collectors.joining("\n")))))
                .onComplete(vtc.succeedingThenComplete());
    }

    @Test
    @Order(12)
    void join(Vertx vertx, VertxTestContext vtc) {
        store.join(certificateStore
                        , (u, c) -> c.user().eq(store.id())
                        , (u, q) -> q
                                .filter(store.id().eq(1L))
                                .maybe())
                .map(Fn.Maybe::orElseNull)
                .map(Fn.peek(v -> log.info("{}", v.map1(x -> x.asJson().encodePrettily()).map2(x -> x.asJson().encodePrettily()))))
                .onComplete(vtc.succeedingThenComplete());
    }
    @Test
    @Order(13)
    void joinWith(Vertx vertx, VertxTestContext vtc) {
        store.joinWith(certificateStore
                        , (u, c) -> c.user().eq(store.id())
                        , (u, q) -> q
                                .filter(store.id().eq(1L))
                                .maybe())
                .map(Fn.Maybe::orElseNull)
                .map(Fn.peek(v -> log.info("{}", v.map1(x -> x.asJson().encodePrettily()).map2(x -> x.asJson().encodePrettily()))))
                .onComplete(vtc.succeedingThenComplete());
    }
    @Test
    @Order(14)
    void joinTo(Vertx vertx, VertxTestContext vtc) {
        store.joinTo(certificateStore
                        , (u, c) -> c.user().eq(store.id())
                        , (u, q) -> q
                                .filter(store.id().eq(1L))
                                .maybe())
                .map(Fn.Maybe::orElseNull)
                .map(Fn.peek(v -> log.info("{}", v.map1(x -> x.asJson().encodePrettily()).map2(x -> x.asJson().encodePrettily()))))
                .onComplete(vtc.succeedingThenComplete());
    }

    @Test
    @Order(113)
    void removeOne(Vertx vertx, VertxTestContext vtc) {
        store.remove(null, 1L, 1)
                .onComplete(vtc.succeedingThenComplete());
    }

    @Test
    @Order(114)
    void removeAny(Vertx vertx, VertxTestContext vtc) {
        store.removeAnyPermanent(null, t -> t.id().nonNull())
                .onComplete(vtc.succeedingThenComplete());
    }


    @Test
    @Order(115)
    void certHistory(Vertx vertx, VertxTestContext vtc) {
        certificateStore.putGetIdentity(null, new CertificateData()
                        .user(1)
                        .kind(2)
                        .identifier("12345")
                        .certificate(JsonObject.of())
                        .asJson())
                .flatMap(id -> certificateStore.set(1L, id, t -> List.of(t.certificate().setAt("some", "secret"))))
                .map(Fn.peek(v -> log.info("{}", v)))
                .onComplete(vtc.succeedingThenComplete());
    }

    @Test
    @Order(116)
    void limit(Vertx vertx, VertxTestContext vtc) {
        certificateStore.any(t -> t.removed().isFalse(), (t, q) -> q.slice(1, 2))
                .map(Fn.peek(v -> log.info("{}", v)))
                .onComplete(vtc.succeedingThenComplete());
    }

    @Test
    @Order(117)
    void count(Vertx vertx, VertxTestContext vtc) {
        certificateStore.any(t -> t.removed().isFalse(), (t, q) -> q.count())
                .map(Fn.peek(v -> log.info("{}", v)))
                .onComplete(vtc.succeedingThenComplete());
    }

    @Test
    @Order(118)
    void count1(Vertx vertx, VertxTestContext vtc) {
        certificateStore.any(t -> t.id().nonNull(), (t, q) -> q.count())
                .map(Fn.peek(v -> log.info("{}", v)))
                .onComplete(vtc.succeedingThenComplete());
    }

    @BeforeAll
    static void setup(Vertx vertx, VertxTestContext vtc) {
        sql = PgBuilder.pool()
                .using(vertx)
                .connectingTo(System.getProperty("PG_CONN"))
                .build();
        dialect = new PgDialect();
        store = UserStore.STORAGE.apply(sql, dialect, null);
        certificateStore = CertificateStore.STORAGE.apply(sql, dialect, null);
        SqlTemplate.forUpdate(sql, "TRUNCATE TABLE %s RESTART IDENTITY".formatted(store._name))
                .execute(Map.of())
                .onComplete(vtc.succeedingThenComplete());

    }
}

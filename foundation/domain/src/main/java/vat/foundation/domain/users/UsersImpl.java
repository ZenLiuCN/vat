package vat.foundation.domain.users;

import com.google.auto.service.AutoService;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.SqlConnection;
import lombok.With;
import org.jooq.lambda.tuple.Tuple4;
import vat.api.Activities;
import vat.api.DomainError;
import vat.api.implement.DomainManager;
import vat.api.meta.Activity;
import vat.api.store.Dialect;
import vat.api.utils.Fn;
import vat.api.utils.JSON;
import vat.foundation.users.api.CertificateData;
import vat.foundation.users.api.UserData;
import vat.foundation.users.api.UsersDomain;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

///
/// @author Zen.Liu
/// @since 2025-11-10
@AutoService(Activities.class)
@Activity(mode = Activity.Mode.FOUNDATION, auto = true)
public class UsersImpl extends UsersDomain<UsersImpl> {
    public UsersImpl() {
        super();
        debug = false;
    }

    private final boolean debug;

    public UsersImpl(Vertx vertx, String address, Pool sql, Dialect dialect, JsonObject conf) {
        super(vertx, address, sql, dialect, conf);
        debug = debug();
        log.info("Found providers: {}", CertificateProvider.PROVIDERS);
    }

    @Override
    protected UsersImpl _this() {
        return this;
    }

    @Override
    protected Future<User> doCertificate(Cert cert) {
        return certCheck(null, cert.kind(), cert.identifier(), cert.secret())
                .flatMap(users(null)::maybe)
                .map(Fn.Maybe.orElseThrow(this::invalidCertificate));
    }

    @Override
    protected Future<User> doUpdateProfile(ProfileUpdate req) {
        return users(null).set(req.actor().orElse(-1L), req.id(), req.version(), t ->
                List.of(req.path()
                        .map(j -> JSON.jsonObjectPathWrite(j, req.data(), t.profile()))
                        .orElseGet(() -> t.profile().set(req.data()))));
    }

    @Override
    protected Future<JsonObject> doReadProfile(ProfileRead req) {
        return users(null).any(
                t -> t.id().eq(req.id()),
                (t, q) -> q.pick(req.path()
                        .map(JSON.Functor.jsonObjectPathRead(t.profile()))
                        .orElseGet(t::profile)).one()
        );
    }

    @Override
    public Future<Optional<JsonObject>> certProfileFetch(CertProfileAccess cert) {
        return Future.succeededFuture()
                .map(Optional.ofNullable(CertificateProvider.PROVIDERS.get(cert.kind())))
                .map(Fn.Maybe.orElseThrow(this::invalidCertificate))
                .flatMap(c -> certs(null)
                        .any(
                                t -> t.kind().eq(cert.kind()).and(t.identifier().eq(cert.identifier()))
                                , (t, q) -> q
                                        .pick(JSON.jsonObjectPathRead(cert.path(), t.profile()))
                                        .maybe()));
    }

    @Override
    public Future<Boolean> certProfileUpdate(CertProfileAccess cert) {
        return Future.succeededFuture()
                .map(Optional.ofNullable(CertificateProvider.PROVIDERS.get(cert.kind())))
                .map(Fn.Maybe.orElseThrow(this::invalidCertificate))
                .flatMap(c -> certs(null)
                        .any(
                                t -> t.kind().eq(cert.kind()).and(t.identifier().eq(cert.identifier()))
                                , (t, q) -> q.pick(t.id(), t.version()).maybe()))
                .flatMap(Fn.Maybe.Flat.isPresent(u -> certs(null)
                        .justSet(-1L, u.v1, u.v2, t -> List.of(JSON.jsonObjectPathWrite(
                                cert.path(),
                                cert.data()
                                        .orElseThrow(() -> DomainError.System.badRequest("required update data")),
                                t.profile())))
                        .map($ -> Optional.of(true))
                ))
                .map(Optional::isPresent)
                ;
    }

    @Override
    protected Future<User> doRegister(Cert cert) {
        return Future.succeededFuture()
                .map(Optional.ofNullable(CertificateProvider.PROVIDERS.get(cert.kind())))
                .map(Fn.Maybe.orElseThrow(this::unsupportedKind))
                .flatMap(ow -> sql.withConnection(tx -> certs(tx)
                        .exists(t -> t.kind().eq(cert.kind())
                                .and(t.identifier().eq(cert.identifier())))
                        .map(Fn.isTrue(this::alreadyRegistered))
                        .flatMap($_ -> users(tx).putGetIdentity(null, new UserData()
                                .profile(JsonObject.of())
                                .toJson()))
                        .flatMap(id -> ow.store(vertx, this, cert.identifier(), cert.secret())
                                .flatMap(st -> certs(tx)
                                        .justPut(null, new CertificateData()
                                                .user(id)
                                                .kind(cert.kind())
                                                .identifier(cert.identifier())
                                                .certificate(st)
                                                .profile(JsonObject.of())
                                                .asJson()))
                                .flatMap($_ -> users(tx).maybe(id))
                                .map(Fn.Maybe.orElseThrow(this::alreadyRegistered))
                        ))
                )
                .onSuccess(u -> changedPublish(d -> d.ofKindCreated().user(u.id()).cert(-1)));
    }

    @Override
    public Future<Optional<User>> check(Cert cert) {
        var id = cert.identifier();
        var sec = cert.secret();
        return certGet(null, cert.kind(), cert.identifier(), cert.secret())
                .flatMap(Fn.Maybe.Flat.isPresent(c -> c.test(vertx, this, id, sec)
                        .map(Fn.trueValue(this::invalidCertificate))
                        .map(c.user)
                        .flatMap(users(null)::maybe)));

    }

    private Future<Long> certCheck(SqlConnection tx, int kind, String identifier, JsonObject secret) {
        return certGet(tx, kind, identifier, secret)
                .map(Fn.Maybe.orElseThrow(this::invalidCertificate))
                .flatMap(ce -> ce.test(vertx, this, identifier, secret)
                        .map(Fn.trueValue(this::invalidCertificate))
                        .map(ce.user)
                );
    }

    record CertEntry(long id, int version, long user, JsonObject cert, @With CertificateProvider provider) {
        static final Function<Optional<Tuple4<Long, Integer, Long, JsonObject>>, Optional<CertEntry>> OF = v -> v.map(CertEntry::new);

        CertEntry(Tuple4<Long, Integer, Long, JsonObject> u) {
            this(u.v1, u.v2, u.v3, u.v4, null);
        }

        Future<Boolean> test(Vertx vertx, DomainManager m, String identifier, JsonObject secret) {
            return provider.test(vertx, m, identifier, secret, cert);
        }
    }

    private Future<Optional<CertEntry>> certGet(SqlConnection tx, int kind, String identifier, JsonObject secret) {
        return Future.succeededFuture()
                .map(Optional.ofNullable(CertificateProvider.PROVIDERS.get(kind)))
                .map(Fn.Maybe.orElseThrow(this::unsupportedKind))
                .flatMap(c -> certs(tx)
                        .any(
                                t -> t.kind().eq(kind).and(t.identifier().eq(identifier))
                                , (t, q) -> q.pick(t.id(), t.version(), t.user(), t.certificate()).maybe())
                        .map(CertEntry.OF)
                        .map(s -> s.map(e -> e.withProvider(c)))
                );
    }

}

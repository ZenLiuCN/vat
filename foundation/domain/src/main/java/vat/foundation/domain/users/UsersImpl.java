package vat.foundation.domain.users;

import com.google.auto.service.AutoService;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.SqlConnection;
import lombok.With;
import org.jooq.lambda.tuple.Tuple4;
import org.jspecify.annotations.Nullable;
import vat.api.Activities;
import vat.api.DomainError;
import vat.api.implement.DomainManager;
import vat.api.meta.Activity;
import vat.api.store.Dialect;
import vat.api.utils.Fn;
import vat.api.utils.JSON;
import vat.api.utils.Monadic;
import vat.foundation.users.api.CertificateData;
import vat.foundation.users.api.UserData;
import vat.foundation.users.api.UsersDomain;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

///
/// @author Zen.Liu
/// @since 2025-11-10
@SuppressWarnings("unused")
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

    static final Monadic<UsersImpl, Cert, User> CERTIFICATE = Monadic.<UsersImpl, Cert>identity()
            .flatMap((c, i) -> c.certCheck(null, i.kind(), i.identifier(), i.secret()))
            .flatMap((c, i) -> c.users().maybe(i))
            .map((c, i) -> i.orElseThrow(c::invalidCertificate))
            .finalization();

    @Override
    protected Future<User> doCertificate(Cert cert) {
        return CERTIFICATE.process(this, cert);
      /*  return certCheck(null, cert.kind(), cert.identifier(), cert.secret())
                .flatMap(users(null)::maybe)
                .map(Fn.Maybe.orElseThrow(this::invalidCertificate));*/
    }

    static final Monadic<UsersImpl, ProfileUpdate, User> PROFILE_UPDATE_MONADIC = Monadic.<UsersImpl, ProfileUpdate>identity()
            .flatMap((c, i) -> c.users().set(i.actor().orElse(-1L), Fn.safe(i.id()), i.version(), t ->
                    List.of(i.path()
                            .map(j -> JSON.jsonObjectPathWrite(j, i.data(), t.profile()))
                            .orElseGet(() -> t.profile().set(i.data())))
            ));

    @Override
    protected Future<User> doUpdateProfile(ProfileUpdate req) {
        return PROFILE_UPDATE_MONADIC.process(this, req);
       /* return users(null).set(req.actor().orElse(-1L), Fn.safe(req.id()), req.version(), t ->
                List.of(req.path()
                        .map(j -> JSON.jsonObjectPathWrite(j, req.data(), t.profile()))
                        .orElseGet(() -> t.profile().set(req.data()))));*/
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

    static Monadic<UsersImpl, Cert, User> REGISTER = Monadic.<UsersImpl, Cert>identity()
            .flatMap((ctx, cert) -> {
                var ow = Optional.ofNullable(CertificateProvider.PROVIDERS.get(cert.kind())).orElseThrow(ctx::unsupportedKind);
                return ctx.sql.withConnection(tx -> ctx.certs(tx)
                                .exists(t -> t.kind().eq(cert.kind())
                                        .and(t.identifier().eq(cert.identifier())))
                                .map(Fn.isTrue(ctx::alreadyRegistered))
                                .flatMap($_ -> ctx.users(tx).putGetIdentity(null, new UserData()
                                        .profile(JsonObject.of())
                                        .toJson()))
                                .flatMap(id ->
                                        Fn.safe(ow).store(ctx.vertx, ctx, cert.identifier(), cert.secret())
                                                .flatMap(st -> ctx.certs(tx)
                                                        .justPut(null, new CertificateData()
                                                                .user(id)
                                                                .kind(cert.kind())
                                                                .identifier(cert.identifier())
                                                                .certificate(st)
                                                                .profile(JsonObject.of())
                                                                .asJson()))
                                                .flatMap($_ -> ctx.users(tx).maybe(id))
                                                .map(Fn.Maybe.orElseThrow(ctx::alreadyRegistered))
                                ))
                        .onSuccess(u -> ctx.changedPublish(d -> d.ofKindCreated().user(Fn.safe(u.id())).cert(-1)));
            });

    @Override
    protected Future<User> doRegister(Cert cert) {
        return REGISTER.process(this, cert);
/*        return Future.succeededFuture(Optional.ofNullable(CertificateProvider.PROVIDERS.get(cert.kind())))
                .map(Fn.Maybe.orElseThrow(this::unsupportedKind))
                .flatMap(ow -> sql.withConnection(tx -> certs(tx)
                        .exists(t -> t.kind().eq(cert.kind())
                                .and(t.identifier().eq(cert.identifier())))
                        .map(Fn.isTrue(this::alreadyRegistered))
                        .flatMap($_ -> users(tx).putGetIdentity(null, new UserData()
                                .profile(JsonObject.of())
                                .toJson()))
                        .flatMap(id ->
                                Fn.safe(ow).store(vertx, this, cert.identifier(), cert.secret())
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
                .onSuccess(u -> changedPublish(d -> d.ofKindCreated().user(Fn.safe(u.id())).cert(-1)));*/
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

    @SuppressWarnings("SameParameterValue")
    private Future<Long> certCheck(@Nullable SqlConnection tx, int kind, String identifier, JsonObject secret) {
        return certGet(tx, kind, identifier, secret)
                .map(Fn.Maybe.orElseThrow(this::invalidCertificate))
                .flatMap(ce -> ce.test(vertx, this, identifier, secret)
                        .map(Fn.trueValue(this::invalidCertificate))
                        .map(ce.user)
                );
    }

    private Future<Long> certCheck(int kind, String identifier, JsonObject secret) {
        return certCheck(null, kind, identifier, secret);
    }

    record CertEntry(long id, int version, long user, JsonObject cert, @Nullable @With CertificateProvider provider) {
        static final Function<Optional<Tuple4<Long, Integer, Long, JsonObject>>, Optional<CertEntry>> OF = v -> v.map(CertEntry::new);

        CertEntry(Tuple4<Long, Integer, Long, JsonObject> u) {
            this(u.v1, u.v2, u.v3, u.v4, null);
        }

        Future<Boolean> test(Vertx vertx, DomainManager m, String identifier, JsonObject secret) {
            return provider == null ? Future.succeededFuture(false) : provider.test(vertx, m, identifier, secret, cert);
        }
    }

    record Request(@Nullable SqlConnection tx, int kind, String identifier, JsonObject secret) {
    }

    static final Monadic<UsersImpl, Request, Optional<CertEntry>> GET_CERT = Monadic.<UsersImpl, Request>identity()
            .flatMap((ctx, r) -> {
                var c = Optional.ofNullable(CertificateProvider.PROVIDERS.get(r.kind)).orElseThrow(ctx::unsupportedKind);
                return ctx.certs(r.tx)
                        .any(
                                t -> t.kind().eq(r.kind).and(t.identifier().eq(r.identifier))
                                , (t, q) -> q.pick(t.id(), t.version(), t.user(), t.certificate()).maybe())
                        .map(CertEntry.OF)
                        .map(s -> s.map(e -> e.withProvider(c)));
            });

    private Future<Optional<CertEntry>> certGet(@Nullable SqlConnection tx, int kind, String identifier, JsonObject secret) {
        return GET_CERT.process(this, new Request(tx, kind, identifier, secret));
     /*   return Future.succeededFuture()
                .map(Optional.ofNullable(CertificateProvider.PROVIDERS.get(kind)))
                .map(Fn.Maybe.orElseThrow(this::unsupportedKind))
                .flatMap(c -> certs(tx)
                        .any(
                                t -> t.kind().eq(kind).and(t.identifier().eq(identifier))
                                , (t, q) -> q.pick(t.id(), t.version(), t.user(), t.certificate()).maybe())
                        .map(CertEntry.OF)
                        .map(s -> s.map(e -> e.withProvider(c)))
                );*/
    }

}

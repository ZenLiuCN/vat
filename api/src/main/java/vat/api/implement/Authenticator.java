package vat.api.implement;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.authentication.Credentials;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.SimpleAuthenticationHandler;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vat.api.DomainError;
import vat.api.meta.Nullable;
import vat.api.utils.Fn;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Arrays;
import java.util.Base64;
import java.util.BitSet;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

///
/// @author Zen.Liu
/// @since 2025-11-10


public interface Authenticator {
    Logger log = LoggerFactory.getLogger(Authenticator.class);
    String USER_ID = "_AUTH_USER_ID";
    String USER_CERT_KIND = "_AUTH_CERT_KIND";
    String ANONYMOUS_USER_KEY = "anonymous";

    static boolean isAnonymous(User user) {
        return user.containsKey(ANONYMOUS_USER_KEY);
    }

    static Optional<Long> userIdentity(User user) {
        return Optional.ofNullable(user.attributes().getLong(USER_ID));
    }

    static Optional<Integer> userCertificateKind(User user) {
        return Optional.ofNullable(user.attributes().getInteger(USER_CERT_KIND));
    }

    /// unique authenticator kind, should never be -1.
    /// + 0: built-in JWT model.
    int kind();
    default User injection(RoutingContext c,User u){
        u.attributes().put(USER_CERT_KIND,kind());
        return Authenticator.inject(c,u);
    }
    /// A customer authenticator
    interface Customer extends Authenticator {

        /// Initialize this Authenticator, this will invoke before activities been created.
        ///
        /// @param config  configuration at `/DOMAIN/authenticator`
        /// @param locator configure  Token reader
        Customer initialize(Vertx vertx, TokenReader locator, @Nullable JsonObject config);

        /// Optional authenticator may allow user to access as anonymous user.
        Future<Optional<User>> optional(RoutingContext ctx);

        Future<User> required(RoutingContext ctx);


        @Override
        default Authenticator copy() {
            return this;
        }

        @Override
        default SimpleAuthenticationHandler authenticate() {
            return SimpleAuthenticationHandler.create()
                    .authenticate(rc -> required(rc)
                            .map(Fn.peek(u -> u.attributes().put(USER_CERT_KIND, kind()))));
        }

        @Override
        default SimpleAuthenticationHandler authenticateMaybe() {
            return SimpleAuthenticationHandler.create()
                    .authenticate(rc -> optional(rc)
                            .map(Fn.Maybe.orElse(() -> User
                                    .create(JsonObject.of(ANONYMOUS_USER_KEY, System.currentTimeMillis()))
                            ))
                            .map(Fn.peek(u -> u.attributes().put(USER_CERT_KIND, kind()))));
        }

    }


    interface TokenReader extends Function<RoutingContext, String> {

        default TokenReader orElse(TokenReader reader) {
            return ctx -> {
                var v = apply(ctx);
                if (v == null) v = reader.apply(ctx);
                return v;
            };
        }


    }

    interface TokenProvider {
        String generate(JsonObject claim);

        String generate(JsonObject claim, int expireInSeconds);
    }

    Authenticator copy();

    /// extract token from RC
    Future<Optional<String>> token(RoutingContext ctx);

    Function<String, TokenReader> HEADER = name ->
            ctx -> ctx.request().getHeader(name);
    Function<String, TokenReader> QUERY = name ->
            ctx -> ctx.queryParams().get(name);
    Function<String, TokenReader> PATH = name ->
            ctx -> ctx.pathParam(name);

    /// create required authentication handler
    SimpleAuthenticationHandler authenticate();

    /// create optional authentication handler
    SimpleAuthenticationHandler authenticateMaybe();


    int INJECT_REMOTE_ADDRESS = 0;
    String REMOTE_ADDRESS = "_RemoteAddress";

    default boolean anonymous(User user) {
        return isAnonymous(user);
    }

    default Optional<String> remoteRealAddress(User user) {
        return Optional.ofNullable(user.principal().getString(REMOTE_ADDRESS));
    }

    default Optional<Integer> certKind(User user) {
        return userCertificateKind(user);
    }

    default Optional<Long> userId(User user) {
        return Authenticator.userIdentity(user);
    }

    AtomicReference<BitSet> INJECT_REQUEST_INFO = new AtomicReference<>(Fn.apply(new BitSet(), s -> s.set(INJECT_REMOTE_ADDRESS)));


    static User inject(RoutingContext ctx, User user) {
        var conf = INJECT_REQUEST_INFO.get();
        if (conf.get(INJECT_REMOTE_ADDRESS))
            user.principal().put(REMOTE_ADDRESS, Web.Context.remoteRealAddress(ctx));
        return user;
    }

    record Validator(int kind, TokenReader reader, AuthenticationProvider provider) implements Authenticator {

        @Override
        public Authenticator copy() {
            return new Validator(kind, reader, provider);
        }

        @Override
        public Future<Optional<String>> token(RoutingContext ctx) {
            return Future.succeededFuture(Optional.ofNullable(reader.apply(ctx)));
        }


        public SimpleAuthenticationHandler authenticateMaybe() {
            return SimpleAuthenticationHandler.create().authenticate(ctx ->
                    token(ctx)
                            .map(x -> x.orElse(""))
                            .flatMap(tk -> tk.isBlank() ? Future.succeededFuture() : provider().authenticate(new TokenCredentials(tk)))
                            .map(u -> {
                                if (u == null)
                                    return injection(ctx, User.create(JsonObject.of(ANONYMOUS_USER_KEY, System.currentTimeMillis())));
                                if (!u.expired())
                                    return injection(ctx, u);
                                throw DomainError.System.unauthorized("expired");
                            })
                            .recover(ex -> {
                                if (log.isDebugEnabled())
                                    log.error("authentication failed: {}", Web.dump(ctx), ex);
                                else {
                                    log.error("authentication failed: {}", ctx.request().absoluteURI(), ex);
                                }
                                ctx.fail(401, ex);
                                return Future.succeededFuture();
                            }));
        }

        public SimpleAuthenticationHandler authenticate() {
            return SimpleAuthenticationHandler.create()
                    .authenticate(ctx -> token(ctx)
                            .map(x -> x.orElse(""))
                            .flatMap(tk -> {
                                if (tk.isBlank())
                                    return Future.failedFuture(DomainError.System.unauthorized("token required"));
                                return provider().authenticate(new TokenCredentials(tk));
                            })
                            .map(u -> {
                                if (!u.expired()) return injection(ctx, u);
                                throw DomainError.System.unauthorized("expired");
                            })
                            .recover(ex -> {
                                if (log.isDebugEnabled())
                                    log.error("authentication failed: {}", Web.dump(ctx), ex);
                                else {
                                    log.error("authentication failed: {}", ctx.request().absoluteURI(), ex);
                                }
                                ctx.fail(401, ex);
                                return Future.succeededFuture();
                            }));
        }
    }

    record Generator<T extends AuthenticationProvider & TokenProvider>(
            int kind,
            TokenReader reader, T provider
    ) implements Authenticator, TokenProvider {
        @Override
        public Authenticator copy() {
            return new Generator<>(kind, reader, provider);
        }

        @Override
        public Future<Optional<String>> token(RoutingContext ctx) {
            return Future.succeededFuture(Optional.ofNullable(reader.apply(ctx)));
        }

        public SimpleAuthenticationHandler authenticateMaybe() {
            return SimpleAuthenticationHandler.create().authenticate(ctx ->
                    token(ctx)
                            .map(x -> x.orElse(""))
                            .flatMap(tk -> tk.isBlank() ? Future.succeededFuture() : provider().authenticate(new TokenCredentials(tk)))
                            .map(u -> {
                                if (u == null) {
                                    return injection(ctx,User.create(JsonObject.of(ANONYMOUS_USER_KEY, System.currentTimeMillis())));
                                }
                                if (!u.expired()) {
                                    return Authenticator.inject(ctx,u);
                                }
                                throw DomainError.System.unauthorized("expired");
                            })
                            .recover(ex -> {
                                if (log.isDebugEnabled())
                                    log.error("authentication failed: {}", Web.dump(ctx), ex);
                                else {
                                    log.error("authentication failed: {}", ctx.request().absoluteURI(), ex);
                                }
                                ctx.fail(401, ex);
                                return Future.succeededFuture();
                            }));
        }

        public SimpleAuthenticationHandler authenticate() {
            return SimpleAuthenticationHandler.create().authenticate(ctx -> token(ctx)
                    .map(x -> x.orElse(""))
                    .flatMap(tk -> {
                        if (tk.isBlank())
                            return Future.failedFuture(DomainError.System.unauthorized("token required"));
                        return provider().authenticate(new TokenCredentials(tk));
                    })
                    .map(u -> {
                        if (!u.expired()) return injection(ctx,u);
                        throw DomainError.System.unauthorized("expired");
                    })
                    .recover(ex -> {
                        if (log.isDebugEnabled())
                            log.error("authentication failed: {}", Web.dump(ctx), ex);
                        else {
                            log.error("authentication failed: {}", ctx.request().absoluteURI(), ex);
                        }
                        ctx.fail(401, ex);
                        return Future.succeededFuture();
                    }));
        }


        @Override
        public String generate(JsonObject claim) {
            return provider.generate(claim);
        }

        @Override
        public String generate(JsonObject claim, int expires) {
            return provider.generate(claim, expires);
        }
    }

    record OpenIDJwt(
            Vertx vertx,
            Cipher cipher,
            Key key,
            JWTAuth raw,
            JWTAuthOptions options
    ) implements JWTAuth, TokenProvider {
        public OpenIDJwt(Vertx vertx, JWTAuth raw, byte[] key, JWTAuthOptions options) {
            this(
                    vertx,
                    aes(),
                    new SecretKeySpec(key.length < 32
                            ? Arrays.copyOfRange(key, 0, 16)
                            : Arrays.copyOfRange(key, 0, 32), "AES"),
                    raw,
                    options
            );
        }

        @SneakyThrows
        static Cipher aes() {
            return Cipher.getInstance("AES/ECB/NoPadding");
        }

        public static final String CLAIMS = "Claims";

        public static JsonObject readClaims(User user) {
            return user.attributes().getJsonObject(user.attributes().getString("rootClaim")).getJsonObject(CLAIMS);
        }

        public static void writeClaims(User user, JsonObject claims) {
            user.principal().mergeIn(claims);
            user.attributes().getJsonObject(user.attributes().getString("rootClaim")).put(CLAIMS, claims);
        }


        @SneakyThrows
        private String enc(JsonObject claims) {
            var bytes = claims.toBuffer().getBytes();
            { //PKCS7
                var count = bytes.length;
                var amountToPad = 32 - (count % 32);
                var pad = (byte) (amountToPad & 0xFF);
                var padding = new byte[count + amountToPad];
                System.arraycopy(bytes, 0, padding, 0, count);
                Arrays.fill(padding, count, count + amountToPad, pad);
                bytes = padding;
            }
            synchronized (cipher) {
                cipher.init(Cipher.ENCRYPT_MODE, key);
                return Base64.getUrlEncoder().withoutPadding().encodeToString(cipher.doFinal(bytes));
            }
        }

        @SneakyThrows
        private JsonObject dec(String oid) {
            var bytes = Base64.getUrlDecoder().decode(oid);
            synchronized (cipher) {
                cipher.init(Cipher.DECRYPT_MODE, key);
                bytes = cipher.doFinal(bytes);
            }
            { //PKCS7
                int pad = bytes[bytes.length - 1];
                if (pad >= 1 && pad <= 32) {
                    bytes = Arrays.copyOf(bytes, bytes.length - pad);
                }
            }
            return new JsonObject(Buffer.buffer(bytes));
        }

        @Override
        public String generateToken(JsonObject claims, JWTOptions options) {
            return raw.generateToken(JsonObject.of("oid", enc(claims)), options);
        }

        @Override
        public String generateToken(JsonObject claims) {
            return raw.generateToken(JsonObject.of("oid", enc(claims)), options.getJWTOptions());
        }

        @Override
        public String generate(JsonObject claim) {
            return generateToken(claim);
        }

        @Override
        public String generate(JsonObject claim, int expireInSeconds) {
            return generateToken(claim, expireInSeconds > 0 ?
                    new JWTOptions(options.getJWTOptions()).setExpiresInSeconds(expireInSeconds)
                    : new JWTOptions(options.getJWTOptions()).setIgnoreExpiration(true)
            );
        }

        private Future<User> decode(User u) {
            return vertx.executeBlocking(() -> {
                var attr = u.attributes();
                var root = attr.getString("rootClaim");
                var access = attr.getJsonObject(root);
                var claims = dec(access.getString("oid"));
                u.principal().mergeIn(claims);
                u.attributes().put(root, access.put(CLAIMS, claims));
                return u;
            });
        }


        @Override
        public Future<User> authenticate(Credentials credentials) {
            return raw.authenticate(credentials)
                    .flatMap(this::decode);
        }
    }
}

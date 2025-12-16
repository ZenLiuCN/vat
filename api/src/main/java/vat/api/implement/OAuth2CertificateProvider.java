package vat.api.implement;

import com.github.benmanes.caffeine.cache.Cache;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2AuthorizationURL;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.Oauth2Credentials;
import io.vertx.ext.web.RoutingContext;
import lombok.With;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import vat.api.Data;
import vat.api.DomainError;
import vat.api.meta.Nullable;
import vat.api.utils.Fn;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

///
/// @author Zen.Liu
/// @since 2025-12-15


public interface OAuth2CertificateProvider {

    UnaryOperator<String> REQUIRED = s -> {
        if (s == null || s.isBlank()) throw DomainError.System.badRequest("missing required argument");
        return s;
    };

    record OAuthPhaseOne(String url, String state) implements Data {
        public OAuthPhaseOne(JsonObject j) {
            this(REQUIRED.apply(j.getString("url")), REQUIRED.apply(j.getString("state")));
        }

        public OAuthPhaseOne(JsonObject j, Void ignore) {
            this(j);
        }

        @Override
        public JsonObject toJson() {
            return asJson();
        }

        @Override
        public JsonObject toJS() {
            return asJson();
        }

        @Override
        public JsonObject asJson() {
            return JsonObject.of(
                    "url", url,
                    "state", state
            );
        }
    }

    record OAuthPhaseTwo(String state, String code) implements Data {
        public OAuthPhaseTwo(JsonObject j) {
            this(REQUIRED.apply(j.getString("state")), REQUIRED.apply(j.getString("code")));
        }

        public OAuthPhaseTwo(JsonObject j, Void ignore) {
            this(j);
        }

        @Override
        public JsonObject toJson() {
            return asJson();
        }

        @Override
        public JsonObject toJS() {
            return asJson();
        }

        @Override
        public JsonObject asJson() {
            return JsonObject.of(
                    "code", code,
                    "state", state
            );
        }
    }

    record UserInfo(
            long userId,
            User user,
            String token,
            String redirect
    ) implements Data {
        public UserInfo(JsonObject v) {
            this(
                    v.getLong("userId"),
                    User.create(v.getJsonObject("principal"), v.getJsonObject("attributes")),
                    v.getString("token"),
                    v.getString("redirect")
            );
        }

        public UserInfo(JsonObject v, Void ignore) {
            this(
                    Fn.parseNullable(v.getString("userId"), Long::parseLong),
                    User.create(v.getJsonObject("principal"), v.getJsonObject("attributes")),
                    v.getString("token"),
                    v.getString("redirect")
            );
        }

        public User getUser() {
            user.attributes().put(Authenticator.USER_ID, userId);
            return user;
        }

        @Override
        public JsonObject toJS() {
            return JsonObject.of(
                    "userId", String.valueOf(userId),
                    "attributes", user.attributes(),
                    "principal", user.principal(),
                    "token", token,
                    "redirect", redirect
            );
        }

        @Override
        public JsonObject toJson() {
            return asJson();
        }

        @Override
        public JsonObject asJson() {
            return JsonObject.of(
                    "userId", userId,
                    "attributes", user.attributes(),
                    "principal", user.principal(),
                    "token", token,
                    "redirect", redirect
            );
        }
    }

    record UserCallback(@With User user, String callback, String state) {
    }

    Predicate<User> DEFUALT_REFRESH_TOKEN_PREDICATE = user -> user.principal().getString("refresh_token") != null;
    Function<User, String> DEFAULT_ACCESS_TOKEN_READER = user -> user.principal().getString("access_token");
    Function<User, String> DEFAULT_SUBJECTOR = User::subject;

    record OAuthProvider(
            Logger log,
            /// OAuth auth
            OAuth2Auth auth,
            /// full callback uri
            String callback,
            /// authenticate scopes
            List<String> scopes,
            /// offline mode support refresh token
            boolean offline,
            /// configurator for extra parameters.
            UnaryOperator<OAuth2AuthorizationURL> urlConfigurator,
            /// configurator for Phase two request.
            UnaryOperator<Oauth2Credentials> credentialsConfigurator,
            UnaryOperator<String> blackIdentity,
            Function<User, String> accessTokenReader,
            Function<User, String> subjector,
            Predicate<User> hasProfilePredicate,
            Predicate<User> refreshTokenPredicate,
            ///Caches
            Cache<@NotNull String, String> authority,
            Cache<@NotNull String, User> users,
            Cache<@NotNull String, Boolean> blackSheep
    ) {
        public OAuthProvider(
                Logger log,
                /// OAuth auth
                OAuth2Auth auth,
                /// full callback uri
                String callback,
                /// authenticate scopes
                List<String> scopes,
                /// offline mode support refresh token
                boolean offline,
                /// configurator for extra parameters.
                UnaryOperator<OAuth2AuthorizationURL> urlConfigurator,
                /// configurator for Phase two request.
                UnaryOperator<Oauth2Credentials> credentialsConfigurator,
                UnaryOperator<String> blackIdentity,
                Function<User, String> accessTokenReader,
                Predicate<User> hasProfilePredicate,
                Predicate<User> refreshTokenPredicate,
                ///Caches
                Cache<@NotNull String, String> authority,
                Cache<@NotNull String, User> users,
                Cache<@NotNull String, Boolean> blackSheep
        ) {
            this(
                    log, auth, callback, scopes, offline, urlConfigurator, credentialsConfigurator, blackIdentity, accessTokenReader, DEFAULT_SUBJECTOR, hasProfilePredicate, refreshTokenPredicate,
                    authority, users, blackSheep
            );
        }

        public Future<String> authorizePhaseOne(String state, String callback) {
            return Future.<String>future(p -> {
                        if (authority.getIfPresent(state) != null)
                            throw DomainError.System.badRequest("duplicate state {}", state);
                        authority.put(state, callback);
                        p.complete(auth.authorizeURL(Fn.log(urlConfigurator.apply(new OAuth2AuthorizationURL()
                                .setRedirectUri(this.callback)
                                .setScopes(scopes)
                                .setState(state)), log.isDebugEnabled(), log::info, "authorize info {}")));
                    })
                    .onFailure(ex -> {
                        if (log.isDebugEnabled()) log.error("authorize url fail: {}", this, ex);
                    });
        }

        public Future<UserCallback> authorizePhaseTwo(String code, String state) {
            var cb = authority.getIfPresent(state);
            if (cb == null) return Future.failedFuture(DomainError.System.badRequest("missing required state"));
            return auth.authenticate(Fn.log(credentialsConfigurator.apply(new Oauth2Credentials()
                                    .setFlow(OAuth2FlowType.AUTH_CODE)
                                    .setCode(code)
                                    .setRedirectUri(this.callback)),
                            log.isDebugEnabled(), log::info, "authenticate info {}"))
                    .map(s -> new UserCallback(s, cb, state))
                    .onSuccess($ -> authority.invalidate(state))
                    .onFailure(ex -> {
                        if (log.isDebugEnabled()) log.error("authorize authenticate fail: {}", this, ex);
                    })
                    ;
        }

        public Future<User> userInfo(User u) {
            return hasProfilePredicate.test(u)
                    ? Future.succeededFuture(u)
                    : auth.userInfo(u)
                    .map(u)
                    .onSuccess(this::usersUpdate)
                    .onFailure(ex -> {
                        if (log.isDebugEnabled()) log.error("authorize user info fail: {}", this, ex);
                    });
        }

        public Future<User> authenticate(String token) {
            var bi = blackIdentity.apply(token);
            if (blackSheep.getIfPresent(bi) != null)
                return Future.failedFuture(DomainError.System.unauthorized("token expired"));
            return auth.authenticate(Fn.log(new TokenCredentials().setToken(token), log.isDebugEnabled(), log::info, "authorization info {}"))
                    .onSuccess(this::usersUpdate)
                    .onFailure(ex -> {
                        if (log.isDebugEnabled()) log.error("authorize authorization fail: {}", this, ex);
                    });
        }

        private void usersUpdate(User u) {
            users.put(subjector.apply(u), u);
        }

        private Handler<Object> usersInvalidate(User u) {
            return $ -> users.invalidate(subjector.apply(u));
        }

        public Future<User> refresh(User user) {
            if (!user.expired()) {
                var tk = accessTokenReader.apply(user);
                var bi = blackIdentity.apply(tk);
                if (bi != null) blackSheep.put(bi, true);
            }
            return refreshTokenPredicate.test(user)
                    ? auth
                    .refresh(user)
                    .onSuccess(this::usersUpdate)
                    .map(vat.api.implement.Authenticator::setRefreshed)
                    : Future.failedFuture(DomainError.System.unauthorized("refresh not supported"));
        }

        public Future<Void> logout(User u) {
            var tk = accessTokenReader.apply(u);
            var bi = blackIdentity.apply(tk);
            if (bi != null) blackSheep.put(bi, true);
            return (offline ? doLogout(u)
                    : Future.<Void>succeededFuture())
                    .onSuccess(usersInvalidate(u));
        }

        private Future<Void> doLogout(User u) {
            if (!refreshTokenPredicate.test(u)) {
                if (log.isDebugEnabled()) log.warn("missing refresh token for user: {}", u);
                return Future.succeededFuture();
            }
            return auth.revoke(u, "refresh_token");
        }
    }

    interface Authenticate {
        /// Check if a token is an authorized User.When not an authorized user, {@link vat.api.DomainError} of 401 should throw.
        Future<UserInfo> authorize(String token);

        /// Logout phase
        Future<Void> logout(String token);

        Future<String> phaseOne(OAuthPhaseOne data);

        Future<UserInfo> phaseTwo(OAuthPhaseTwo data);
    }

    interface Authenticator extends vat.api.implement.Authenticator.Customer {
        /// Initialize method with which should finish the configuration of authenticator.
        @Override
        Authenticator initialize(Vertx vertx, TokenReader locator, @Nullable JsonObject config);

        /// Extract and authenticate user. When authorization is missing an Anonymous user is provided.
        @Override
        Future<Optional<User>> optional(RoutingContext ctx);

        /// Extract and authenticate user.Not accept anonymous users.
        @Override
        Future<User> required(RoutingContext ctx);

        /// Extract token from request. Normally should direct use {@link vat.api.implement.Authenticator.TokenReader }
        @Override
        Future<Optional<String>> token(RoutingContext ctx);
    }
}

package vat.foundation.users.api;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.SqlConnection;
import org.jetbrains.annotations.Nullable;
import vat.api.*;
import vat.api.Record;
import vat.api.meta.*;
import vat.api.trait.History;
import vat.api.utils.Fn;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

///
/// @author Zen.Liu
/// @since 2025-11-04

@Enhance
@Describe("_USERS")
public interface Users extends Activities {
    @Enhance
    @Describe(value = "_USERS_USER", domain = Users.class)
    @Table("foundation_users_user")
    interface User extends Actor.Base {
        @Describe("_USERS_USER_PROFILE")
        JsonObject profile();
    }

    @Enhance
    @Describe(value = "_USERS_CERTIFICATE", identity = "certIdentity", domain = Users.class)
    @Table("foundation_users_certificate")
    interface Certificate extends Record.Base, History {
        @Describe("_USERS_CERTIFICATE_USER")
        @Identity.Reference(value = User.class, provider = Users.class)
        @Column(indexed = {"user"})
        long user();

        @Describe("_USERS_CERTIFICATE_KIND")
        @Column(unique = {"identifier"})
        int kind();

        @Describe("_USERS_CERTIFICATE_IDENTIFIER")
        @Column(size = 128)
        String identifier();

        @Describe("_USERS_CERTIFICATE_CERTIFICATE")
        JsonObject certificate();

        @Describe("_USERS_CERTIFICATE_PROFILE")
        JsonObject profile();

    }

    @Enhance
    @Describe("_USERS_CHANGES")
    interface Changes extends Event.EnumBased<Changes.ChangeKind> {
        @Describe("_USERS_CHANGES_KIND")
        enum ChangeKind {
            @Describe("_USERS_CHANGES_KIND_REMOVED")
            REMOVED,
            @Describe("_USERS_CHANGES_KIND_CREATED")
            CREATED,
            @Describe("_USERS_CHANGES_KIND_CHANGED")
            CHANGED,
            @Describe("_USERS_CHANGES_KIND_LOGOUT")
            LOGOUT,
            @Describe("_USERS_CHANGES_KIND_LOGIN")
            LOGIN,
            @Describe("_USERS_CHANGES_KIND_PROFILE_CHANGED")
            PROFILE_CHANGED
        }

        @Describe("_USERS_CHANGES_USER")
        long user();

        @Describe(value = "_USERS_CHANGES_CERT", desc = "_DESC_USERS_CHANGES_CERT")
        long cert();
    }

    //region action

    /// 标准方法
    @Describe("_USERS_ACT_USER_IDENTITY")
    @Access
    Future<Optional<User>> identity(long id);

    @Describe("_USERS_ACT_CERT_IDENTITY")
    @Access
    Future<Optional<Certificate>> identityCert(long id);

    @Enhance
    @Describe("_USERS_CERT")
    interface Cert extends Data {
        @Describe("_USERS_CERTIFICATE_KIND")
        int kind();

        @Validate(value = "NONE_BLANK", construct = true)
        @Describe("_USERS_CERTIFICATE_IDENTIFIER")
        String identifier();

        @Validate(value = "NONE_EMPTY_JSON", construct = true)
        @Describe("_USERS_CERTIFICATE_CERTIFICATE")
        JsonObject secret();
    }

    @Auditing(mode = Auditing.Mode.FAILURE)
    @Describe("_USERS_REGISTER")
    Future<User> register(Cert cert);

    @Enhance
    @Describe("_USERS_CERT_UPDATE")
    interface CertUpdate extends Data {
        @Describe("_USERS_CERTIFICATE_KIND")
        int kind();

        @Validate(value = "NONE_BLANK", construct = true)
        @Describe("_USERS_CERTIFICATE_IDENTIFIER")
        String identifier();

        @Validate(value = "NONE_EMPTY_JSON", construct = true)
        @Describe("_USERS_CERT_UPDATE_OLD_CERTIFICATE")
        JsonObject secret();

        @Validate(value = "NONE_EMPTY_JSON", construct = true)
        @Describe("_USERS_CERT_UPDATE_NEW_CERTIFICATE")
        JsonObject newSecret();
    }
    /// check a cert if registered
    @Describe("_USERS_CERTIFICATION_CHECK")
    Future<Optional<User>> check(Cert cert);

    @Describe("_USERS_CERTIFICATION")
    @Auditing(mode = Auditing.Mode.FAILURE)
    Future<User> certificate(Cert cert);

    @Enhance
    @Describe("_USERS_CERT_PROFILE_ACCESS")
    interface CertProfileAccess extends Data {
        @Describe("_USERS_CERTIFICATE_KIND")
        int kind();

        @Validate(value = "NONE_BLANK", construct = true)
        @Describe("_USERS_CERTIFICATE_IDENTIFIER")
        String identifier();

        @Describe("_USERS_CERT_PROFILE_ACCESS_PATH")
        JsonArray path();

        @Describe("_USERS_CERT_PROFILE_ACCESS_DATA")
        Optional<JsonObject> data();
    }

    /// fetch a certificate profile, request data must be null
    @Describe("_USERS_ACT_CERT_PROFILE_FETCH")
    Future<Optional<JsonObject>> certProfileFetch(CertProfileAccess data);

    /// update a certificate profile, request data is required
    @Describe("_USERS_ACT_CERT_PROFILE_UPDATE")
    Future<Boolean> certProfileUpdate(CertProfileAccess data);

    @Enhance
    @Describe("_USERS_PROFILE_UPDATE")
    interface ProfileUpdate extends Data.Request<ProfileUpdate>, Entity.Entry {
        @Describe("_USERS_PROFILE_UPDATE_PATH")
        Optional<JsonArray> path();

        @Describe("_USERS_PROFILE_UPDATE_DATA")
        JsonObject data();

        @Override
        default void doValidate() {
            if (data() == null) throw DomainError.System.badRequest("data required");
            if (version() < 0) throw DomainError.System.badRequest("version required");
            if (id() <= 0) throw DomainError.System.badRequest("id required");
        }
    }

    @Enhance
    @Describe("_USERS_ACT_PROFILE_READ")
    interface ProfileRead extends Data.Request<ProfileRead> {
        long id();

        @Describe(value = "_USERS_PROFILE_READ_PATH")
        Optional<JsonArray> path();


        @Override
        default void doValidate() {
            if (id() <= 0) throw DomainError.System.badRequest("id required");
        }
    }

    @Describe("_USERS_UPDATE_PROFILE")
    @Auditing(mode = Auditing.Mode.FAILURE)
    Future<User> updateProfile(ProfileUpdate req);

    @Describe("_USERS_ACT_READ_PROFILE")
    @Auditing(mode = Auditing.Mode.FAILURE)
    Future<JsonObject> readProfile(ProfileRead req);

    /// Wrapper to read a Data object
    default <T extends Data> Future<T> readProfile(ProfileRead req, Function<JsonObject, T> read) {
        return readProfile(req)
                .map(read)
                .map(Fn.peek(v -> {
                    if (v instanceof Data.Validation<?> val) val.doValidate();
                }));
    }
    //endregion

    @Enhance
    interface Contextual extends Users, Domain.Context {
        //region context
        @Storage("/schema/users/user")
        default Store<User> users(@Nullable SqlConnection tx) {
            throw new IllegalStateException("Not Implemented");
        }

        @Storage("/schema/users/certificate")
        default Store<Certificate> certs(@Nullable SqlConnection tx) {
            throw new IllegalStateException("Not Implemented");
        }

        @Config
        default boolean debug() {
            return false;
        }

        @Publish
        default void changed(Consumer<Changes> consumer) {
        }

        @Errors
        default DomainError alreadyRegistered() {
            return DomainError.User.badRequestNotify("已经注册");
        }

        @Errors
        default DomainError unsupportedKind() {
            return DomainError.User.badRequestNotify("不支持的方式");
        }

        @Errors
        default DomainError invalidCertificate() {
            return DomainError.User.unauthorizedNotify("请检查重试");
        }
        //endregion

    }
}

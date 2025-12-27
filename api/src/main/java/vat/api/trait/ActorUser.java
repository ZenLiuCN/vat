package vat.api.trait;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import vat.api.Data;
import vat.api.utils.Buf;
import vat.api.utils.Fn;

import java.util.Objects;
import java.util.Optional;

///
/// @author Zen.Liu
/// @since 2025-12-09


@SuppressWarnings("unused")
public record ActorUser(
        long uid
) implements Data.Request<ActorUser>, Data.Binary, Applicative<ActorUser>, UserRefer {
    public ActorUser(JsonObject v) {
        this(v.getLong("actor"));
    }

    public ActorUser(Buf v) {
        this(v.i64());
    }

    public ActorUser(JsonObject v, Void ignore) {
        this((long) Objects.requireNonNull(Fn.parseNullable(v.getString("actor"), Long::parseLong), "actor required"));
    }

    @Override
    public Future<ActorUser> applyValidateFuture() {
        return Future.succeededFuture(this);
    }

    @Override
    public ActorUser applyValidate() {
        return this;
    }

    @Override
    public JsonObject toJS() {
        return JsonObject.of(
                "actor", uid + ""
                            );
    }

    @Override
    public JsonObject toJson() {
        return asJson();
    }

    @Override
    public JsonObject asJson() {
        return JsonObject.of(
                "actor", uid
                            );
    }

    @Override
    public Optional<Long> actor() {
        return Optional.of(uid);
    }

    @Override
    public Buf toBuf(Buf buf) {
        return buf.i64(uid);
    }

    @Override
    public ActorUser _this() {
        return this;
    }

    @Override
    public long user() {
        return uid;
    }
}

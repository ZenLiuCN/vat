package vat.api;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import lombok.ToString;
import vat.api.meta.Describe;
import vat.api.trait.Applicative;
import vat.api.utils.Buf;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author Zen.Liu
 * @since 2025-10-20
 */
@Prototype
public interface Data extends Domain {
    interface Comparable<T extends java.lang.Comparable<T>> extends Data, java.lang.Comparable<T> {
    }

    @Override
    default Class<? extends Domain> domainIdentity() {
        return this.getClass();
    }

    default JsonObject toJson() {
        return asJson().copy();
    }


    JsonObject asJson();

    /// to Js compatible json data
    default JsonObject toJS() {
        return toJson();
    }

    /// binary compatible data
    @Prototype
    interface Binary extends Data {
        default Buffer toBuffer() {
            return toBuf().raw();
        }

        default Buffer toBuffer(Buffer buf) {
            return toBuf(Buf.of(buf)).raw();
        }

        Buf toBuf(Buf buf);

        default Buf toBuf() {
            return toBuf(Buf.of());
        }
    }

    /// a pojo based prototype
    abstract class DataObject<T extends DataObject<T>> implements Data, Applicative<T> {
        @Override
        public JsonObject toJson() {
            return asJson();
        }

    }

    /// a json object based prototype
    @ToString(includeFieldNames = false)
    abstract class DataJson<T extends DataJson<T>> implements Data, Applicative<T> {
        @ToString.Include
        protected final JsonObject json;

        protected DataJson(JsonObject json) {
            this.json = json;
        }

        @Override
        public JsonObject asJson() {
            return json;
        }


    }

    interface Accessor<T> {

        <R> R apply(Function<T, R> m);

        T accept(Consumer<T> m);

        T accept(boolean cond, Consumer<T> m);

        boolean test(Predicate<T> m);

        <R> Future<R> applyFuture(Function<T, Future<R>> m);

        Future<T> acceptFuture(Function<T, Future<Void>> m);

        Future<T> acceptFuture(boolean cond, Function<T, Future<Void>> m);

        Future<Boolean> testFuture(Function<T, Future<Boolean>> m);
    }

    /// request data object to invoke a activities action.
    interface Request<T extends Request<T>> extends Validation<T> {
        @Describe(value = "_AUTHORITY_USER", identity = "vat.foundation.users.api.Users::identity")
        Optional<Long> actor();
    }

    interface Validation<T extends Validation<T>> extends Data {

        /// validate request data matches requirement
        default void doValidate() {

        }

        /// validate then returns self, otherwise returns failure.
        Future<T> applyValidateFuture();

        T applyValidate();
    }
}

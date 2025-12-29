package vat.api.implement;

import io.vertx.core.json.JsonObject;
import org.jspecify.annotations.Nullable;
import vat.api.DomainError;

import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

///
/// @author Zen.Liu
/// @since 2025-10-30


public interface Validators {
    Consumer<@Nullable String> NONE_BLANK = s -> {
        if (s == null || s.isBlank()) throw DomainError.System.badRequest("invalid string value:", s);
    };
    Consumer<@Nullable String> NONE_EMPTY = s -> {
        if (s == null || s.isBlank()) throw DomainError.System.badRequest("invalid string value:", s);
    };
    Consumer<@Nullable JsonObject> NONE_EMPTY_JSON = s -> {
        if (s == null || s.isEmpty()) throw DomainError.System.badRequest("invalid object value:", s);
    };

    interface NullableValidator<T> extends Consumer<@Nullable T> {
    }

    interface LongValidator extends LongConsumer {
        void accept(long l);
    }

    interface ByteValidator extends IntConsumer {
        void accept(byte l);

        @Override
        default void accept(int value) {
            accept(((byte) value));
        }
    }

    interface ShortValidator extends IntConsumer {
        void accept(short l);

        @Override
        default void accept(int value) {
            accept(((short) value));
        }
    }

    interface IntValidator extends IntConsumer {
        void accept(int l);
    }


    interface FloatValidator {
        void accept(float l);
    }

    interface DoubleValidator extends DoubleConsumer {
        void accept(double l);
    }


}

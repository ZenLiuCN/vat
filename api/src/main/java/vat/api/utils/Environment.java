package vat.api.utils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

///
/// @author Zen.Liu
/// @since 2025-11-12


@SuppressWarnings("unused")
public interface Environment {
    Function<String, String> PROPERTY_ENV = Cases.convert(Cases.LOWER_QUALIFIED_CASE, Cases.UPPER_SNAKE_CASE);
    Function<String, String> POINTER_ENV = s -> {
        if (!s.startsWith("/")) s = "/" + s;
        return s.contains(".") ? s.replaceAll("\\.", "/") : s;
    };

    static <T> Optional<T> of(String key, Function<String, T> decode) {
        return Optional.ofNullable(System.getProperty(key))
                       .or(() -> Optional.ofNullable(System.getenv(PROPERTY_ENV.apply(key))))
                       .filter(Predicate.not(String::isBlank))
                       .map(decode);
    }

    static Optional<String> string(String key) {
        return of(key, Function.identity());
    }

    static Optional<Boolean> bool(String key) {
        return of(key, Boolean::parseBoolean);
    }

    static Optional<Integer> integer(String key) {
        return of(key, Integer::parseInt);
    }

    static <T> Lazy<T> argument(String key, Function<String, T> decode,
                                BiFunction<Pointer, JsonObject, Optional<T>> reader) {
        return new Lazy<>(key, decode, reader);
    }

    static Lazy<Boolean> ofBoolean(String key) {
        return argument(key, Boolean::parseBoolean, Pointer::getBoolean);
    }

    static Lazy<String> ofString(String key) {
        return argument(key, Function.identity(), Pointer::getString);
    }

    static Lazy<Integer> ofInteger(String key) {
        return argument(key, Integer::parseInt, Pointer::getInteger);
    }

    static Lazy<Long> ofLong(String key) {
        return argument(key, Long::parseLong, Pointer::getLong);
    }

    static Lazy<JsonObject> ofObject(String key) {
        return argument(key, JsonObject::new, Pointer::getObject);
    }

    static Lazy<JsonArray> ofArray(String key) {
        return argument(key, JsonArray::new, Pointer::getArray);
    }

    ///  should only contain one type of argument.
    @EqualsAndHashCode
    @Accessors(fluent = true)
    final class Lazy<T> {
        @Getter
        @Nullable
        private String key;
        @Nullable
        private JsonObject config;
        @Nullable
        private T value;
        @Nullable
        private Function<String, T> decode;
        @Nullable
        private BiFunction<Pointer, JsonObject, Optional<T>> reader;

        public Lazy<T> config(JsonObject config) {
            this.config = config;
            return this;
        }

        public Lazy(String key, Function<String, T> decode, BiFunction<Pointer, JsonObject, Optional<T>> reader) {
            this.key = key;
            this.decode = decode;
            this.reader = reader;
        }


        public Optional<T> get() {
            if (decode == null) return Optional.ofNullable(value);
            //noinspection DataFlowIssue
            var v = Optional.ofNullable(config)
                            .flatMap(j -> reader.apply(Pointer.of(POINTER_ENV.apply(key)), j))
                            .or(() -> Optional
                                    .ofNullable(System.getProperty(key))
                                    .or(() -> Optional.ofNullable(System.getenv(PROPERTY_ENV.apply(key))))
                                    .filter(Predicate.not(String::isBlank))
                                    .map(decode)
                               );
            value = v.orElse(null);
            key = null;
            config = null;
            decode = null;
            reader = null;
            return v;
        }
    }

}

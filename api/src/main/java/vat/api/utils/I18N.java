package vat.api.utils;

import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import java.util.stream.StreamSupport;

/// I18N helper
///
/// @author Zen.Liu
/// @since 2025-11-13


public interface I18N {
    Logger log = LoggerFactory.getLogger(I18N.class);
    ConcurrentHashMap<String, JsonObject> messages = new ConcurrentHashMap<>();
    AtomicReference<String> locale = new AtomicReference<>(Environment.string("activate.locate")
            .map(x -> x.replace("-", "#"))
            .map(Locale::of)
            .or(() -> Optional.of(Locale.getDefault()))
            .map(x -> x.toString().replace("#", "-") + ".json")
            .orElseThrow()
    );

    static private JsonObject load(String prefix) {
        return messages.computeIfAbsent(prefix, p -> {
            var name = p + "_" + locale;
            try {
                var res = I18N.class.getClassLoader().getResources(name);
                return StreamSupport.stream(
                                Spliterators.spliteratorUnknownSize(res.asIterator(), Spliterator.ORDERED | Spliterator.NONNULL),
                                false)
                        .map(u -> {
                            try (var inputStream = u.openStream()) {
                                return new JsonObject(new String(inputStream.readAllBytes()));
                            } catch (IOException e) {
                                log.info("reading i18n file {}", u, e);
                                return new JsonObject();
                            }
                        })
                        .sorted(Comparator.comparingInt(x -> x.getInteger("locale_order", 0)))
                        .peek(x -> x.remove("locale_order"))
                        .reduce(new JsonObject(), JsonObject::mergeIn);
            } catch (IOException e) {
                log.error("reading i18n {} ", name, e);
                return new JsonObject();
            }
        });
    }

    static String get(String prefix, String key) {
        return Optional.ofNullable(load(prefix))
                .map(x -> x.getString(key))
                .orElse(key);
    }

    static UnaryOperator<String> translator(String prefix) {
        return s -> get(prefix, s);
    }
}

package vat.codegen.utils;

import io.vertx.core.json.JsonObject;
import vat.api.utils.Lazy;

import java.io.IOException;
import java.util.Comparator;
import java.util.Locale;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

///
/// @author Zen.Liu
/// @since 2025-11-07


public interface I18N {


    @SuppressWarnings("CallToPrintStackTrace")
    Lazy<JsonObject> values = Lazy.of(() -> {
        var locale = Configuration.LOCALE.get().orElseGet(Locale::getDefault);
        var name = "meta_" + locale.toString().replace("#", "-") + ".json";
        try {
            var res = I18N.class.getClassLoader().getResources(name);//Thread.currentThread().getContextClassLoader().getResources(name);
            if (!res.hasMoreElements()) {
                System.err.println("found nothing for " + name);
            }
            return StreamSupport.stream(
                            Spliterators.spliteratorUnknownSize(res.asIterator(), Spliterator.ORDERED | Spliterator.NONNULL),
                            false)
                    .map(u -> {
                        try (var inputStream = u.openStream()) {
                            return new JsonObject(new String(inputStream.readAllBytes()));
                        } catch (IOException e) {
                            System.err.println("read i18n " + u + " error");
                            e.printStackTrace();
                            return new JsonObject();
                        }
                    })
                    .sorted(Comparator.comparingInt(x -> x.getInteger("locale_order", 0)))
                    .peek(x -> x.remove("locale_order"))
                    .reduce(new JsonObject(), JsonObject::mergeIn);
        } catch (IOException e) {
            System.err.println("read i18n " + name + " fail");
            e.printStackTrace();
            return new JsonObject();
        }
    });

    static String get(String key) {
        if (key.isBlank() || key.charAt(0) != '_') return key;
        return values.get().getString(key, key);
    }
}

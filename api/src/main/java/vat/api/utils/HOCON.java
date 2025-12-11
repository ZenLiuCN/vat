package vat.api.utils;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.io.StringReader;
import java.nio.file.Path;

///
/// @author Zen.Liu
/// @since 2025-11-27


public interface HOCON {
    String MIME_TYPE = "application/hocon";

    static JsonObject read(Path f) {
        return new JsonObject(ConfigFactory
                .parseFile(f.toFile())
                .resolve()
                .root()
                .render(ConfigRenderOptions.concise()
                        .setJson(true)
                        .setComments(false)
                        .setFormatted(false)));
    }

    static Future<JsonObject> read(Vertx vertx, Path f) {
        return vertx.executeBlocking(() -> read(f));
    }

    static JsonObject parse(String s) {
        try (var f = new StringReader(s)) {
            return new JsonObject(ConfigFactory
                    .parseReader(f)
                    .resolve()
                    .root()
                    .render(ConfigRenderOptions.concise()
                            .setJson(true)
                            .setComments(false)
                            .setFormatted(false)));
        }
    }

    static Future<JsonObject> parse(Vertx vertx, String s) {
        return vertx.executeBlocking(() -> parse(s));
    }
}

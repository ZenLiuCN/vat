package test;

import io.vertx.core.json.JsonObject;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import vat.api.metadata.MetaData;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.stream.Stream;

///
/// @author Zen.Liu
/// @since 2025-11-11


public class MetaLoadTest {
    @Test
    void load()  {
        loopMeta(pth -> {
            pth.forEach(v -> {
                try {
                    var jo = new JsonObject(Files.readString(v));
                    System.out.println(MetaData.Codec.DOMAIN_DATA.get(jo).asJson().encodePrettily());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        });
    }

    @SneakyThrows
    static void loopMeta(Consumer<Stream<Path>> act) {
        var p = MetaLoadTest.class.getResource("../../classes/META-INF/meta/");
        if (p == null) throw new IllegalStateException();
        var uri = p.toURI();
        if (uri.getScheme().equals("jar")) {
            var fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
            try (fileSystem; var s = Files.list(fileSystem.getPath("META-INF/meta/"))) {
                act.accept(s);
            }
        } else {
            try (var s = Files.list(Paths.get(p.toURI()))) {
                act.accept(s);
            }
        }
    }
}

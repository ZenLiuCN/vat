package vat.codegen.utils;


import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.TypeSpec;
import io.vertx.core.buffer.Buffer;
import lombok.SneakyThrows;
import vat.api.utils.Environment;
import vat.api.utils.HOCON;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;



///
/// @author Zen.Liu
/// @since 2025-10-27


public interface FileTool extends LoggingTool {
    boolean DEBUG=Environment.bool("codegen.debug").orElse(false);
    @SneakyThrows
    static void initializeConfig(Filer filer) {
        var root = moduleRoot(filer);
        if (root == null) {
            System.err.println(" not found module root ");
            return;
        }
        if(DEBUG)System.out.println("found module root: " + root);
        var enhance = root.resolve(".enhance");
        if (!enhance.toFile().exists()) return;

        try {
            var data = Files.readString(enhance);
            if(DEBUG) System.out.println("load enhance: " + data);
            if (data.isBlank()) return;
            var obj = HOCON.parse(data);
            if(DEBUG) System.out.println("read json: "+obj);
            Configuration.set(obj);
        } catch (IOException e) {
            System.err.println("read " + enhance + " fail: " + e.getMessage());
        }
    }

    Filer filer();

    Path path();

    void path(Path path);

    default Path getModuleRoot() {
        var p = path();
        if (p != null) return p;
        p = moduleRoot(filer());
        if (p != null) path(p);
        return p;
    }

    static Path moduleRoot(Filer filer) {
        try {
            var dummyFile = filer.createResource(StandardLocation.CLASS_OUTPUT, "", "dummy_" + (int) (Math.random() * 100) + "+" + System.currentTimeMillis() + ".temp");
            var uri = dummyFile.toUri();
            var path = uri.toString();
            if (path.startsWith("file:///")) {
                path = path.substring("file:///".length());
            }
            var p = Path.of(path).toAbsolutePath().getParent(); // [build|target]/classes
            while (!p.getFileName().endsWith("classes") && !p.getFileName().endsWith("test-classes")) {
                p = p.getParent();
            }
            p = p.getParent();
            var name = p.getFileName().toString();
            if (name.equals("build") || name.equals("target")) {
                p = p.getParent();
            }
            dummyFile.delete();
            return p;
        } catch (Exception e) {
            System.err.println("lookup module root failed:" + e.getMessage());
            return null;
        }
    }

    @SneakyThrows
    default void saveToRoot(String file, Buffer data) {
        var root = getModuleRoot();
        if (root != null)
            Files.write(root.resolve(file), data.getBytes());
        else {
            var out = filer().createResource(
                    StandardLocation.SOURCE_OUTPUT,
                    "",
                    file);
            try (var writer = out.openOutputStream()) {
                writer.write(data.getBytes());
            }
        }
    }


    @SneakyThrows
    default void save(JavaFile file) {
        file.writeTo(filer());
    }

    @SneakyThrows
    default void save(String pkg, TypeSpec type) {
        JavaFile.builder(pkg, type).build().writeTo(filer());
    }

    @SneakyThrows
    default void saveResource(String identity, Buffer buffer) {
        var out = filer().createResource(
                StandardLocation.CLASS_OUTPUT,
                "",
                identity);
        try (var writer = out.openOutputStream()) {
            writer.write(buffer.getBytes());
        }
    }

    default void saveServiceProvider(Class<?> type, String impl, Element where) {
        saveServiceProvider(type.getName(), impl, where);
    }

    @SneakyThrows
    default void saveServiceProvider(String type, String impl, Element where) {
        var out = filer().createResource(
                StandardLocation.CLASS_OUTPUT,
                "",
                "META-INF/services/" + type);
        try (var writer = out.openWriter()) {
            writer.write(impl);
        }
    }
}

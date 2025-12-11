package vat.codegen;

import com.google.auto.service.processor.AutoServiceProcessor;
import com.google.common.collect.ImmutableList;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.CompilationSubject;
import com.google.testing.compile.JavaFileObjects;
import lombok.SneakyThrows;
import vat.api.utils.Environment;
import vat.api.utils.Lazy;

import javax.annotation.processing.Processor;
import javax.tools.JavaFileObject;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.BooleanSupplier;

import static com.google.testing.compile.Compiler.javac;

class GeneratorTest {
    static final Lazy.MutableBoolean CLIP = Lazy.mutable((BooleanSupplier) ()->Environment.bool("test.codegen.clip").orElse(false));

    @SneakyThrows
    void testing(JavaFileObject code) {
        // --add-opens lombok/lombok.launch=vat.codegen
        var lombokAnnotationProcessor = GeneratorTest.class.getClassLoader()
                .loadClass("lombok.launch.AnnotationProcessorHider$AnnotationProcessor");
        var lombokClaimingProcessor = GeneratorTest.class.getClassLoader()
                .loadClass("lombok.launch.AnnotationProcessorHider$ClaimingProcessor");
        var cu = javac()
                .withOptions("-Xlint:unchecked")
                .withProcessors(new Generator(),
                        new AutoServiceProcessor(),
                        (Processor) lombokAnnotationProcessor.getConstructor().newInstance(),
                        (Processor) lombokClaimingProcessor.getConstructor().newInstance()).compile(code);
        try {
            CompilationSubject.assertThat(cu).succeeded();
        } catch (Exception e) {
            if (CLIP.get()) clip(cu);
            throw new RuntimeException(e);
        }
        if (CLIP.get()) clip(cu);
    }

    @SneakyThrows
    void testing(List<JavaFileObject> code) {
        var lombokAnnotationProcessor = GeneratorTest.class.getClassLoader()
                .loadClass("lombok.launch.AnnotationProcessorHider$AnnotationProcessor");
        var lombokClaimingProcessor = GeneratorTest.class.getClassLoader()
                .loadClass("lombok.launch.AnnotationProcessorHider$ClaimingProcessor");
        var cu = javac().withProcessors(new Generator(),
                (Processor) lombokAnnotationProcessor.getConstructor().newInstance(),
                (Processor) lombokClaimingProcessor.getConstructor().newInstance()).compile(code);
        try {
            CompilationSubject.assertThat(cu).succeeded();
        } catch (Exception e) {
            if (CLIP.get()) clip(cu);
            throw new RuntimeException(e);
        }
        if (CLIP.get()) clip(cu);
    }

    void testing(String fullQualifiedName, String code) {
        testing(JavaFileObjects.forSourceString(fullQualifiedName, code));
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    static void clip(Compilation cu) {
        var out = new ByteArrayOutputStream();
        var cls = cu.getClass().getDeclaredField("generatedFiles");
        cls.setAccessible(true);
        var fs = ((ImmutableList<JavaFileObject>) cls.get(cu)).stream()
                .filter(j -> !j.getKind().equals(JavaFileObject.Kind.CLASS))
                .toList();
        fs.forEach(j -> {
            if (j.getName().endsWith(".temp")) return;
            System.out.println("dump: " + j.getName());
            out.writeBytes("\n///%s\n".formatted(j.getName()).getBytes());
            try (var is = j.openInputStream()) {
                is.transferTo(out);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        if (!fs.isEmpty()) {
            System.out.print(out);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(out.toString()), null);
            System.err.println("generated sources copied to clipboard");
        }

    }


}

package vat.codegen;

import com.google.auto.service.AutoService;
import vat.codegen.utils.Context;
import vat.codegen.utils.FileTool;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

/// @author Zen.Liu
/// @since 2025-10-26
@AutoService(Processor.class)
public class Generator extends AbstractProcessor {
    static final List<Proc> processors = ServiceLoader
            .load(Proc.class, Generator.class.getClassLoader())
            .stream().map(ServiceLoader.Provider::get)
            .sorted(Comparator.comparingInt(Proc::order))
            .toList();
    static final Set<String> accept = processors
            .stream()
            .flatMap(x -> x.accept().stream())
            .distinct()
            .map(Class::getCanonicalName)
            .collect(Collectors.toSet());

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return accept;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        FileTool.initializeConfig(processingEnv.getFiler());

    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        var ctx = new Context(processingEnv, roundEnv);
        processors.forEach(x -> x.accept(ctx));
        return false;// allow other processors to process
    }


}

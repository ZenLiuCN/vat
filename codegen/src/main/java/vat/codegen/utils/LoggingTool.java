package vat.codegen.utils;

import org.slf4j.helpers.MessageFormatter;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

///
/// @author Zen.Liu
/// @since 2025-10-27


public interface LoggingTool {
    Messager messager();

    //region logging
    default void print(Diagnostic.Kind kind, String message, Element el, AnnotationMirror mirror) {
        var m = messager();
        if (el == null && mirror == null) m.printMessage(kind, message);
        else if (el != null && mirror == null) m.printMessage(kind, message, el);
        m.printMessage(kind, message, el, mirror);
    }

    default void log(Diagnostic.Kind kind, String message, Object... args) {
        if (args.length == 0) {
            print(kind, message, null, null);
            return;
        }
        var m = MessageFormatter.arrayFormat(message, args);
        Element el = null;
        AnnotationMirror mirror = null;
        for (var arg : args) {
            if (arg instanceof Element e) el = e;
            if (arg instanceof AnnotationMirror mr) mirror = mr;
        }
        print(kind, m.getMessage(), el, mirror);
    }

    default void info(String message, Object... args) {
        log(Diagnostic.Kind.NOTE, message, args);
    }

    default void warn(String message, Object... args) {
        log(Diagnostic.Kind.MANDATORY_WARNING, message, args);
    }

    default void warning(String message, Object... args) {
        log(Diagnostic.Kind.WARNING, message, args);
    }

    default void error(String message, Object... args) {
        log(Diagnostic.Kind.ERROR, message, args);
    }

    default void debug(String message, Object... args) {
        log(Diagnostic.Kind.OTHER, message, args);
    }
    //endregion
}

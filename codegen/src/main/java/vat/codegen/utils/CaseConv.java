package vat.codegen.utils;

import vat.api.utils.Cases;

import java.util.function.Function;

///
/// @author Zen.Liu
/// @since 2025-10-30


public interface CaseConv {
    Function<String, String> PASCAL_CAMEL = Cases.convert(Cases.PASCAL_CASE, Cases.CAMEL_CASE);
    Function<String, String> PASCAL_UPPER_SNAKE = Cases.convert(Cases.PASCAL_CASE, Cases.UPPER_SNAKE_CASE);
    Function<String, String> PASCAL_SNAKE = Cases.convert(Cases.PASCAL_CASE, Cases.SNAKE_CASE);
    Function<String, String> CODEC_NAME = Cases.convert(Cases.LOWER_QUALIFIED_CASE, Cases.UPPER_SNAKE_CASE);
    Function<String, String> CAMEL_SNAKE = Cases.convert(Cases.CAMEL_CASE, Cases.SNAKE_CASE);
    Function<String, String> CAMEL_UPPER_SNAKE = Cases.convert(Cases.CAMEL_CASE, Cases.UPPER_SNAKE_CASE);
    Function<String, String> UPPER_SNAKE_CAMEL = Cases.convert(Cases.UPPER_SNAKE_CASE, Cases.CAMEL_CASE);
    Function<String, String> UPPER_SNAKE_PASCAL = Cases.convert(Cases.UPPER_SNAKE_CASE, Cases.PASCAL_CASE);

    static CharSequence caption(CharSequence name) {
        if (name == null) return null;
        if (name.isEmpty()) return name;
        if (Character.isUpperCase(name.charAt(0))) return name;
        return Character.toUpperCase(name.charAt(0))+"" + name.subSequence(1,name.length());
    }

    static CharSequence captionWord(CharSequence name) {
        if (name == null) return null;
        if (name.isEmpty()) return name;

        return Character.toUpperCase(name.charAt(0))+ name.subSequence(1,name.length()).toString().toLowerCase();
    }
}

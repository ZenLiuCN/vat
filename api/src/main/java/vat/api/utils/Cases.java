package vat.api.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

///
/// @author Zen.Liu
/// @since 2025-10-23

@SuppressWarnings("unused")
sealed public interface Cases {


    static List<String> split(String s, String regex) {
        String[] words = s.split(regex);
        if (words.length == 1 && words[0].isEmpty()) {
            return Collections.emptyList();
        } else {
            return Arrays.asList(words);
        }
    }

    String format(Iterable<String> atoms);

    List<String> parse(String name);

    default String to(Cases dest, String name) {
        if (this == dest) return name;
        return dest.format(parse(name));
    }

    record CaseType(Function<Iterable<String>, String> format, Function<String, List<String>> parse) implements Cases {
        CaseType withFormat(Function<Iterable<String>, String> format) {
            return new CaseType(format, parse);
        }

        @Override
        public String format(Iterable<String> atoms) {
            return format.apply(atoms);
        }

        @Override
        public List<String> parse(String name) {
            return parse.apply(name);
        }
    }

    CaseType RAW = new CaseType(w -> String.join("", w), List::of);
    CaseType CAMEL_CASE = new CaseType(w -> {
        var sb = new StringBuilder();
        for (var word : w) {
            if (!word.isEmpty()) {
                var c = word.charAt(0);
                if (sb.isEmpty()) {
                    sb.append(Character.toLowerCase(c));
                    sb.append(word.toLowerCase(), 1, word.length());
                    continue;
                }
                sb.append(Character.toUpperCase(c));
                sb.append(word.toLowerCase(), 1, word.length());
            }
        }
        return sb.toString();
    }, s -> split(s, "(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])"));
    CaseType PASCAL_CASE = CAMEL_CASE.withFormat(w -> {
        var sb = new StringBuilder();
        for (var word : w) {
            if (!word.isEmpty()) {
                var c = word.charAt(0);
                if (Character.isLowerCase(c)) {
                    sb.append(Character.toUpperCase(c));
                    sb.append(word.toLowerCase(), 1, word.length());
                } else {
                    sb.append(word.charAt(0));
                    sb.append(word.toLowerCase(), 1, word.length());
                }
            }
        }
        return sb.toString();
    });
    Predicate<String> KEBAB_PATTERN = Pattern.compile("(?:\\p{Alnum}|(?<=\\p{Alnum})-(?=\\p{Alnum}))*").asMatchPredicate();
    CaseType KEBAB_CASE = new CaseType(w -> {
        var sb = new StringBuilder();
        for (var word : w) {
            if (!word.isEmpty()) {
                if (!sb.isEmpty()) {
                    sb.append('-');
                }
                sb.append(word.toUpperCase());
            }
        }
        return sb.toString();
    }, s -> {
        if (!KEBAB_PATTERN.test(s)) {
            throw new IllegalArgumentException("Invalid kebab case:" + s);
        }
        return split(s, "\\-");
    });
    CaseType UPPER_KEBAB_CASE = KEBAB_CASE.withFormat(w -> {
        var sb = new StringBuilder();
        for (var word : w) {
            if (!word.isEmpty()) {
                if (!sb.isEmpty()) {
                    sb.append('-');
                }
                sb.append(word.toLowerCase());
            }
        }
        return sb.toString();
    });
    Predicate<String> SNAKE_PATTERN = Pattern.compile("(?:\\p{Alnum}|(?<=\\p{Alnum})_(?=\\p{Alnum}))*").asMatchPredicate();
    CaseType SNAKE_CASE = new CaseType(w -> {
        var sb = new StringBuilder();
        for (var word : w) {
            if (!word.isEmpty()) {
                if (!sb.isEmpty()) {
                    sb.append('_');
                }
                sb.append(word.toLowerCase());
            }
        }
        return sb.toString();
    }, s -> {
        if (!SNAKE_PATTERN.test(s)) {
            throw new IllegalArgumentException("Invalid snake case:" + s);
        }
        return split(s, "\\_");
    });
    CaseType UPPER_SNAKE_CASE = SNAKE_CASE.withFormat(w -> {
        var sb = new StringBuilder();
        for (var word : w) {
            if (!word.isEmpty()) {
                if (!sb.isEmpty()) {
                    sb.append('_');
                }
                sb.append(word.toUpperCase());
            }
        }
        return sb.toString();
    });
    Predicate<String> QUALIFIED_PATTERN = Pattern.compile("(?:\\p{Alnum}|(?<=\\p{Alnum})\\.(?=\\p{Alnum}))*").asMatchPredicate();
    CaseType QUALIFIED_CASE = new CaseType(w -> {
        var sb = new StringBuilder();
        for (var word : w) {
            if (!word.isEmpty()) {
                if (!sb.isEmpty()) {
                    sb.append('.');
                }
                sb.append(word);
            }
        }
        return sb.toString();
    }, s -> {
        if (!QUALIFIED_PATTERN.test(s)) {
            throw new IllegalArgumentException("Invalid qualified case:" + s);
        }
        return split(s, "\\.");
    });
    CaseType LOWER_QUALIFIED_CASE = QUALIFIED_CASE.withFormat(w -> {
        var sb = new StringBuilder();
        for (var word : w) {
            if (!word.isEmpty()) {
                if (!sb.isEmpty()) {
                    sb.append('.');
                }
                sb.append(word.toLowerCase());
            }
        }
        return sb.toString();
    });
    CaseType UPPER_QUALIFIED_CASE = QUALIFIED_CASE.withFormat(w -> {
        var sb = new StringBuilder();
        for (var word : w) {
            if (!word.isEmpty()) {
                if (!sb.isEmpty()) {
                    sb.append('.');
                }
                sb.append(word.toUpperCase());
            }
        }
        return sb.toString();
    });

    static Function<String, String> convert(CaseType from, CaseType to) {
        return from.parse.andThen(to.format);
    }
}

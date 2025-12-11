package vat.api.store;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

///
/// @author Zen.Liu
/// @since 2025-10-23
public record Writer(Rendered rendered, StringBuilder builder, AtomicReference<Renderable> exprRefer) {
    public Writer(Rendered rendered) {
        this(rendered, new StringBuilder(), new AtomicReference<>());
    }

    void expr(Renderable expr) {
        exprRefer.set(expr);
    }

    public Renderable expr() {
        return exprRefer.get();
    }

    @Override
    public @NotNull String toString() {
        return builder.toString();
    }

    public Writer wp(String s) {
        builder.append(' ').append(s).append(' ');
        return this;
    }

    public Writer wp(char s) {
        builder.append(' ').append(s).append(' ');
        return this;
    }

    public Writer w(String s) {
        builder.append(s);
        return this;
    }


    public Writer w(char s) {
        builder.append(s);
        return this;
    }

    public Writer w(int s) {
        builder.append(s);
        return this;
    }


    public Writer use(Consumer<Writer> a) {
        a.accept(this);
        return this;
    }

    public <T> Writer use(T t, BiConsumer<Writer, T> a) {
        a.accept(this, t);
        return this;
    }

    public Writer sp() {
        return w(' ');
    }

    public Writer dot() {
        return w('.');
    }

    public Writer p(String s) {
        return w("#{").w(s).w('}');
    }

    public Writer render(Object value) {
        return rendered.render(this, value);
    }

    public Writer when(boolean cond, String suc) {
        return cond ? w(suc) : this;
    }

    public Writer when(boolean cond, String suc, String el) {
        return cond ? w(suc) : w(el);
    }

    public <T> Writer when(T cond, BiConsumer<Writer, T> suc) {
        return when(cond, null, suc, null);
    }

    public <T> Writer when(T cond, @Nullable Consumer<Writer> before, BiConsumer<Writer, T> suc, @Nullable Consumer<Writer> after) {
        if (cond != null) {
            if (before != null) before.accept(this);
            suc.accept(this, cond);
            if (after != null) after.accept(this);
        }
        return this;
    }

    public Writer when(String cond, BiConsumer<Writer, String> suc) {
        return when(cond, null, suc, null);
    }

    public Writer when(String cond, @Nullable Consumer<Writer> before, BiConsumer<Writer, String> suc) {
        return when(cond, before, suc, null);
    }

    public Writer when(String cond, BiConsumer<Writer, String> suc, @Nullable Consumer<Writer> after) {
        return when(cond, null, suc, after);
    }

    public Writer when(String cond, @Nullable Consumer<Writer> before, BiConsumer<Writer, String> suc, @Nullable Consumer<Writer> after) {
        if (cond != null && !cond.isEmpty()) {
            if (before != null) before.accept(this);
            suc.accept(this, cond);
            if (after != null) after.accept(this);
        }
        return this;
    }

    public Writer when(boolean cond, Consumer<Writer> suc, Consumer<Writer> el) {
        if (cond) suc.accept(this);
        else el.accept(this);
        return this;
    }

    public Writer bracket(String s) {
        return w('[').w(s).w(']');
    }

    public Writer dblQuote(String s) {
        return w('"').w(s).w('"');
    }

    public Writer quote(String s) {
        return w('\'').w(s).w('\'');
    }

    public Writer backQuote(String s) {
        return w('`').w(s).w('`');
    }

    public String escape(String s, char escape) {
        return s.replaceAll(Pattern.quote("" + escape), "" + escape + escape);
    }

    public Writer apply(Consumer<Writer> act) {
        act.accept(this);
        return this;
    }



    public interface IndexConsumer<T> {
        void accept(Writer w, int index, T t);
    }

    public interface RangeConsumer {
        void accept(Writer w, int value);
    }

    public Writer range(int n, RangeConsumer o) {
        for (int i = 0; i < n; i++) {
            o.accept(this, i);
        }
        return this;
    }

    public Writer range(int n, char sep, RangeConsumer o) {
        for (int i = 0; i < n; i++) {
            if (i > 0) w(sep);
            o.accept(this, i);
        }
        return this;
    }

    public Writer range(int n, String sep, RangeConsumer o) {
        for (int i = 0; i < n; i++) {
            if (i > 0) w(sep);
            o.accept(this, i);
        }
        return this;
    }

    public <T, E extends Iterable<T>> Writer each(E col, BiConsumer<Writer, T> a) {
        for (var e : col) {
            a.accept(this, e);
        }
        return this;
    }

    public <T, E extends Iterable<T>> Writer each(E col, char sep, BiConsumer<Writer, T> a) {
        var f = true;
        for (var e : col) {
            if (f) f = false;
            else w(sep);
            a.accept(this, e);

        }
        return this;
    }

    public <T, E extends Iterable<T>> Writer each(E col, String sep, BiConsumer<Writer, T> a) {
        var f = true;
        for (var e : col) {
            if (f) f = false;
            else w(sep);
            a.accept(this, e);

        }
        return this;
    }

    public <T, E extends Iterable<T>> Writer each(E col, IndexConsumer<T> a) {
        var i = 0;
        for (var e : col) {
            a.accept(this, i, e);
            i++;
        }
        return this;
    }


    public <T, R, E extends Iterable<T>> Writer each(E col, Function<T, R> mapper, BiConsumer<Writer, R> a) {
        for (var e : col) {
            a.accept(this, mapper.apply(e));
        }
        return this;
    }

    public <T, R, E extends Iterable<T>> Writer each(E col, Function<T, R> mapper, char sep, BiConsumer<Writer, R> a) {
        var f = true;
        for (var e : col) {
            if (f) f = false;
            else w(sep);
            a.accept(this, mapper.apply(e));

        }
        return this;
    }

    public <T, R, E extends Iterable<T>> Writer each(E col, Function<T, R> mapper, String sep,
                                                     BiConsumer<Writer, R> a) {
        var f = true;
        for (var e : col) {
            if (f) f = false;
            else w(sep);
            a.accept(this, mapper.apply(e));

        }
        return this;
    }

    public <T, R, E extends Iterable<T>> Writer each(E col, Function<T, R> mapper, IndexConsumer<R> a) {
        var i = 0;
        for (var e : col) {
            a.accept(this, i, mapper.apply(e));
            i++;
        }
        return this;
    }


    public <T> Writer each(T[] col, BiConsumer<Writer, T> a) {
        for (var e : col) {
            a.accept(this, e);
        }
        return this;
    }

    public <T> Writer each(T[] col, char sep, BiConsumer<Writer, T> a) {
        var f = true;
        for (var e : col) {
            if (f) f = false;
            else w(sep);
            a.accept(this, e);

        }
        return this;
    }

    public <T> Writer each(T[] col, String sep, BiConsumer<Writer, T> a) {
        var f = true;
        for (var e : col) {
            if (f) f = false;
            else w(sep);
            a.accept(this, e);

        }
        return this;
    }

    public <T> Writer each(T[] col, IndexConsumer<T> a) {
        var i = 0;
        for (var e : col) {
            a.accept(this, i, e);
            i++;
        }
        return this;
    }


    public <T> Writer each(Stream<T> col, char sep, BiConsumer<Writer, T> a) {
        var f = true;
        var it = col.iterator();
        while (it.hasNext()) {
            var e = it.next();
            if (f) f = false;
            else w(sep);
            a.accept(this, e);
        }
        return this;
    }

    public <T, R> Writer each(T[] col, Function<T, R> mapper, BiConsumer<Writer, R> a) {
        for (var e : col) {
            a.accept(this, mapper.apply(e));
        }
        return this;
    }

    public <T, R> Writer each(T[] col, Function<T, R> mapper, char sep, BiConsumer<Writer, R> a) {
        var f = true;
        for (var e : col) {
            if (f) f = false;
            else w(sep);
            a.accept(this, mapper.apply(e));

        }
        return this;
    }

    public <T, R> Writer each(T[] col, Function<T, R> mapper, String sep, BiConsumer<Writer, R> a) {
        var f = true;
        for (var e : col) {
            if (f) f = false;
            else w(sep);
            a.accept(this, mapper.apply(e));

        }
        return this;
    }

    public <T, R> Writer each(T[] col, Function<T, R> mapper, IndexConsumer<R> a) {
        var i = 0;
        for (var e : col) {
            a.accept(this, i, mapper.apply(e));
            i++;
        }
        return this;
    }
}

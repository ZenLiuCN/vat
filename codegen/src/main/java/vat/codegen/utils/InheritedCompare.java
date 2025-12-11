package vat.codegen.utils;

import org.jetbrains.annotations.NotNull;

import javax.lang.model.type.TypeMirror;

///
/// @author Zen.Liu
/// @since 2025-12-03
record InheritedCompare(TypeMirror own, Context ctx) implements Comparable<InheritedCompare> {
    @Override
    public int compareTo(@NotNull InheritedCompare o) {
        if (own.equals(o.own)) return 0;
        if (ctx.assignableTo(own, o.own))
            return 1;
        if (ctx.assignableTo(o.own, own))
            return -1;
        return 0;
    }
}

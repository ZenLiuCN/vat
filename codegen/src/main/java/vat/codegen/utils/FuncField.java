package vat.codegen.utils;

import com.palantir.javapoet.TypeName;
import lombok.With;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.jooq.lambda.tuple.Tuple3;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

///
/// @author Zen.Liu
/// @since 2025-12-03


public record FuncField(@With TypeMirror holder, String field) {

    public TypeName typeName() {
        return TypeName.get(holder);
    }

    public TypeElement element(Context ctx) {
        return Objects.requireNonNull(ctx.typeElementOf(holder));
    }
    public <T> Optional<T> genericInfo(Context ctx,Class<?> type,Function<List<? extends TypeMirror>,T> map) {
        return ctx.functorField(Objects.requireNonNull(ctx.typeElementOf(holder)),field).flatMap(x->ctx.genericInfo(x.asType(),type,map));
    }
    public Optional<Tuple2<TypeMirror,TypeMirror>> functionInfo(Context ctx) {
        return genericInfo(ctx,Function.class,a-> Tuple.tuple(a.getFirst(),a.get(1)));
    }

    public Optional<Tuple3<TypeMirror,TypeMirror,TypeMirror>> biFunctionInfo(Context ctx) {
        return genericInfo(ctx,BiFunction.class,a-> Tuple.tuple(a.getFirst(),a.get(1),a.get(2)));
    }

    public Optional<TypeMirror> predicateInfo(Context ctx) {
        return genericInfo(ctx, Predicate.class, List::getFirst);
    }

    public Optional<Tuple2<TypeMirror,TypeMirror>> biPredicateInfo(Context ctx) {
        return genericInfo(ctx,BiPredicate.class,a-> Tuple.tuple(a.getFirst(),a.get(1)));
    }
}

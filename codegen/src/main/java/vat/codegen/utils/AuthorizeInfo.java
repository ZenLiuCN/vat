package vat.codegen.utils;

import com.palantir.javapoet.CodeBlock;
import vat.api.Ability;
import vat.api.Data;
import vat.api.DomainError;
import vat.api.meta.Errors;
import vat.api.utils.Fn;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.Objects;
import java.util.function.Predicate;

///
/// @author Zen.Liu
/// @since 2025-12-09

public record AuthorizeInfo(
        TypeMirror ability,
        String authorize,
        FuncField authorized,
        boolean requireAuth,
        /// a supplier lambda
        CodeBlock badRequest,
        /// a supplier lambda
        CodeBlock forbidden0,
        /// a supplier lambda or biFunction lambda
        CodeBlock forbidden1
) {
    public static AuthorizeInfo parse(
            AnnotatedValue av,
            Domain domain,
            TypeElement context,
            ExecutableElement method) {
        var ctx = domain.ctx();
        var ff = av.getString()
                .filter(Predicate.not(String::isBlank))
                .map(x -> new FuncField(av.getType("holder")
                        .filter(t -> !ctx.isVoid(t))
                        .orElseGet(context::asType), x))
                .orElse(null);
        if (ff == null) throw new IllegalStateException("Authorize Processor not configure: " + method);
        var isBiPred = ff.biPredicateInfo(ctx);
        if (isBiPred.isEmpty())
            throw new IllegalStateException("Authorize Processor should be BiPredicate: " + method + " with " + ff);
        var bip = isBiPred.get();
        if (!ctx.rawAssignableTo(bip.v1, Data.Request.class))
            throw new IllegalStateException("Authorize Processor should be accept first parameter of Data.Request: " + method + " with " + ff);
        if (!ctx.rawAssignableTo(bip.v2, Ability.class))
            throw new IllegalStateException("Authorize Processor should be accept second parameter of Ability: " + method + " with " + ff);
        var inName = method.getParameters().getFirst().getSimpleName().toString();
        var inType = method.getParameters().getFirst().asType();
        if (!ctx.rawAssignableTo(inType, Data.Request.class))
            throw new IllegalStateException("Authorized method must with input type of Data.Request: " + method + " with " + ff);
        var ability = av.getType("ability").orElse(bip.v2);
        var authorize = av.getString("authorize")
                .orElseGet(() -> "authorize" + ((DeclaredType) Objects.requireNonNull(ability, "missing ability type")).asElement().getSimpleName());
        var badRequestName = av.getString("badRequest").orElse(null);
        CodeBlock badRequest, forbidden0, forbidden1;
        if (badRequestName != null) {
            var fn = ctx.methods(context)
                    .filter(x -> x.isDefault() && ctx.sameType(x.getReturnType(), DomainError.class))
                    .filter(x -> AnnotatedValue.find(x, Errors.class).isPresent())
                    .filter(x -> x.getSimpleName().contentEquals(badRequestName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Not found valid bad request provider method " + badRequestName + " on context: " + context));
            var p = fn.getParameters();
            badRequest = switch (p.size()) {
                case 0 -> CodeBlock.of("()->$L()", badRequestName);
                case 1 -> ctx.rawAssignableTo(inType, p.getFirst().asType())
                        ? CodeBlock.of("()->$L($L)", badRequestName, inName)
                        : Fn.fail(new IllegalStateException("invalid bad request provider signature " + fn));
                default -> throw new IllegalStateException("invalid bad request provider signature " + fn);
            };
        } else {
            badRequest = DEFAULT_BAD_REQUEST;
        }
        var forbiddenName = av.getString("forbidden").orElse(null);
        if (forbiddenName != null) {
            var fn = ctx.methods(context)
                    .filter(x -> x.isDefault() && ctx.sameType(x.getReturnType(), DomainError.class))
                    .filter(x -> AnnotatedValue.find(x, Errors.class).isPresent())
                    .filter(x -> x.getSimpleName().contentEquals(forbiddenName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Not found valid forbidden provider method " + forbiddenName + " on context: " + context));
            var p = fn.getParameters();
            forbidden0 = switch (p.size()) {
                case 0 -> CodeBlock.of("()->$L()", forbiddenName);
                case 2 -> ctx.rawAssignableTo(inType, p.getFirst().asType())
                        ? CodeBlock.of("()->$L($L,null)", forbiddenName, inName)
                        : Fn.fail(new IllegalStateException("invalid forbidden provider signature " + fn));
                default -> throw new IllegalStateException("invalid forbidden provider signature " + fn);
            };
            forbidden1 = switch (p.size()) {
                case 0 -> CodeBlock.of("()->$L()", forbiddenName);
                case 2 -> ctx.rawAssignableTo(inType, p.getFirst().asType())
                          && ctx.rawAssignableTo(ability, p.get(1).asType())
                        ? CodeBlock.of("this::$L", forbiddenName)
                        : Fn.fail(new IllegalStateException("invalid forbidden provider signature " + fn));
                default -> throw new IllegalStateException("invalid forbidden provider signature " + fn);
            };
        } else {
            forbidden0 = DEFAULT_FORBIDDEN;
            forbidden1 = DEFAULT_FORBIDDEN;
        }
        return new AuthorizeInfo(ability, authorize, ff, av.getBoolean("allowSystem").orElse(false), badRequest, forbidden0, forbidden1);

    }

    static CodeBlock DEFAULT_FORBIDDEN = CodeBlock.of("()->$T.forbidden($S)", DomainError.System.class, "actor invalid");
    static CodeBlock DEFAULT_BAD_REQUEST = CodeBlock.of("()->$T.badRequest($S)", DomainError.System.class, "actor required");

    public void buildAuthorize(CodeBlock.Builder body) {
        if (requireAuth)
            body
                    .add("\n").add(".flatMap($T.concat($$->$L($$.actor().orElseThrow($L)).map($T.orElseThrow($L))))",
                            Fn.Flat.class,
                            authorize,
                            badRequest,
                            Fn.Maybe.class, forbidden0)
                    .add("\n").add(".map($T.predicate1($T.$L,$L))",
                            Fn.Pair.class,
                            authorized.holder(), authorized.field(), forbidden1)
                    ;
        else
            body
                    .add("\n").add(".flatMap($T.concat($$->$$.actor().map(fx->$L(fx).map($T.orElseThrow($L))).orElseGet(Future::succeededFuture)))",
                            Fn.Flat.class,
                            authorize,
                            Fn.Maybe.class, forbidden0
                    )
                    .add("\n").add(".map($T.predicate1($T.$L,$L))",
                            Fn.Pair.class,
                            authorized.holder(), authorized.field(), forbidden1)
                    ;
    }
}

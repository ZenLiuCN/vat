package vat.codegen.utils;

import vat.api.*;
import vat.api.Record;

import javax.lang.model.type.TypeMirror;

///
/// @author Zen.Liu
/// @since 2025-12-03
public record DomainTypes(
        TypeMirror actorType,
        TypeMirror abilityType,
        TypeMirror recordType,
        TypeMirror eventType,
        TypeMirror domainType,
        TypeMirror activitiesType,
        TypeMirror dataType
) {
    public DomainTypes(Context ctx) {
        this(
                ctx.typeElement(Actor.class).asType(),
                ctx.typeElement(Ability.class).asType(),
                ctx.typeElement(Record.class).asType(),
                ctx.typeElement(Event.class).asType(),
                ctx.typeElement(vat.api.Domain.Context.class).asType(),
                ctx.typeElement(Activities.class).asType(),
                ctx.typeElement(Data.class).asType());
    }
}

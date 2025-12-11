package vat.api.trait;

import vat.api.Data;
import vat.api.Entity;
import vat.api.Prototype;

///
/// @author Zen.Liu
/// @since 2025-12-05


@Prototype
sealed public interface Accessor<T extends Accessor<T>> extends Data.Request<T>{
    non-sealed interface Modificator<T extends Modificator<T>> extends Data.Request<T>, Entity.Entry, vat.api.trait.Accessor<T> {
    }

    non-sealed interface Remover<T extends Remover<T>> extends Data.Request<T>, Entity.Entry, vat.api.trait.Accessor<T> {

    }

    non-sealed interface Creator<T extends Creator<T>> extends Data.Request<T>, Entity.Entry, vat.api.trait.Accessor<T> {

    }
}

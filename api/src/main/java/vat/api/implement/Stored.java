package vat.api.implement;

import io.vertx.sqlclient.SqlClient;
import org.jetbrains.annotations.Nullable;
import vat.api.store.Dialect;
import vat.api.store.Model;


///
/// Generated for each domain entity.
/// 1. a static {@link Storage} field named as `STORAGE`.
/// 2. a static {@link T} field named as `MODEL` with default schema.
/// 3. a static {@link  java.util.function.Function} field named as `FACTORY` accept {@link Model} and product {@link vat.api.store.Field} array.
///
/// @author Zen.Liu
/// @since 2025-10-21
///
public interface Stored<ID,  E,T extends Model.Base<ID, E, T> & Stored<ID, E, T>> {
    interface Storage<ID,  E,T extends Model.Base<ID, E, T> & Stored<ID, E, T>> {
        T apply(SqlClient sql, Dialect dialect, @Nullable String schema);
    }

}

package vat.codegen.utils;

import io.vertx.core.json.JsonObject;
import vat.api.utils.Environment;

import java.util.List;
import java.util.Locale;
import java.util.Optional;


///
/// @author Zen.Liu
/// @since 2025-12-05


public interface Configuration {
    Environment.Lazy<Boolean> DDL_MYSQL = Environment.ofBoolean("codegen.ddl.mysql");
    Environment.Lazy<Boolean> DDL_POSTGRES = Environment.ofBoolean("codegen.ddl.postgres");
    Environment.Lazy<Locale> LOCALE = Environment.argument("codegen.locate",
            s -> Optional.ofNullable(s)
                    .map(x -> x.replace("-", "#"))
                    .map(Locale::of)
                    .orElseGet(Locale::getDefault),
            (p, j) -> p.getString(j)
                    .map(x -> x.replace("-", "#"))
                    .map(Locale::of)
    );
    List<Environment.Lazy<?>> ALL = List.of(DDL_MYSQL, DDL_POSTGRES, LOCALE);

    static void set(JsonObject v) {
        if (FileTool.DEBUG) ALL.forEach(k -> System.out.println("config key :" + k.key()));
        ALL.forEach(c -> c.config(v));
        if (FileTool.DEBUG) ALL.forEach(k -> System.out.println("config value :" + k.get()));
    }
}

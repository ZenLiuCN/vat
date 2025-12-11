package vat.core;

import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.core.json.JsonObject;
import vat.api.Data;
import vat.api.implement.Codec;
import vat.api.implement.CommonCodec;

import java.util.List;
import java.util.Optional;

///
/// @author Zen.Liu
/// @since 2025-11-20

public sealed interface ApplicationConfiguration extends Data {

    /// Disabled activities' className
    Optional<List<String>> disabled();

    /// Manually provided activities' className without SPI
    Optional<List<String>> manual();

    /// Remote retriever config, check {@link ConfigRetrieverOptions}
    Optional<JsonObject> retriever();

    /// Enable or disable SPI loader,default is true.
    Optional<Boolean> discovery();

    Optional<JsonObject> get(String activities);

    record Data(
            JsonObject asJson) implements ApplicationConfiguration {
        public Data() {
            this(new JsonObject());
        }

        @Override
        public final Class<ApplicationConfiguration> domainIdentity() {
            return ApplicationConfiguration.class;
        }

        @Override
        public Optional<Boolean> discovery() {
            return Optional.ofNullable(asJson.getBoolean("discovery"));
        }

        @Override
        public Optional<List<String>> disabled() {
            return Optional.ofNullable(CommonCodec.LIST_$$STRING.get(this.asJson, "disabled"));
        }


        @Override
        public Optional<List<String>> manual() {
            return Optional.ofNullable(CommonCodec.LIST_$$STRING.get(this.asJson, "manual"));
        }


        @Override
        public Optional<JsonObject> retriever() {
            return Optional.ofNullable(Codec.JSON_OBJECT.get(this.asJson, "retriever"));
        }


        public static Data from(ApplicationConfiguration t) {
            return t == null ? null : t instanceof Data u ? u : new Data(t.asJson());
        }

        public Data copy() {
            return new Data(toJson());
        }

        @Override
        public Optional<JsonObject> get(String activities) {
            return Optional.ofNullable(asJson.getJsonObject(activities));
        }
    }

}

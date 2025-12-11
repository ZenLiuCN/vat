package vat.api;

import io.vertx.core.json.JsonObject;
import vat.api.meta.*;
import vat.api.trait.Applicative;
import vat.api.utils.Buf;
import vat.api.utils.Fn;

import java.time.Instant;

/**
 * an entity data which can stored.
 *
 * @author Zen.Liu
 * @since 2025-10-20
 */
@Prototype
public interface Entity extends Data {

    interface Base extends Entity, Entry {
        @Describe(value = "_IDENTITY", desc = "_DESC_IDENTITY")
        @Identity
        Long id();

        /// opt-lock field
        @Describe(value = "_VERSION", desc = "_DESC_VERSION")
        @OptimisticLock
        int version();

        /// soft removed field
        @Describe(value = "_REMOVED", desc = "_DESC_REMOVED")
        @SoftRemoved
        boolean removed();

        /// store creator, 0 if missing
        @Describe(value = "_CREATOR", desc = "_DESC_CREATOR", identity = "vat.foundation.users.api.Users::identity")
        @Audit.Creator
        long creator();

        @Describe(value = "_CREATED_AT", desc = "_DESC_CREATED_AT")
        @Audit.Created
        Instant createdAt();

        /// store last modifier, 0 if missing
        @Describe(value = "_MODIFIER", desc = "_DESC_MODIFIER", identity = "vat.foundation.users.api.Users::identity")
        @Audit.Modifier
        long modifier();

        @Describe(value = "_MODIFIED_AT", desc = "_DESC_MODIFIED_AT")
        @Audit.Modified
        Instant modifiedAt();
    }


    interface Entry extends Data {
        static Entry of(Long id, int version) {
            return new Entry.entry(id, version);
        }

        record entry(Long id, int version) implements Entry, Data.Binary, Applicative<entry> {
            public entry(JsonObject v) {
                this(v.getLong("id"), v.getInteger("version"));
            }

            public entry(JsonObject v, Void ignore) {
                this(Fn.parseNullable(v.getString("id"), Long::parseLong), v.getInteger("version"));
            }

            public entry(Buf v) {
                this(v.object(Buf::i64), v.i32());
            }

            @Override
            public JsonObject toJson() {
                return JsonObject.of(
                        "id", id,
                        "version", version
                );
            }

            @Override
            public JsonObject asJson() {
                return toJson();
            }

            @Override
            public entry _this() {
                return this;
            }

            @Override
            public Buf toBuf(Buf buf) {
                return buf.object(id, Buf.I64).i32(version);
            }
        }

        Long id();

        /// opt-lock field
        int version();
    }
}

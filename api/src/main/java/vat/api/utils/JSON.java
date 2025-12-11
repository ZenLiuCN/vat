package vat.api.utils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.jetbrains.annotations.NotNull;
import vat.api.DomainError;
import vat.api.meta.Nullable;
import vat.api.store.Field;
import vat.api.store.Statement;
import vat.api.store.Value;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;

///
/// @author Zen.Liu
/// @since 2025-11-19


public interface JSON {
    Base64.Encoder BASE64_ENCODER = Base64.getUrlEncoder().withoutPadding();
    Base64.Decoder BASE64_DECODER = Base64.getUrlDecoder();

    /// parse a JsonArray to json path list which contains string and list
    static List<Object> jsonPath(JsonArray array) {
        var out = new ArrayList<>(array.size());
        for (var i = 0; i < array.size(); i++) {
            var v = array.getValue(i);
            if (v instanceof String s) out.add(s);
            else if (v instanceof Number n) out.add(n.intValue());
            else throw DomainError.System.badRequest("json path should only contains string and integer: {}", v);
        }
        return out;
    }

    static Value.JsonObjectValue jsonObjectPathRead(JsonArray p, Value.JsonObjectValue profile) {
        if (p.size() == 1)
            if (p.getValue(0) instanceof String s) return profile.objectAt(s);
            else throw DomainError.System.badRequest("one path key must a string");
        Value.JsonValue<?> r = null;
        for (var i = 0; i < p.size(); i++) {
            var k = p.getValue(i);
            if (i == 0) {
                var next = p.getValue(i + 1);
                r = switch (k) {
                    case String s when next instanceof Number -> profile.arrayAt(s);
                    case String s when next instanceof String -> profile.objectAt(s);
                    default -> throw DomainError.System.badRequest("first path invalid");
                };
            } else if (i < p.size() - 1) {
                var next = p.getValue(i + 1);
                var lr = r;
                r = switch (k) {
                    case String s when next instanceof Number && lr instanceof Value.JsonObjectValue j -> j.arrayAt(s);
                    case String s when next instanceof String && lr instanceof Value.JsonObjectValue j -> j.objectAt(s);
                    case Number s when next instanceof Number && lr instanceof Value.JsonArrayValue j ->
                            j.arrayAt(s.intValue());
                    case Number s when next instanceof String && lr instanceof Value.JsonArrayValue j ->
                            j.objectAt(s.intValue());
                    default -> throw DomainError.System.badRequest("first path invalid");
                };
            } else {
                var lr = r;
                r = switch (k) {
                    case String s when lr instanceof Value.JsonObjectValue j -> j.objectAt(s);
                    case Number s when lr instanceof Value.JsonArrayValue j -> j.objectAt(s.intValue());
                    default -> throw DomainError.System.badRequest("last path invalid");
                };
            }
        }
        return (Value.JsonObjectValue) r;
    }

    /// @param p when path is null or empty set full field as value
    static Statement.SetStmt jsonObjectPathWrite(@Nullable JsonArray p, @NotNull JsonObject value, Field.JsonObjectField profile) {
        if (p == null || p.isEmpty()) return profile.set(value);
        var path = jsonPath(p);
        if (!(path.getFirst() instanceof String s))
            throw DomainError.System.badRequest("json object path should have string for first : {}", path.getFirst());
        if (path.size() == 1) return profile.setAt(value, s);
        return profile.setAt(value, s, path.stream().skip(1).toArray());
    }

    interface Functor {
        static Function<JsonArray, Value.@Nullable JsonObjectValue> jsonObjectPathRead(Value.JsonObjectValue profile) {
            return j -> j == null ? null : JSON.jsonObjectPathRead(j, profile);
        }
    }

}

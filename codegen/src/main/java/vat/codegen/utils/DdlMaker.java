package vat.codegen.utils;

import com.palantir.javapoet.TypeName;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.data.Numeric;
import vat.api.utils.ITimes;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

///
/// @author Zen.Liu
/// @since 2025-11-12


public interface DdlMaker {


    record TypeDefine(String mysql, String pg, boolean numeric, boolean precision, boolean len) {
        TypeDefine(String mysql, String pg, boolean numeric) {
            this(mysql, pg, numeric, false, false);
        }

        TypeDefine(String mysql, String pg, boolean len, Void ignore) {
            this(mysql, pg, false, false, len);
        }

        TypeDefine(String mysql, String pg, boolean precision, Void ignore, Void ignore2) {
            this(mysql, pg, precision, precision, false);
        }

        TypeDefine(String mysql, String pg) {
            this(mysql, pg, false, false, false);
        }

        TypeDefine format(int value) {
            assert len;
            return new TypeDefine(mysql.formatted(value), pg.formatted(value), numeric, false, false);
        }

        TypeDefine formatPrecision(int precision, int scale) {
            assert this.precision;
            return new TypeDefine(mysql.formatted(precision, scale), pg.formatted(precision, scale), numeric, false, false);
        }

        TypeDefine format(ColumnInfo x) {
            assert len : "format should only with length type";
            var v = x.size() > 0 ? x.size() : x.max();
            assert v > 0 : "invalid column size: " + x;
            return format(v);
        }

        TypeDefine format(ColumnInfo x, int def) {
            assert def > 0 : "default length must positive";
            assert len : "format should only with length type";
            var v = x.size() > 0 ? x.size() : x.max() > 0 ? x.max() : def;
            return format(v);
        }

        TypeDefine formatPrecision(ColumnInfo x, int precision, int scale) {
            assert this.precision;
            var p = x.precision() > 0 ? x.precision() : precision;
            var s = x.scale() >= 0 ? x.scale() : scale;
            assert p > 0 && s >= 0 : "invalid precision and scale: (" + p + "," + s + "): " + x;
            return formatPrecision(p, s);
        }

        TypeDefine formatPrecision(ColumnInfo x) {
            assert this.precision;
            var p = x.precision() > 0 ? x.precision() : -1;
            var s = x.scale() >= 0 ? x.scale() : -1;
            assert p > 0 && s >= 0 : "invalid precision and scale: (" + p + "," + s + "): " + x;
            return formatPrecision(p, s);
        }
    }

    Map<TypeName, TypeDefine> dbTypes = new ConcurrentHashMap<>();


    TypeDefine BOOLEAN = new TypeDefine("BOOLEAN", "BOOLEAN");
    TypeDefine BYTE = new TypeDefine("TINYINT", "\"CHAR\"", true);
    TypeDefine SHORT = new TypeDefine("SMALLINT", "INT2", true);
    TypeDefine INT = new TypeDefine("INT", "INT4", true);
    TypeDefine LONG = new TypeDefine("BIGINT", "INT8", true);
    TypeDefine FLOAT = new TypeDefine("FLOAT", "FLOAT4", true);
    TypeDefine DOUBLE = new TypeDefine("DOUBLE", "FLOAT8", true);

    TypeDefine STRING = new TypeDefine("VARCHAR(%d)", "VARCHAR(%d)", true, null);
    TypeDefine TEXT = new TypeDefine("TEXT", "TEXT", false, null);
    TypeDefine MID_TEXT = new TypeDefine("MEDIUMTEXT", "TEXT", false, null);
    TypeDefine LONG_TEXT = new TypeDefine("LONGTEXT", "TEXT", false, null);
    TypeDefine INSTANT = new TypeDefine("TIMESTAMP", "TIMESTAMP");
    TypeDefine JSON = new TypeDefine("JSON", "JSONB");
    TypeDefine BUFFER = new TypeDefine("TINYBLOB(%d)", "BYTEA", true, null);
    TypeDefine BLOB = new TypeDefine("BLOB(%d)", "BYTEA", true, null);
    TypeDefine MED_BLOB = new TypeDefine("MEDIUMBLOB(%d)", "BYTEA", true, null);
    TypeDefine LONG_BLOB = new TypeDefine("LONGBLOB(%d)", "BYTEA", true, null);

//    TypeDefine TEXT = new TypeDefine("TEXT", "TEXT");
//    TypeDefine CHAR = new TypeDefine("CHAR(%d)", "CHAR(%d)", true, null);

    TypeDefine DECIMAL = new TypeDefine("DECIMAL(%d,%d)", "DECIMAL(%d,%d)", true, null, null);
    TypeDefine NUMERIC = new TypeDefine("NUMERIC(%d,%d)", "NUMERIC(%d,%d)", true, null, null);
    TypeDefine DATE_TIME_TZ = new TypeDefine("TIMESTAMP", "TIMESTAMPTZ");
    TypeDefine DATE_TIME = new TypeDefine("DATETIME", "TIMESTAMP");
    TypeDefine DATE = new TypeDefine("DATE", "DATE");
    TypeDefine TIME = new TypeDefine("TIME", "TIME");
    TypeDefine TIME_TZ = new TypeDefine("TIME", "TIMETZ");
    TypeDefine UUID = new TypeDefine("CHAR(36)", "UUID");


    TypeName T_boolean = TypeName.get(boolean.class);
    TypeName T_Boolean = TypeName.get(Boolean.class);
    TypeName T_byte = TypeName.get(byte.class);
    TypeName T_Byte = TypeName.get(Byte.class);
    TypeName T_short = TypeName.get(short.class);
    TypeName T_Short = TypeName.get(Short.class);
    TypeName T_int = TypeName.get(int.class);
    TypeName T_Integer = TypeName.get(Integer.class);
    TypeName T_char = TypeName.get(char.class);
    TypeName T_Character = TypeName.get(Character.class);
    TypeName T_long = TypeName.get(long.class);
    TypeName T_Long = TypeName.get(Long.class);
    TypeName T_float = TypeName.get(float.class);
    TypeName T_Float = TypeName.get(Float.class);
    TypeName T_double = TypeName.get(double.class);
    TypeName T_Double = TypeName.get(Double.class);
    TypeName T_String = TypeName.get(String.class);
    TypeName T_bytes = TypeName.get(byte[].class);
    TypeName T_Buffer = TypeName.get(Buffer.class);
    TypeName T_JsonObject = TypeName.get(JsonObject.class);
    TypeName T_JsonArray = TypeName.get(JsonArray.class);
    TypeName T_Instant = TypeName.get(Instant.class);
    TypeName T_LocalDate = TypeName.get(LocalDate.class);
    TypeName T_LocalTime = TypeName.get(LocalTime.class);
    TypeName T_LocalDateTime = TypeName.get(LocalDateTime.class);
    TypeName T_OffsetDateTime = TypeName.get(OffsetDateTime.class);
    TypeName T_OffsetTime = TypeName.get(OffsetTime.class);
    TypeName T_BigDecimal = TypeName.get(BigDecimal.class);
    TypeName T_Numeric = TypeName.get(Numeric.class);
    TypeName T_Duration = TypeName.get(Duration.class);
    TypeName T_Period = TypeName.get(Period.class);
    TypeName T_Class = TypeName.get(Class.class);
    TypeName T_UUID = TypeName.get(UUID.class);
    TypeName T_I_TIME = TypeName.get(ITimes.ITime.class);
    TypeName T_I_DATE = TypeName.get(ITimes.IDate.class);
    TypeName T_I_DATETIME = TypeName.get(ITimes.IDatetime.class);
    int MAX_MID_TEXT = 1 << 24;
    int MAX_TEXT = 1 << 16;


    static TypeDefine dbType(ColumnInfo x, boolean mySQL) {
        if (dbTypes.isEmpty()) {
            synchronized (dbTypes) {
                if (dbTypes.isEmpty()) {
                    dbTypes.put(T_boolean, BOOLEAN);
                    dbTypes.put(T_Boolean, BOOLEAN);
                    dbTypes.put(T_byte, BYTE);
                    dbTypes.put(T_Byte, BYTE);
                    dbTypes.put(T_short, SHORT);
                    dbTypes.put(T_Short, SHORT);
                    dbTypes.put(T_int, INT);
                    dbTypes.put(T_Integer, INT);
                    dbTypes.put(T_char, INT);
                    dbTypes.put(T_Character, INT);
                    dbTypes.put(T_long, LONG);
                    dbTypes.put(T_Long, LONG);
                    dbTypes.put(T_float, LONG);
                    dbTypes.put(T_Float, LONG);
                    dbTypes.put(T_double, LONG);
                    dbTypes.put(T_Double, LONG);

                    dbTypes.put(T_String, STRING);
                    dbTypes.put(T_bytes, BUFFER);
                    dbTypes.put(T_Buffer, BUFFER);

                    dbTypes.put(T_JsonObject, JSON);
                    dbTypes.put(T_JsonArray, JSON);
                    dbTypes.put(T_Instant, INSTANT);
                    dbTypes.put(T_LocalDate, DATE);
                    dbTypes.put(T_LocalTime, TIME);
                    dbTypes.put(T_LocalDateTime, DATE_TIME);
                    dbTypes.put(T_OffsetDateTime, DATE_TIME_TZ);
                    dbTypes.put(T_OffsetTime, TIME_TZ);

                    dbTypes.put(T_BigDecimal, DECIMAL);
                    dbTypes.put(T_Numeric, NUMERIC);

                    dbTypes.put(T_Duration, STRING);
                    dbTypes.put(T_Period, STRING);
                    dbTypes.put(T_Class, STRING);
                    dbTypes.put(T_UUID, UUID);


                    dbTypes.put(T_I_DATE, INT);
                    dbTypes.put(T_I_TIME, INT);
                    dbTypes.put(T_I_DATETIME, LONG);
                }
            }
        }
        if (x.field().ctx().rawSameType(x.type(), Class.class)) {
            return STRING.format(x, 255);
        }
        var t = TypeName.get(x.type());
        var p = dbTypes.get(t);
        if (p != null) {
            if (p.len) {
                if (t.equals(T_String)) {
                    var size = x.size() > 0 ? x.size() : x.max() > 0 ? x.max() : -1;
                    var idx = x.hasAnyIndex();
                    if (mySQL) {
                        if (idx && size < 0)
                            throw new IllegalStateException("length required for string " + x.identityString());
                        if (size < 256) return STRING.format(size);
                        if (size < MAX_TEXT) return TEXT;
                        if (size < MAX_MID_TEXT) return MID_TEXT;
                        return LONG_TEXT;
                    } else {
                        return TEXT;
                    }
                }
                if (t.equals(T_bytes) || t.equals(T_Buffer)) {
                    var size = x.size() > 0 ? x.size() : x.max() > 0 ? x.max() : -1;
                    var idx = x.hasAnyIndex();
                    if (mySQL) {
                        if (idx && size < 0)
                            throw new IllegalStateException("length required for " + x.identityString());
                        if (size < 256) return BUFFER.format(size);
                        if (size < MAX_TEXT) return BLOB.format(size);
                        if (size < MAX_MID_TEXT) return MED_BLOB.format(size);
                        return LONG_BLOB.format(size);
                    } else {
                        return BLOB;
                    }
                }

                if (t.equals(T_Class)) return p.format(x, 255);
                if (t.equals(T_Duration)) return p.format(x, 255);
                if (t.equals(T_Period)) return p.format(x, 255);
                if (x.size() <= 0 && x.max() <= 0) throw new IllegalStateException("length required for " + x);
                return p.format(x);
            }
            if (p.precision) {
                if (x.precision() <= 0 || x.scale() < 0)
                    throw new IllegalStateException("precision and scale required for " + x);
                return p.formatPrecision(x);
            }
            return p;
        }
        if (x.enumType()) {
            if (x.enumName()) return STRING.format(x, 255);
            return INT;
        }
        throw new IllegalStateException("unsupported DDL type: " + x);
    }

    record ColumnIndexInfo(
            /// single column index marker
            boolean index,
            /// single column unique marker
            boolean unique
    ) {
    }

    static ColumnIndexInfo info(ColumnInfo x, HashMap<ColumnInfo, Set<String>> indexes, HashMap<ColumnInfo, Set<String>> uniques) {
        var idx = x.indexed() != null && (x.indexed().size() == 1 && x.indexed().contains(x.name()));
        var uniq = x.unique() != null && (x.unique().size() == 1 && x.unique().contains(x.name()));
        if (idx && uniq)
            throw new IllegalStateException("can't have both index and unique constraint:" + x);
        if (x.indexed() != null) {
            indexes.put(x, new HashSet<>(x.indexed()));
        }
        if (x.unique() != null) {
            uniques.put(x, new HashSet<>(x.unique()));
        }
        return new ColumnIndexInfo(idx, uniq);
    }

    static void ddlMySQL(Builders.Storage store) {
        if (!Configuration.DDL_MYSQL.get().orElse(false)) return;
        var b = store.data().domain().getDDL(DDL.MYSQL);
        var table = store.store().table();
        var indexes = new HashMap<ColumnInfo, Set<String>>();
        var uniques = new HashMap<ColumnInfo, Set<String>>();
        var columns = store.store().columns().stream()
                .sorted(Comparator.comparingInt(x -> x.field().index()))
                .map(x -> {
                    var type = dbType(x, true);
                    var info = info(x, indexes, uniques);
                    return "\t" + (switch (x.kind()) {
                        case IDENTITY -> "`%s` %s %s PRIMARY KEY".formatted(
                                x.name(),
                                type.mysql,
                                type.numeric ? "AUTO_INCREMENT" : ""
                        );
                        case REMOVED -> "`%s` BOOLEAN NOT NULL DEFAULT FALSE".formatted(x.name());
                        case VERSION -> "`%s` INT NOT NULL DEFAULT 0".formatted(x.name());
                        case CREATOR, MODIFIER -> "`%s` %s %s".formatted(
                                x.name(),
                                type.mysql,
                                type.numeric ? "NOT NULL DEFAULT -1" : ""
                        );
                        case CREATED -> "`%s` %s %s".formatted(
                                x.name(),
                                type.mysql,
                                type.numeric ? "NOT NULL DEFAULT -1" : "NOT NULL DEFAULT CURRENT_TIMESTAMP"
                        );
                        case MODIFIED -> "`%s` %s %s".formatted(
                                x.name(),
                                type.mysql,
                                type.numeric ? "NOT NULL DEFAULT -1" : "NOT NULL DEFAULT CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP"
                        );
                        case HISTORY -> "`%s` %s".formatted(
                                x.name(),
                                type.mysql
                        );
                        case NORMAL -> "`%s` %s %s".formatted(
                                x.name(),
                                type.mysql,
                                x.nullable() ? "" : "NOT NULL"
                        );

                    }).trim() + (info.unique ? " UNIQUE" : "");
                }).collect(Collectors.joining(",\n"));
        b.append("CREATE TABLE IF NOT EXISTS `").append(table).append("` (\n")
                .append(columns);
        if (!indexes.isEmpty()) {
            var indexGroups = new HashMap<String, Set<String>>();
            indexes.forEach((col, indexFields) -> {
                var old = indexGroups.put("idx_" + table + "-" + String.join("_", indexFields), indexFields);
                if (old != null) throw new IllegalStateException("conflict index: " + col);
            });
            b.append(",\n").append(indexGroups
                    .entrySet()
                    .stream()
                    .map(e -> "\tINDEX `%s` (%s)".formatted(e.getKey(),
                            e.getValue().stream().map("`%s`"::formatted).collect(Collectors.joining(","))))
                    .collect(Collectors.joining(",\n")));
        }
        if (uniques.values().stream().anyMatch(i -> i.size() > 1)) {
            var uniqueGroups = new HashMap<String, Set<String>>();
            uniques.forEach((col, uniqueFields) -> {
                if (uniqueFields.size() == 1) return;//! ignore one unique for already processed
                var old = uniqueGroups.put("udx_" + table + "-" + String.join("_", uniqueFields), new HashSet<>(uniqueFields));
                if (old != null) throw new IllegalStateException("conflict unique: " + col);
            });
            b.append(",\n")
                    .append(uniqueGroups
                            .entrySet()
                            .stream()
                            .map(e -> "\tCONSTRAINT `%s` UNIQUE (%s)".formatted(e.getKey(),
                                    e.getValue().stream().map("`%s`"::formatted).collect(Collectors.joining(", "))))
                            .collect(Collectors.joining(",\n")));
        }
        b.append("\n);\n");
    }


    static void ddlPG(Builders.Storage store) {
        if (!Configuration.DDL_POSTGRES.get().orElse(false)) return;
        var b = store.data().domain().getDDL(DDL.POSTGRES);
        var table = store.store().table();
        var indexes = new HashMap<ColumnInfo, Set<String>>();
        var uniques = new HashMap<ColumnInfo, Set<String>>();
        var columns = store.store().columns().stream()
                .sorted(Comparator.comparingInt(x -> x.field().index()))
                .map(x -> {
                    var type = dbType(x, false);
                    var info = info(x, indexes, uniques);
                    return "\t" + (switch (x.kind()) {
                        case IDENTITY -> "\"%s\" %s %s PRIMARY KEY".formatted(
                                x.name(),
                                type.pg,
                                type.numeric ? "GENERATED BY DEFAULT AS IDENTITY" : ""
                        );
                        case REMOVED -> "\"%s\" BOOLEAN NOT NULL DEFAULT FALSE".formatted(x.name());
                        case VERSION -> "\"%s\" INT4 NOT NULL DEFAULT 0".formatted(x.name());
                        case CREATOR, MODIFIER -> "\"%s\" %s %s".formatted(
                                x.name(),
                                type.pg,
                                type.numeric ? "NOT NULL DEFAULT -1" : ""
                        );
                        case CREATED, MODIFIED -> "\"%s\" %s %s".formatted(
                                x.name(),
                                type.pg,
                                type.numeric ? "NOT NULL DEFAULT -1" : "NOT NULL DEFAULT CURRENT_TIMESTAMP"
                        );
                        case HISTORY -> "\"%s\" %s".formatted(
                                x.name(),
                                type.pg
                        );
                        case NORMAL -> "\"%s\" %s %s".formatted(
                                x.name(),
                                type.pg,
                                x.nullable() ? "" : "NOT NULL"
                        );

                    }).trim() + (info.unique ? " UNIQUE " : "");
                }).collect(Collectors.joining(",\n"));
        b.append("CREATE TABLE IF NOT EXISTS \"").append(table).append("\" (\n")
                .append(columns)
                .append(");\n");
        if (!indexes.isEmpty()) {
            var indexGroups = new HashMap<String, Set<String>>();
            indexes.forEach((col, indexFields) -> {
                var old = indexGroups.put("idx_" + table + "-" + String.join("_", indexFields), indexFields);
                if (old != null) throw new IllegalStateException("conflict index: " + col);
            });
            b.append(indexGroups
                            .entrySet()
                            .stream()
                            .map(e -> "CREATE INDEX \"%s\" ON \"%s\" (%s);".formatted(
                                    e.getKey(),
                                    table,
                                    e.getValue().stream().map("\"%s\""::formatted).collect(Collectors.joining(", "))))
                            .collect(Collectors.joining("\n")))
                    .append("\n");
        }
        if (uniques.values().stream().mapToInt(i -> i.size() > 1 ? 1 : 0).sum() > 0) {
            var uniqueGroups = new HashMap<String, Set<String>>();
            uniques.forEach((col, uniqueFields) -> {
                if (uniqueFields.size() == 1) return;//! ignore one unique.
                var old = uniqueGroups.put("udx_" + table + "-" + String.join("_", uniqueFields), uniqueFields);
                if (old != null) throw new IllegalStateException("conflict index: " + col);
            });
            b.append(uniqueGroups
                            .entrySet()
                            .stream()
                            .map(e -> "ALTER TABLE \"%s\" ADD CONSTRAINT \"%s\" UNIQUE (%s);".formatted(
                                    table,
                                    e.getKey(),
                                    e.getValue().stream().map("\"%s\""::formatted)
                                            .collect(Collectors.joining(", "))))
                            .collect(Collectors.joining("\n")))
                    .append("\n");
        }
    }
}

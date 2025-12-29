package vat.api.store;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author Zen.Liu
 * @since 2025-10-22
 */
public interface Renderer {


    void aggregated(Writer w, Field.AggregatedField ag);

    void virtualField(Writer w, Field<?> field);

    void field(Writer w, Field<?> field);


    void set(Writer w, Field<?> left, @Nullable Object value);


    void jsonRemove(Writer w, Field<?> left, Object[] path);

    void jsonMerge(Writer w, Field<?> left, Object[] path, Object value);

    void jsonSet(Writer w, Field<?> left, Object[] path, Object value);

    boolean registerPlaceHolder(Writer w, String name, Class<?> type);

    void model(Writer w, Model<?> model);


    void jsonPath(Writer w, Value.JsonValue<?> root, List<JsonGet.Path> path);


    void logicalAnd(Writer w, Value<?> left, Object right);

    void logicalNot(Writer w, Value<?> left);

    void logicalOr(Writer w, Value<?> left, Object right);

    void mathPlus(Writer w, Value<?> left, Object right, int bits);

    void mathDivide(Writer w, Value<?> left, Object right, int bits);

    void mathTimes(Writer w, Value<?> left, Object right, int bits);

    void mathMinus(Writer w, Value<?> left, Object right, int bits);

    void mathReminder(Writer w, Value<?> left, Object right, int bits);

    void mathNegative(Writer w, Value<?> left, int bits);

    void innerJoin(Writer w, Model<?> model, Value.BooleanValue cond);

    void leftOuterJoin(Writer w, Model<?> model, Value.BooleanValue cond);

    void rightOuterJoin(Writer w, Model<?> model, Value.BooleanValue cond);

    void order(Writer w, Field<?> field, boolean desc);

    void equal(Writer w, Value<?> left, Object right);

    void noneEqual(Writer w, Value<?> left, Object right);

    void isNull(Writer w, Value<?> left);

    void isNotNull(Writer w, Value<?> left);

    void isTrue(Writer w, Value<?> left);

    void isFalse(Writer w, Value<?> left);

    void greater(Writer w, Value<?> left, Object right);

    void greaterOrEqual(Writer w, Value<?> left, Object right);

    void lesser(Writer w, Value<?> left, Object right);

    void lesserOrEqual(Writer w, Value<?> left, Object right);

    void in(Writer w, Value<?> left, Object[] right);

    void notIn(Writer w, Value<?> left, Object[] right);

    void between(Writer w, Value<?> left, Object l, Object h);

    void contains(Writer w, Value<?> left, Object right, boolean caseInsensitive);

    void startsWith(Writer w, Value<?> left, Object right, boolean caseInsensitive);

    void endsWith(Writer w, Value<?> left, Object right, boolean caseInsensitive);

    void equalCaseInsensitive(Writer w, Value<?> left, Object right);

    void notEqualCaseInsensitive(Writer w, Value<?> left, Object right);

    void bitwiseAnd(Writer w, Value<?> left, Object right, int bits);

    void bitwiseNot(Writer w, Value<?> left, int bits);

    void bitwiseXor(Writer w, Value<?> left, Object right, int bits);

    void bitwiseOr(Writer w, Value<?> left, Object right, int bits);

    void bitwiseShiftLeft(Writer w, Value<?> left, Object right, int bits);

    void bitwiseShiftRight(Writer w, Value<?> left, Object right, int bits);

    void assign(Writer w, Field<?> left, @Nullable Object value);

    void rawValue(Writer w, RawValue<?> tRawValue);

    /// render a  virtual history field
    void virtualHistory(Writer w, Field<?> raw);


    enum Stage {
        SELECT,
        COUNTING,
        FROM,
        WHERE,
        JOIN,
        ON,
        GROUP_BY,
        HAVING,
        ORDER_BY,
        LIMITS,

        INSERT,
        COLUMNS,
        VALUES,

        UPDATE,
        SET,
        DELETE,
        RETURNING,

    }

    @Nullable Stage stage();

}

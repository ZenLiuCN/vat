package vat.api.store;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.SqlClient;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.jspecify.annotations.Nullable;
import vat.api.DomainError;
import vat.api.Store;
import vat.api.implement.Stored;
import vat.api.utils.Fn;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Zen.Liu
 * @since 2025-10-21
 */
public interface Model<T> extends Renderable {
    @Nullable  String _schema();

    String _name();

    String _alias();

    Function<JsonObject, T> _create();

    /// fluent returns current model
    Model<T> _alias(String alias);

    Field<?> _identity();

    boolean _optLock();

    boolean _softRemovable();

    boolean _historic();

    boolean _auditable();

    <F extends Field<?>> @Nullable F _traits(TraitField kind);


    Field<?>[] _fields();

    @Override
    default void _render(Renderer renderer, Writer w) {
        w.expr(this);
        renderer.model(w, this);
    }

    @Accessors(fluent = true)
    abstract class Base<ID, T, S extends Base<ID, T, S>> implements Model<T>, Store<T>, Stored<ID, T, S> {
        //region  base
        @Getter
        @Nullable public final String _schema;
        @Getter
        public final String _name;
        public final int identity;
        public final int version;
        public final int removed;
        public final int creator;
        public final int createdAt;
        public final int modifier;
        public final int modifiedAt;
        public final int history;
        @Getter
        public final Function<JsonObject, T> _create;
        @Getter
        public final Field<?>[] _fields;
        @Getter
        @Nullable  public String _alias;

        @Getter
        public final boolean _auditable;
        @Getter
        public final boolean _softRemovable;
        @Getter
        public final boolean _optLock;
        @Getter
        public final boolean _historic;


        protected Base(
                @Nullable String schema, String name,
                Function<JsonObject, T> create,
                int identity,
                int version,
                int removed,
                int creator,
                int createdAt,
                int modifier,
                int modifiedAt,
                int history
        ) {
            this._schema = schema;
            this._name = name;
            this._create = create;
            assert identity >= 0;
            this.identity = identity;
            _fields = buildFields();
            this.version = version < 0 || version > _fields.length ? -1 : version;
            this.removed = removed < 0 || removed > _fields.length ? -1 : removed;
            this.creator = creator < 0 || creator > _fields.length ? -1 : creator;
            this.createdAt = createdAt < 0 || createdAt > _fields.length ? -1 : createdAt;
            this.modifier = modifier < 0 || modifier > _fields.length ? -1 : modifier;
            this.modifiedAt = modifiedAt < 0 || modifiedAt > _fields.length ? -1 : modifiedAt;
            this.history = history < 0 || history > _fields.length ? -1 : history;
            this._historic = this.history > 0;
            this._optLock = this.version > 0;
            this._softRemovable = this.removed > 0;
            this._auditable = this.creator > 0 && this.createdAt > 0 && this.modifier > 0 && this.modifiedAt > 0;
        }

        protected abstract Field<?>[] buildFields();

        protected abstract S copy(@Nullable String schema);

        public Model<T> _alias(String alias) {
            assert this._alias == null;
            this._alias = alias;
            return this;
        }


        @SuppressWarnings("unchecked")
        @Override
        public Field<ID> _identity() {
            var f = (Field<ID>) _fields[identity];
            assert f instanceof Value.ComparableValue : "identity must comparable";
            return f;
        }

        @SuppressWarnings("unchecked")
        protected <F extends Field<?>> F field(int index) {
            return (F) _fields[index];
        }

        @Override
        public <F extends Field<?>> @Nullable F _traits(TraitField kind) {
            return switch (kind) {
                case VERSION -> version < 0 ? null : field(version);
                case REMOVED -> removed < 0 ? null : field(removed);
                case CREATOR -> creator < 0 ? null : field(creator);
                case CREATE_AT -> createdAt < 0 ? null : field(createdAt);
                case MODIFIER -> modifier < 0 ? null : field(modifier);
                case MODIFIED_AT -> modifiedAt < 0 ? null : field(modifiedAt);
                case HISTORY -> history < 0 ? null : field(history);
            };
        }

        protected abstract S _self();

        @Nullable  protected SqlClient _sql;
        @Nullable  protected Dialect _dialect;

        public Stages.Store<T> store() {
            assert _sql != null;
            assert _dialect != null;
            var state = new Status(new State(_self(), _sql, _dialect));
            return () -> state;
        }

        protected <R> Future<R> transaction(Function<Stages.Store<T>, Future<R>> act) {
            return _sql instanceof Pool p ? p.withTransaction(tx -> {
                assert _dialect != null;
                var state = new Status(new State(_self(), tx, _dialect));
                Stages.Store<T> s = () -> state;
                return act.apply(s);
            }) : act.apply(store());
        }

        public S _with(SqlClient sql, Dialect dialect) {
            this._sql = sql;
            this._dialect = dialect;
            return _self();
        }

        //endregion

        //region extended singular


        public Future<Void> remove(@Nullable ID actor, ID id, int version) {
            return transaction(s -> s.withActor(actor)
                    .filter(_identity().eq(id).and(Objects.requireNonNull(this.<Field.IntegerField>_traits(TraitField.VERSION)).eq(version)))
                    .remove(false)
                    .map(Fn.equal(1))
                    .mapEmpty());
        }

        public Future<Void> remove(@Nullable ID actor, ID id) {
            return transaction(s -> s.withActor(actor)
                    .filter(_identity().eq(id))
                    .remove(false)
                    .map(Fn.equal(1))
                    .mapEmpty());
        }

        public Future<Integer> removeAny(@Nullable ID actor, Function<S, Value.BooleanValue> cond) {
            return transaction(s -> s.withActor(actor)
                    .filter(cond.apply(_self()))
                    .remove(false));
        }

        public Future<Void> removePermanent(@Nullable ID actor, ID id, int version) {
            return transaction(s -> s.withActor(actor)
                    .filter(_identity().eq(id).and(Objects.requireNonNull(this.<Field.IntegerField>_traits(TraitField.VERSION)).eq(version)))
                    .remove(true)
                    .map(Fn.equal(1))
                    .mapEmpty());
        }

        public Future<Void> removePermanent(@Nullable ID actor, ID id) {
            return transaction(s -> s.withActor(actor)
                    .filter(_identity().eq(id))
                    .remove(true)
                    .map(Fn.equal(1))
                    .mapEmpty());
        }

        public Future<Integer> removeAnyPermanent(@Nullable ID actor, Function<S, Value.BooleanValue> cond) {
            return transaction(s -> s.withActor(actor)
                    .filter(cond.apply(_self()))
                    .remove(true));
        }

        public Future<Void> justPut(ID actor, Function<S, Collection<StmtAssign>> set) {
            return store().withActor(actor).justPut(set.apply(_self()).toArray(StmtAssign[]::new)).map(Fn::isTrue).mapEmpty();
        }

        public Future<Void> justPut(@Nullable ID actor, JsonObject set) {
            var assigns = assign(set);
            return store().withActor(actor)
                    .justPut(assigns)
                    .map(Fn::isTrue)
                    .mapEmpty();
        }

        public Future<ID> putGetIdentity(@Nullable ID actor, Function<S, Collection<StmtAssign>> set) {
            return store()
                    .withActor(actor)
                    .<ID>put(set.apply(_self()).toArray(StmtAssign[]::new))
                    ;
        }

        public Future<ID> putGetIdentity(@Nullable ID actor, JsonObject set) {
            var assigns = assign(set);
            return store().withActor(actor)
                    .put(assigns)
                    ;
        }

        public Future<T> put(@Nullable ID actor, JsonObject set) {
            var assigns = assign(set);
            return store().withActor(actor)
                    .<ID>put(assigns)
                    .flatMap(this::identity)
                    ;
        }

        public Future<T> put(@Nullable ID actor, Function<S, Collection<StmtAssign>> set) {
            return store()
                    .withActor(actor)
                    .<ID>put(set.apply(_self()).toArray(StmtAssign[]::new))
                    .flatMap(this::identity);
        }

        public Future<Void> justPutMany(@Nullable ID actor, Function<S, Collection<? extends Collection<StmtAssign>>> set) {
            var assigns = set.apply(_self()).stream().map(x -> x.toArray(StmtAssign[]::new)).toArray(StmtAssign[][]::new);
            return store().withActor(actor)
                    .justPutMany(assigns)
                    .map(Fn.equal(assigns.length))
                    .mapEmpty();
        }

        public Future<Void> justPutMany(@Nullable ID actor, Collection<JsonObject> set) {
            var assigns = set.stream().map(this::assign).toArray(StmtAssign[][]::new);
            return store().withActor(actor)
                    .justPutMany(assigns)
                    .map(Fn.equal(assigns.length))
                    .mapEmpty();
        }

        public Future<List<T>> putMany(@Nullable ID actor, Collection<JsonObject> set) {
            var assigns = set.stream().map(this::assign).toArray(StmtAssign[][]::new);
            return store().withActor(actor)
                    .<ID>putMany(assigns)
                    .map(Fn.lengthEqual(assigns.length, List::size, n -> DomainError.System.notAcceptable("require {} got {}", assigns.length, n)))
                    .flatMap(this::identities);
        }

        public Future<List<T>> putMany(@Nullable ID actor, Function<S, Collection<? extends Collection<StmtAssign>>> set) {
            var assigns = set.apply(_self()).stream().map(x -> x.toArray(StmtAssign[]::new)).toArray(StmtAssign[][]::new);
            return store().withActor(actor)
                    .<ID>putMany(assigns)
                    .map(Fn.lengthEqual(assigns.length, List::size))
                    .flatMap(this::identities);
        }

        public Future<List<ID>> putManyGetIdentity(@Nullable ID actor, Collection<JsonObject> set) {
            var assigns = set.stream().map(this::assign).toArray(StmtAssign[][]::new);
            return store().withActor(actor)
                    .<ID>putMany(assigns)
                    .map(Fn.lengthEqual(assigns.length, List::size))
                    ;
        }

        public Future<List<ID>> putManyGetIdentity(@Nullable ID actor, Function<S, Collection<? extends Collection<StmtAssign>>> set) {
            var assigns = set.apply(_self()).stream().map(x -> x.toArray(StmtAssign[]::new)).toArray(StmtAssign[][]::new);
            return store().withActor(actor)
                    .<ID>putMany(assigns)
                    .map(Fn.lengthEqual(assigns.length, List::size))
                    ;
        }


        public Future<Integer> setAny(@Nullable ID actor, Function<S, Value.BooleanValue> cond, Function<S, Collection<Statement.SetStmt>> sets) {
            return store().withActor(actor)
                    .filter(cond.apply(_self()))
                    .justSet(sets.apply(_self()).toArray(Statement.SetStmt[]::new))
                    ;
        }

        public Future<Void> justSet(@Nullable ID actor, ID id, int version, Function<S, Collection<Statement.SetStmt>> sets) {
            return store().withActor(actor)
                    .filter(_identity().eq(id).and(Objects.requireNonNull(this.<Field.IntegerField>_traits(TraitField.VERSION)).eq(version)))
                    .justSet(sets.apply(_self()).toArray(Statement.SetStmt[]::new))
                    .map(Fn.equal(1))
                    .mapEmpty()
                    ;
        }

        public Future<Void> justSet(@Nullable ID actor, ID id, Function<S, Collection<Statement.SetStmt>> sets) {
            return store().withActor(actor)
                    .filter(_identity().eq(id))
                    .justSet(sets.apply(_self()).toArray(Statement.SetStmt[]::new))
                    .map(Fn.equal(1))
                    .mapEmpty()
                    ;
        }

        public Future<Void> justSet(@Nullable ID actor, ID id, int version, Collection<Statement.SetStmt> sets) {
            return store().withActor(actor)
                    .filter(_identity().eq(id).and(Objects.requireNonNull(this.<Field.IntegerField>_traits(TraitField.VERSION)).eq(version)))
                    .justSet(sets.toArray(Statement.SetStmt[]::new))
                    .map(Fn.equal(1))
                    .mapEmpty()
                    ;
        }

        public Future<Void> justSet(@Nullable ID actor, ID id, Collection<Statement.SetStmt> sets) {
            return store().withActor(actor)
                    .filter(_identity().eq(id))
                    .justSet(sets.toArray(Statement.SetStmt[]::new))
                    .map(Fn.equal(1))
                    .mapEmpty()
                    ;
        }

        public Future<Void> justSet(@Nullable ID actor, ID id, int version, JsonObject set) {
            var sets = sets(set);
            return store().withActor(actor)
                    .filter(_identity().eq(id).and(Objects.requireNonNull(this.<Field.IntegerField>_traits(TraitField.VERSION)).eq(version)))
                    .justSet(sets)
                    .map(Fn.equal(1))
                    .mapEmpty()
                    ;
        }

        public Future<Void> justSet(@Nullable ID actor, ID id, JsonObject set) {
            var sets = sets(set);
            return store().withActor(actor)
                    .filter(_identity().eq(id))
                    .justSet(sets)
                    .map(Fn.equal(1))
                    .mapEmpty()
                    ;
        }


        public Future<T> set(@Nullable ID actor, ID id, int version, Function<S, Collection<Statement.SetStmt>> sets) {
            return justSet(actor, id, version, sets)
                    .map(id)
                    .flatMap(this::identity)
                    ;
        }

        public Future<T> set(@Nullable ID actor, ID id, Function<S, Collection<Statement.SetStmt>> sets) {
            return justSet(actor, id, sets)
                    .map(id)
                    .flatMap(this::identity)
                    ;
        }

        public Future<T> set(@Nullable ID actor, ID id, int version, Collection<Statement.SetStmt> sets) {
            return justSet(actor, id, version, sets)
                    .map(id)
                    .flatMap(this::identity)
                    ;
        }

        public Future<T> set(@Nullable ID actor, ID id, Collection<Statement.SetStmt> sets) {
            return
                    justSet(actor, id, sets)
                            .map(id)
                            .flatMap(this::identity)
                    ;
        }

        public Future<T> set(@Nullable ID actor, ID id, int version, JsonObject set) {
            return justSet(actor, id, version, set)
                    .map(id)
                    .flatMap(this::identity)
                    ;
        }

        public Future<T> set(@Nullable ID actor, ID id, JsonObject set) {
            return justSet(actor, id, set)
                    .map(id)
                    .flatMap(this::identity)
                    ;
        }

        public Future<Boolean> exists(Function<S, Value.BooleanValue> cond) {
            return store().filter(cond.apply(_self())).count().map(x -> x > 0);
        }

        /// fetch optional one
        public Future<Optional<T>> maybe(ID id) {
            return store().filter(_identity().eq(id)).maybe();
        }

        /// fetch exactly one
        public Future<T> identity(ID id) {
            return store().filter(_identity().eq(id)).one();
        }

        /// fetch by identities

        public Future<List<T>> identities(Collection<ID> id) {
            return store().filter( _identity().eqAny(id)).any();
        }
        public Future<Optional<T>> one(ID id,int version) {
            return store().filter(_identity().eq(id).and(((Field.IntegerField) Objects.requireNonNull(_traits(TraitField.VERSION))).eq(version))).one().map(Optional::ofNullable);
        }
        /// fetch exactly one value that match this condition.
        public Future<Optional<T>> one(Function<S, Value.BooleanValue> cond) {
            return store().filter(cond.apply(_self())).one().map(Optional::ofNullable);
        }
        public Future<Optional<T>> oneWithHistory(Function<S, Value.BooleanValue> cond) {
            return store().withHistory().filter(cond.apply(_self())).one().map(Optional::ofNullable);
        }
        public Future<Optional<T>> maybe(ID id,int version) {
            return store().filter(_identity().eq(id).and(((Field.IntegerField) Objects.requireNonNull(_traits(TraitField.VERSION))).eq(version))).maybe();
        }
        public Future<Optional<T>> maybe(Function<S, Value.BooleanValue> cond) {
            return store().filter(cond.apply(_self())).maybe();
        }
        public Future<Optional<T>> maybeWithHistory(Function<S, Value.BooleanValue> cond) {
            return store().withHistory().filter(cond.apply(_self())).maybe();
        }
        public Future<List<T>> any(Function<S, Value.BooleanValue> cond) {
            return store().filter(cond.apply(_self())).any();
        }
        public Future<List<T>> anyWithHistory(Function<S, Value.BooleanValue> cond) {
            return store().withHistory().filter(cond.apply(_self())).any();
        }
        public <R> Future<R> any(Function<S, Value.BooleanValue> cond, BiFunction<S, Stages.Picker<T>, Future<R>> operate) {
            return operate.apply(_self(), store().filter(cond.apply(_self())));
        }
        //region tools
        private transient volatile @Nullable LinkedList<Field<?>> assignFields;

        @SuppressWarnings("unchecked")
        private StmtAssign[] assign(JsonObject set) {
            if (assignFields == null) {
                synchronized (_name) {
                    if (assignFields == null) {
                        var skip = new HashSet<Field<?>>();
                        var id = _identity();
                        if (!(id instanceof Value.NumberValue)) {
                            skip.add(_identity());
                        }
                        if (version >= 0) skip.add(_fields[version]);
                        if (removed >= 0) skip.add(_fields[removed]);
                        if (creator >= 0) skip.add(_fields[creator]);
                        if (createdAt >= 0) skip.add(_fields[createdAt]);
                        if (modifier >= 0) skip.add(_fields[modifier]);
                        if (modifiedAt >= 0) skip.add(_fields[modifiedAt]);
                        if (history >= 0) skip.add(_fields[history]);

                        assignFields = Arrays.stream(_fields)
                                .filter(Predicate.not(skip::contains))
                                .collect(Collectors.toCollection(LinkedList::new));
                    }
                }
            }
            assert assignFields!=null;
            //noinspection DataFlowIssue
            return assignFields.stream()
                    .map(x -> {
                        if (set.containsKey(x._property())) {
                            var v = set.getValue(x._property());
                            var b = ((Field.BaseField<Object, ?>) x);
                            return v == null ? b.valueNull() : b.value(v);
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .toArray(StmtAssign[]::new);

        }

        private transient volatile @Nullable LinkedList<Field<?>> setsFields;

        @SuppressWarnings("unchecked")
        private Statement.SetStmt[] sets(JsonObject set) {
            if (setsFields == null) {
                synchronized (_name) {
                    if (setsFields == null) {
                        var skip = new HashSet<Field<?>>();
                        skip.add(_identity());
                        if (version >= 0) skip.add(_fields[version]);
                        if (removed >= 0) skip.add(_fields[removed]);
                        if (creator >= 0) skip.add(_fields[creator]);
                        if (createdAt >= 0) skip.add(_fields[createdAt]);
                        if (modifier >= 0) skip.add(_fields[modifier]);
                        if (modifiedAt >= 0) skip.add(_fields[modifiedAt]);
                        if (history >= 0) skip.add(_fields[history]);
                        setsFields = Arrays.stream(_fields)
                                .filter(Predicate.not(skip::contains))
                                .collect(Collectors.toCollection(LinkedList::new));
                    }
                }
            }
            assert setsFields!=null;
            //noinspection DataFlowIssue
            return setsFields.stream()
                    .map(x -> {
                        if (set.containsKey(x._property())) {
                            var v = set.getValue(x._property());
                            var b = ((Field.BaseField<Object, ?>) x);
                            return v == null ? b.setNull() : b.set(v);
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .toArray(Statement.SetStmt[]::new);

        }

        //endregion

        //endregion

        //region join

        /// act inner join
        public <S1 extends Model<T1>, T1, R> Future<R> join(S1 s1, BiFunction<S, S1, Value.BooleanValue> cond, BiFunction<Tuple2<S, S1>, Stages.Joined.Joined2<T, T1>, Future<R>> action) {
            return action.apply(Tuple.tuple(_self(), s1), store().join(s1, cond.apply(_self(), s1)));

        }

        /// act left outer join
        public <S1 extends Model<T1>, T1, R> Future<R> joinWith(S1 s1, BiFunction<S, S1, Value.BooleanValue> cond, BiFunction<Tuple2<S, S1>, Stages.Joined.Joined2<T, T1>, Future<R>> action) {
            return action.apply(Tuple.tuple(_self(), s1), store().joinWith(s1, cond.apply(_self(), s1)));

        }

        /// act right outer join
        public <S1 extends Model<T1>, T1, R> Future<R> joinTo(S1 s1, BiFunction<S, S1, Value.BooleanValue> cond, BiFunction<Tuple2<S, S1>, Stages.Joined.Joined2<T, T1>, Future<R>> action) {
            return action.apply(Tuple.tuple(_self(), s1), store().joinTo(s1, cond.apply(_self(), s1)));

        }
        //endregion


    }
}

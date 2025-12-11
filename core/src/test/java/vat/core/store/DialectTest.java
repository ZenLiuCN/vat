package vat.core.store;

import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.impl.RowBase;
import org.jetbrains.annotations.Nullable;
import vat.api.store.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Function;

/// @author Zen.Liu
/// @since 2025-10-26

public abstract class DialectTest {
    static class SampleStore extends Model.Base<Long,JsonObject,SampleStore> {
        protected SampleStore(String schema) {
            super(schema, "sample", Function.identity(), 0, 1, 2, 3, 4, 5, 6,
                    -1);
        }
        protected SampleStore(){
            this(null);
        }

        @Override
        protected Field<?>[] buildFields() {
            return new Field<?>[]{
                    new Field.LongField("id", null, this),
                    new Field.IntegerField("version", null, this),
                    new Field.BooleanField("removed", null, this),
                    new Field.LongField("creator", null, this),
                    new Field.InstantField("create_at", "createAt", this),
                    new Field.LongField("modifier", null, this),
                    new Field.InstantField("modified_at", "modifiedAt", this),
                    new Field.JsonObjectField("profile", null, this),
            };
        }

        public Field.LongField id() {
            return field(0);
        }
        public Field.IntegerField version() {
            return  field(1);
        }
        public Field.BooleanField removed() {
            return  field(2);
        }
        public Field.LongField creator() {
            return  field(3);
        }
        public Field.InstantField createAt() {
            return  field(4);
        }
        public Field.LongField modifier() {
            return  field(5);
        }
        public Field.InstantField modifiedAt() {
            return  field(6);
        }
        public Field.JsonObjectField profile() {
            return  field(7);
        }
        @Override
        protected SampleStore _self() {
            return this;
        }

        @Override
        protected SampleStore copy(@Nullable String schema) {
            return new SampleStore(schema);
        }
    }

    static class Sample2Store extends Model.Base<Long,JsonObject,Sample2Store> {
        protected Sample2Store(String schema) {
            super(schema, "sample2", Function.identity(),
                    0, 1, 2, 3, 4, 5, 6,
                    -1);
        }
        protected Sample2Store(){
            this(null);
        }

        @Override
        protected Field<?>[] buildFields() {
            return new Field<?>[]{
                    new Field.LongField("id", null, this),
                    new Field.IntegerField("version", null, this),
                    new Field.BooleanField("removed", null, this),
                    new Field.LongField("creator", null, this),
                    new Field.InstantField("create_at", "createAt", this),
                    new Field.LongField("modifier", null, this),
                    new Field.InstantField("modified_at", "modifiedAt", this),
                    new Field.StringField("name", null, this),
                    new Field.JsonObjectField("profile", null, this),
            };
        }
        public Field.LongField id() {
            return field(0);
        }
        public Field.IntegerField version() {
            return  field(1);
        }
        public Field.BooleanField removed() {
            return  field(2);
        }
        public Field.LongField creator() {
            return  field(3);
        }
        public Field.InstantField createAt() {
            return  field(4);
        }
        public Field.LongField modifier() {
            return  field(5);
        }
        public Field.InstantField modifiedAt() {
            return  field(6);
        }
        public Field.StringField name() {
            return  field(7);
        }
        public Field.JsonObjectField profile() {
            return  field(8);
        }

        @Override
        protected Sample2Store _self() {
            return this;
        }

        @Override
        protected Sample2Store copy(@Nullable String schema) {
            return new Sample2Store(schema);
        }
    }

    protected void testSelectOne(Dialect dialect) {
        Field.INSTANT_OFFSET_MODE.set(true);
        var model = new SampleStore();
        var status = new Status(new State(model, null, dialect));
        var store = status.store();
        store.filter(model.id().eq(1L));
        var rendered = status.render(QueryType.SELECT_ONE);
        System.out.println(rendered);
        var out = rendered.reader.read(new RowBase(List.of(
                1L, 1, false, 2L, OffsetDateTime.now(), 2L, OffsetDateTime.now(),JsonObject.of("1sm","sample2")
        )) {
            @Override
            public String getColumnName(int pos) {
                return "";
            }

            @Override
            public int getColumnIndex(String column) {
                return 0;
            }
        });
        System.out.println(((JsonObject) out).encodePrettily());


        status = new Status(new State(model, null, dialect) {
        });
        store = status.store();
        store.filter(model.id().eq(1L)).pick(model.id(), model.removed());
        rendered = status.render(QueryType.SELECT_ONE);
        System.out.println(rendered);
        out = rendered.reader.read(new RowBase(List.of(
                1L, false
        )) {
            @Override
            public String getColumnName(int pos) {
                return "";
            }

            @Override
            public int getColumnIndex(String column) {
                return 0;
            }
        });
        System.out.println(out);
    }

    protected void testSelectJoinOne(Dialect dialect) {
        Field.INSTANT_OFFSET_MODE.set(true);
        var model = new SampleStore();
        var model2 = new Sample2Store();
        var status = new Status(new State(model, null, dialect) {
        });
        var store = status.store();
        store.join(model2, model2.id().eq(model.id())).filter(model.id().eq(1L));
        var rendered = status.render(QueryType.SELECT_ONE);
        System.out.println(rendered);
        var out = rendered.reader.read(new RowBase(List.of(
                1L, 1, false, 2L, OffsetDateTime.now(), 2L, OffsetDateTime.now(),JsonObject.of("1sm","sample1"),
                1L, 1, false, 2L, OffsetDateTime.now(), 2L, OffsetDateTime.now(), "sample2",JsonObject.of("1sm","sample2")
        )) {
            @Override
            public String getColumnName(int pos) {
                return "";
            }

            @Override
            public int getColumnIndex(String column) {
                return 0;
            }
        });

        System.out.println(out);
    }

    protected void testSelectJsonOne(Dialect dialect) {
        Field.INSTANT_OFFSET_MODE.set(true);
        var model = new SampleStore();
        var status = new Status(new State(model, null, dialect) {
        });
        var store = status.store();
        store.filter(model.id().eq(1L)).pick(model.profile().integerAt("1"));
        var rendered = status.render(QueryType.SELECT_ONE);
        System.out.println(rendered);
        var out = rendered.reader.read(new RowBase(List.of(
                1
        )) {
            @Override
            public String getColumnName(int pos) {
                return "";
            }

            @Override
            public int getColumnIndex(String column) {
                return 0;
            }
        });
        System.out.println(out);
    }
    protected void testSetJsonOne(Dialect dialect) {
        Field.INSTANT_OFFSET_MODE.set(true);
        var model = new SampleStore();
        var status = new Status(new State(model, null, dialect) {
        });
        var store = status.store();
        store.withActor(1L).filter(model.id().eq(1L));
        status.state().sets = new Statement.SetStmt[]{model.profile().setAt("1","1")};
        var rendered = status.render(QueryType.UPDATE);
        System.out.println(rendered);

    }
}

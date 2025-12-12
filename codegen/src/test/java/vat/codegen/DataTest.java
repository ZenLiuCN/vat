package vat.codegen;

import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;
import vat.api.utils.Lazy;

import javax.tools.JavaFileObject;
import java.util.List;

///
/// @author Zen.Liu
/// @since 2025-10-28


public class DataTest extends GeneratorTest {
    static {
        System.setProperty("codegen.ddl.mysql", "true");
        System.setProperty("codegen.ddl.postgres", "true");
//        System.setProperty("test.codegen.clip", "true");

    }

    @Test
    void generateData() {
        testing(
                List.of(
                      /*  JavaFileObjects.forSourceString(
                                "src/main/java/module-info",

//formatter:off
//language=java
"""
open module store.data {
     requires transitive vat.api;
}
"""
//formatter:on
                        ),*/
                        JavaFileObjects.forSourceString(
                                "store.data.Deal",

//formatter:off
//language=java
                                """
                                        package store.data.deal;
                                        import vat.api.Data;
                                        import vat.api.meta.*;
                                        import vat.api.implement.Interceptors;
                                        import vat.api.implement.Validators;
                                        import io.vertx.core.buffer.Buffer;
                                        import io.vertx.core.json.JsonArray;
                                        import io.vertx.core.json.JsonObject;import vat.api.utils.ITimes;
                                        
                                        import java.math.BigDecimal;
                                        import java.time.*;
                                        import java.util.List;
                                        import java.util.Map;
                                        import java.util.UUID;
                                        
                                        @Enhance(pojo = true)
                                        public interface Deal extends Data.Binary {
                                            Validators.LongValidator ID2V=i->{};
                                            Validators.LongValidator IDV=i->{};
                                            Interceptors.LongInterceptor IDI=i->i;
                                            Interceptors.LongInterceptor ID2I=i->i;
                                            @Intercept(value = "IDI",construct = true,holder = Deal.class)
                                            @Intercept(value = "ID2I",construct = true,holder = Deal.class)
                                            @Validate(value = "IDV",construct = true,holder = Deal.class)
                                            @Validate(value = "ID2V",construct = true,holder = Deal.class)
                                            long id();
                                            @Validate(value = "NONE_BLANK",construct = true)
                                            @Validate(value = "NONE_EMPTY")
                                            String name();
                                            @Alias("timestamp")
                                            Instant time();
                                            JsonObject profile();
                                            JsonArray array();
                                            List<JsonArray> arrays();
                                            Duration dur();
                                            Period period();
                                            OffsetDateTime dtz();
                                            OffsetTime tz();
                                            LocalDateTime dt();
                                            LocalDate dtl();
                                            LocalTime tl();
                                        
                                            BigDecimal decimalObject();
                                            Double doubleObject();
                                            Boolean booleanObject();
                                            Float floatObject();
                                            Long longObject();
                                            Integer intObject();
                                            Short shortObject();
                                            Byte byteObject();
                                        
                                            BigDecimal[] decimalArray();
                                            Double[] doubleArray();
                                            Boolean[] booleanArray();
                                            Float[] floatArray();
                                            Long[] longArray();
                                            Integer[] intArray();
                                            Short[] shortArray();
                                            Byte[] byteArray();
                                        
                                            double doubleProperty();
                                            boolean booleanProperty();
                                            float floatProperty();
                                            long longProperty();
                                            int   intProperty();
                                            short shortProperty();
                                            byte byteProperty();
                                        
                                           double [] doubleArrayProperty();
                                            boolean [] booleanArrayProperty();
                                            float [] floatArrayProperty();
                                            long [] longArrayProperty();
                                            int   [] intArrayProperty();
                                            short [] shortArrayProperty();
                                        
                                            byte[] binProperty();
                                            UUID uuidProperty();
                                            ITimes.IDate iDateProperty();
                                            ITimes.ITime iTimeProperty();
                                            ITimes.IDatetime iDatetimeProperty();
                                            Class<?> classProperty();
                                        
                                            Buffer bufferProperty();
                                        
                                            Map<String,Long> secure();
                                        
                                            @Virtual("profile")
                                            Map<String,String[]> security();
                                        }
                                        """
//formatter:on
                        )
                )
        );
    }

    @Test
    void generateActor() {
        testing("store.data.Deal",
                //language=java
                """
                        package store.data.deal;
                        import vat.api.Actor;
                        import vat.api.Data;import vat.api.Entity;
                        import vat.api.meta.*;
                        
                        import vat.api.implement.Interceptors;
                        import vat.api.implement.Validators;
                        import vat.api.store.Interceptor;
                        import io.vertx.core.buffer.Buffer;
                        import io.vertx.core.json.JsonArray;
                        import io.vertx.core.json.JsonObject;
                        
                        import java.math.BigDecimal;
                        import java.util.UUID;
                        import java.time.Instant;
                        import java.util.Map;
                        @Enhance(pojo = true)
                        @Table("store_actor")
                        public interface Deal extends Actor,Entity.Base,Data.Binary {
                            Validators.LongValidator ID2V=i->{};
                            Validators.LongValidator IDV=i->{};
                            Interceptors.LongInterceptor IDI=i->i;
                            Interceptors.LongInterceptor ID2I=i->i;
                            @Intercept(value = "IDI",construct = true,holder = Deal.class)
                            @Intercept(value = "ID2I",construct = true,holder = Deal.class)
                            @Validate(value = "IDV",construct = true,holder = Deal.class)
                            @Validate(value = "ID2V",construct = true,holder = Deal.class)
                            @Override
                            Long id();
                            @Validate(value = "NONE_BLANK",construct = true)
                            @Validate(value = "NONE_EMPTY")
                            @Column(size = 255)
                            String name();
                            Instant time();
                            JsonObject profile();
                            JsonArray array();
                        
                            @Column(value = "dec",precision = 10,scale = 2)
                            BigDecimal decimalObject();
                            Interceptor<Double> DoubleID=(r,i)->i;
                            @Column(interceptField = "DoubleID")
                            Double doubleObject();
                            Boolean booleanObject();
                            Float floatObject();
                            Long longObject();
                            Integer intObject();
                            Short shortObject();
                            Byte byteObject();
                        
                            double doubleProperty();
                            boolean booleanProperty();
                            float floatProperty();
                            long longProperty();
                            int   intProperty();
                            short shortProperty();
                            byte byteProperty();
                            UUID uuidProperty();
                             Class<?> classProperty();
                             @Column(size = 255)
                            Buffer bufferProperty();
                            @Column(size = 255)
                            byte[] bytesProperty();
                        
                            @Virtual("profile")
                            Map<String,String[]> security();
                        }
                        """);
    }

    @Test
    void generateAbility() {
        testing("store.data.Deal",
                //language=java
                """
                        package store.data.deal;
                        import vat.api.Ability;
                        import vat.api.Entity;
                        import vat.api.implement.Interceptors;
                        import vat.api.implement.Validators;
                        import vat.api.meta.*;
                        import vat.api.store.Interceptor;
                        import io.vertx.core.buffer.Buffer;
                        import io.vertx.core.json.JsonArray;
                        import io.vertx.core.json.JsonObject;
                        
                        import java.math.BigDecimal;
                        import java.time.Duration;
                        import java.time.Instant;
                        import java.time.Period;
                        import java.util.List;
                        import java.util.Map;
                        @Enhance
                        @Table("store_actor")
                        public interface Deal extends Ability,Entity.Base {
                            Validators.LongValidator ID2V=i->{};
                            Validators.LongValidator IDV=i->{};
                            Interceptors.LongInterceptor IDI=i->i;
                            Interceptors.LongInterceptor ID2I=i->i;
                            @Intercept(value = "IDI",construct = true,holder = Deal.class)
                            @Intercept(value = "ID2I",construct = true,holder = Deal.class)
                            @Validate(value = "IDV",construct = true,holder = Deal.class)
                            @Validate(value = "ID2V",construct = true,holder = Deal.class)
                            @Override
                            Long id();
                            @Validate(value = "NONE_BLANK",construct = true)
                            @Validate(value = "NONE_EMPTY")
                            @Column(size = 255)
                            String name();
                            Instant time();
                            JsonObject profile();
                            JsonArray array();
                        
                            @Column(value = "dec",precision = 10,scale = 2)
                            BigDecimal decimalObject();
                            Interceptor<Double> DoubleID=(r,i)->i;
                            @Column(interceptField = "DoubleID")
                            Double doubleObject();
                            Boolean booleanObject();
                            Float floatObject();
                            Long longObject();
                            Integer intObject();
                            Short shortObject();
                            Byte byteObject();
                        
                            double doubleProperty();
                            boolean booleanProperty();
                            float floatProperty();
                            long longProperty();
                            int   intProperty();
                            short shortProperty();
                            byte byteProperty();
                            @Column(size = 255)
                            Buffer bufferProperty();
                        
                            @Virtual("profile")
                            Map<String,String[]> security();
                        }
                        """);
    }

    @Test
    void generateEvent() {
        testing("store.data.Deal",
                //language=java
                """
                        package store.data.deal;
                        import vat.api.Event;
                        import vat.api.implement.Interceptors;
                        import vat.api.implement.Validators;
                        import vat.api.meta.*;
                        import vat.api.store.Interceptor;
                        import io.vertx.core.buffer.Buffer;
                        import io.vertx.core.json.JsonArray;
                        import io.vertx.core.json.JsonObject;
                        
                        import java.math.BigDecimal;
                        import java.time.Duration;
                        import java.time.Instant;
                        import java.time.Period;
                        import java.util.List;
                        import java.util.Map;
                        @Enhance(pojo = true)
                        public interface Deal extends Event {
                            enum SomeKind{
                                One,
                                Two,
                                Three,
                            }
                            Validators.LongValidator ID2V=i->{};
                            Validators.LongValidator IDV=i->{};
                            Interceptors.LongInterceptor IDI=i->i;
                            Interceptors.LongInterceptor ID2I=i->i;
                            @Intercept(value = "IDI",construct = true,holder = Deal.class)
                            @Intercept(value = "ID2I",construct = true,holder = Deal.class)
                            @Validate(value = "IDV",construct = true,holder = Deal.class)
                            @Validate(value = "ID2V",construct = true,holder = Deal.class)
                            Long id();
                            @Validate(value = "NONE_BLANK",construct = true)
                            @Validate(value = "NONE_EMPTY")
                            String name();
                            Instant time();
                            JsonObject profile();
                            JsonArray array();
                            @EventKind
                            SomeKind kind();
                        
                            BigDecimal decimalObject();
                            Double doubleObject();
                            Boolean booleanObject();
                            Float floatObject();
                            Long longObject();
                            Integer intObject();
                            Short shortObject();
                            Byte byteObject();
                        
                            double doubleProperty();
                            boolean booleanProperty();
                            float floatProperty();
                            long longProperty();
                            int   intProperty();
                            short shortProperty();
                            byte byteProperty();
                        
                            Buffer bufferProperty();
                        
                            @Virtual("profile")
                            Map<String,String[]> security();
                        }
                        """);
    }

    @Test
    void generateEventStatic() {
        testing("store.data.Deal",
                //language=java
                """
                        package store.data.deal;
                        import vat.api.Event;
                        import vat.api.implement.Interceptors;
                        import vat.api.implement.Validators;
                        import vat.api.meta.*;
                        import vat.api.store.Interceptor;
                        import io.vertx.core.buffer.Buffer;
                        import io.vertx.core.json.JsonArray;
                        import io.vertx.core.json.JsonObject;
                        
                        import java.math.BigDecimal;
                        import java.time.Duration;
                        import java.time.Instant;
                        import java.time.Period;
                        import java.util.List;
                        import java.util.Map;
                        @Enhance(pojo=true)
                        public interface Deal extends Event {
                            enum SomeKind{
                                One,
                                Two,
                                Three,
                            }
                            Validators.LongValidator ID2V=i->{};
                            Validators.LongValidator IDV=i->{};
                            Interceptors.LongInterceptor IDI=i->i;
                            Interceptors.LongInterceptor ID2I=i->i;
                            @Intercept(value = "IDI",construct = true,holder = Deal.class)
                            @Intercept(value = "ID2I",construct = true,holder = Deal.class)
                            @Validate(value = "IDV",construct = true,holder = Deal.class)
                            @Validate(value = "ID2V",construct = true,holder = Deal.class)
                            Long id();
                            @Validate(value = "NONE_BLANK",construct = true)
                            @Validate(value = "NONE_EMPTY")
                            String name();
                            Instant time();
                            JsonObject profile();
                            JsonArray array();
                            int KIND_ONE=1;
                            int KIND_TWO=2;
                            int KIND_THREE=3;
                            int kind();
                        
                            BigDecimal decimalObject();
                            Double doubleObject();
                            Boolean booleanObject();
                            Float floatObject();
                            Long longObject();
                            Integer intObject();
                            Short shortObject();
                            Byte byteObject();
                        
                            double doubleProperty();
                            boolean booleanProperty();
                            float floatProperty();
                            long longProperty();
                            int   intProperty();
                            short shortProperty();
                            byte byteProperty();
                        
                            Buffer bufferProperty();
                        
                            @Virtual("profile")
                            Map<String,String[]> security();
                        }
                        """);
    }

    @Test
    void generateEventEnum() {
        testing("store.data.Deal",
                //language=java
                """
                        package store.data.deal;
                        import vat.api.Event;
                        import vat.api.implement.Interceptors;
                        import vat.api.implement.Validators;
                        import vat.api.meta.*;
                        import vat.api.store.Interceptor;
                        import io.vertx.core.buffer.Buffer;
                        import io.vertx.core.json.JsonArray;
                        import io.vertx.core.json.JsonObject;
                        
                        import java.math.BigDecimal;
                        import java.time.Duration;
                        import java.time.Instant;
                        import java.time.Period;
                        import java.util.List;
                        import java.util.Map;
                        @Enhance
                        public interface Deal extends Event.EnumBased<Deal.SomeKind> {
                            enum SomeKind{
                                One,
                                Two,
                                Three,
                            }
                            Validators.LongValidator ID2V=i->{};
                            Validators.LongValidator IDV=i->{};
                            Interceptors.LongInterceptor IDI=i->i;
                            Interceptors.LongInterceptor ID2I=i->i;
                            @Intercept(value = "IDI",construct = true,holder = Deal.class)
                            @Intercept(value = "ID2I",construct = true,holder = Deal.class)
                            @Validate(value = "IDV",construct = true,holder = Deal.class)
                            @Validate(value = "ID2V",construct = true,holder = Deal.class)
                            Long id();
                            @Validate(value = "NONE_BLANK",construct = true)
                            @Validate(value = "NONE_EMPTY")
                            String name();
                            Instant time();
                            JsonObject profile();
                            JsonArray array();
                        
                        
                            BigDecimal decimalObject();
                            Double doubleObject();
                            Boolean booleanObject();
                            Float floatObject();
                            Long longObject();
                            Integer intObject();
                            Short shortObject();
                            Byte byteObject();
                            @Computed
                            default double doubleComputed(){
                                return 5;
                            }
                            double doubleProperty();
                            boolean booleanProperty();
                            float floatProperty();
                            long longProperty();
                            int   intProperty();
                            short shortProperty();
                            byte byteProperty();
                        
                            Buffer bufferProperty();
                        
                            @Virtual("profile")
                            Map<String,String[]> security();
                        }
                        """);
    }

    @Test
    void generateAudit() {
        testing("vat.foundation.audits.api.Audits",
//@formatter:off
//language=java
"""
package vat.foundation.audits.api;

import vat.api.Activities;
import vat.api.meta.*;
import vat.api.Event;
import vat.api.Record;
import vat.api.Store;
import vat.api.trait.History;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import java.util.Optional;

/**
 * @author Zen.Liu
 * @since 2025-10-20
 */
@Enhance
@Describe("_AUDITS")
public interface Audits extends Activities {
    @Describe("_AUDIT_RESULT")
    enum Result {
        @Describe("_AUDIT_RESULT_TODO")
        TODO,
        @Describe("_AUDIT_RESULT_SAFE")
        SAFE,
        @Describe("_AUDIT_RESULT_WARN")
        WARN,
        @Describe("_AUDIT_RESULT_ERROR")
        ERROR,
        @Describe("_AUDIT_RESULT_FATAL")
        FATAL,
        @Describe("_AUDIT_RESULT_DONE")
        DONE,
    }

    @Enhance
    @Describe(value = "_AUDITS_AUDIT", domain = Audit.class)
    @Table("foundation_audits_audit")
    interface Audit extends Record.Base, History {
        @Describe("_AUDITS_AUDIT_TOPIC")
        @Column(size = 255, indexed = {"topic"})
        String topic();

        @Describe("_AUDITS_AUDIT_REQUEST")
        @Nullable
        JsonObject request();

        @Describe("_AUDITS_AUDIT_RESPONSE")
        @Nullable
        JsonObject response();

        @Describe("_AUDITS_AUDIT_STATUS")
        @Column(indexed = {"status"})
        Status status();


        @Describe("_AUDITS_AUDIT_ACTOR")
        long actor();

        @Describe("_AUDITS_AUDIT_REPORTED")
        long reported();

        @Describe("_AUDITS_AUDIT_AUDITED")
        long audited();

        @Describe("_AUDITS_AUDIT_AUDITOR")
        long auditor();

        @Describe("_AUDITS_AUDIT_RESULT")
        @Column(indexed = {"result"})
        Result result();

        @Describe("_AUDITS_AUDIT_COMMENT")
        @Column(size = 1024)
        String comment();

    }

    @Describe("_AUDITS_STATUS")
    enum Status {
        @Describe("_AUDITS_STATUS_REQUEST")
        REQUEST,
        @Describe("_AUDITS_STATUS_RESPONSE")
        RESPONSE,
        @Describe("_AUDITS_STATUS_SUCCESS")
        SUCCESS,
        @Describe("_AUDITS_STATUS_FAILURE")
        FAILURE
    }

    @Enhance
    @Describe("_AUDITS_AUDIT_REQUEST")
    interface AuditRequest extends Event.EnumBased<Status> {
        @Describe("_AUDITS_AUDIT_TOPIC")
        String topic();

        @Describe("_AUDITS_AUDIT_REQUEST")
        JsonObject request();

        @Describe("_AUDITS_AUDIT_ACTOR")
        long actor();

    }


    @Enhance
    @Describe("_AUDITS_AUDIT_RESPONSE")
    interface AuditResponse extends Event.EnumBased<Status> {
        @Describe("_AUDITS_AUDIT_TOPIC")
        String topic();

        @Describe("_AUDITS_AUDIT_RESPONSE")
        JsonObject response();

        @Describe("_AUDITS_AUDIT_ACTOR")
        long actor();

    }

    @Enhance
    @Describe("_AUDITS_AUDIT_INVOKE")
    interface AuditInvoke extends Event.EnumBased<Status> {
        @Describe("_AUDITS_AUDIT_TOPIC")
        String topic();

        @Describe("_AUDITS_AUDIT_REQUEST")
        JsonObject request();

        @Describe("_AUDITS_AUDIT_RESPONSE")
        JsonObject response();

        @Describe("_AUDITS_AUDIT_ACTOR")
        long actor();

    }

    @Describe("_AUDITS_ACT_SUBSCRIBE_REQUEST")
    @Subscribe
    default void onRequest(AuditRequest event) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Describe("_AUDITS_ACT_SUBSCRIBE_RESPONSE")
    @Subscribe
    default void onResponse(AuditResponse event) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Describe("_AUDITS_ACT_SUBSCRIBE_INVOKE")
    @Subscribe
    default void onInvoke(AuditInvoke event) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Storage("/schema/audits/audit")
    default Store<Audit> audits() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    Future<Optional<Audit>> identity(long id);
}

"""
//@formatter:on
        );
    }

    @Test
    void generateActivity() {
        testing(List.of(

                JavaFileObjects.forSourceString("vat.foundation.audits.api.Audits",
//@formatter:off
//region code
//language=java
"""
package vat.foundation.audits.api;
import vat.api.Record;
import vat.api.Activities;
import vat.api.Event;
import vat.api.meta.*;
import io.vertx.core.json.JsonObject;
@Enhance
public interface Audits extends Activities {
    @Enhance
    interface Audit extends Record.Base{
        @Column(max = 255)
        String topic();
        JsonObject request();
        JsonObject response();
    }
    @Describe("审计类型")
    enum Status{
        REQUEST,RESPONSE,SURROUND
    }
    @Enhance
    interface Auditing extends Event.EnumBased<Status>{
        String topic();
        JsonObject request();
        JsonObject response();
    }
}
"""
//endregion code
//@formatter:on
                ),
                //endregion file
                JavaFileObjects.forSourceString("vat.foundation.users.api.Users",
//@formatter:off
//region code
//language=java
"""
package vat.foundation.users.api;

import vat.api.Record;
import vat.api.meta.*;
import vat.api.*;
import vat.foundation.audits.api.Audits;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.SqlConnection;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

@Enhance
@Describe(value = "用户域",desc = "基础用户管理")
public interface Users extends Activities {
    @Enhance
    @Table("foundation_user")
    @Describe(value = "用户", desc = "基础用户")
    interface User extends Actor, Entity.Base {
        @Describe(value = "档案", desc = "共享档案")
        JsonObject profile();
    }
    
    @Enhance
    @Table("foundation_certificate")
    @Describe(value = "鉴权", desc = "鉴权档案")
    interface Certificate extends Record, Entity.Base {
        @Describe(value = "类型", desc = "鉴权类型")
        int kind();
        
        @Describe(value = "用户", desc = "归属用户")
        @Identity.Reference(value = User.class, provider = Users.class)
        @Column(indexed = {"user"})
        long user();
        
        @Column(max = 256,unique = {"kind","identifier"})
        @Describe(value = "凭据", desc = "类型下唯一凭据")
        String identifier();
         @Column(max = 256)
        @Describe(value = "证明", desc = "身份证明")
        Buffer prof();
    }
    
    @Enhance
    interface UserChanges extends Event {
        int KIND_REGISTERED = 1;
        int KIND_UNREGISTERED = 2;
        int KIND_AUTHORIZED = 3;
        int KIND_REVOKED = 4;
        
        int kind();
    }
    
    @Config
    default Optional<String> port(){
        return Optional.of("8080");
    }
    
    @Storage
    default Store<User> users(@Nullable SqlConnection tx){
        return null;
    }
    
    @Errors
    default DomainError unauthorized(){
        return  DomainError.System.unauthorized("");
    }
    @Errors
    default DomainError unregistered(String user){
        return  DomainError.System.unauthorized(user);
    }
    @Subscribe("/events/changes")
    default void onChange(UserChanges evt) {
        throw new UnsupportedOperationException("not yet implemented");
    }
    
    @Publish
    default void notifyChange(Consumer<? super UserChanges> evt) {
        throw new UnsupportedOperationException("not yet implemented");
    }
    @Uses("/audits/address")
    default Audits audits(){
        throw new UnsupportedOperationException("not yet implemented");
    }
    @Describe(value = "用户身份", desc = "将用户ID转换为用户")
    Future<User> identity(long id);
    record Identifier(int kind,String identifier) implements Data{
        public Identifier(JsonObject v) {
            this(v.getInteger("kind"),v.getString("identifier"));
        }
        @Override public JsonObject asJson() {
            return JsonObject.of("kind",kind,"identifier",identifier);
        }
    }
    @Describe(value = "用户身份", desc = "将用户识别号转换为用户")
    Future<User> identifier(Identifier identifier);
    @Enhance
    interface Authorize extends Data {
        int kind();
        
        @Describe(value = "凭据", desc = "唯一凭据")
        String identity();
        
        @Describe(value = "证明", desc = "身份证明")
        String prof();
    }
    
    @Describe(value = "用户鉴权", desc = "将用户ID转换为用户")
    Future<User> authorize(Authorize auth);
    
    Future<Byte> exPrimByte(byte in);
    Future<Short> exPrimShort(byte in);
    Future<Integer> exPrimInteger(int in);
    Future<Long> exPrimLong(long in);
    Future<Float> exPrimFloat(float in);
    Future<Double> exPrimDouble(double in);
    
    Future<Void> exNoneDouble(double in);
    enum  Sample{
        ONE,TWO,THREE
    }
    Future<Void> enumWrite(Sample in);
    Future<Sample> enumRead(@Nullable Sample in);
    Future<JsonObject> jsonObjectRead(@Nullable JsonObject in);
    Future<JsonArray> jsonArrayRead(@Nullable JsonArray in);
    Future<Set<JsonArray>> sets(@Nullable List<JsonObject> in);
    Future<List<JsonObject>> lists(@Nullable Set<JsonArray> in);
    Future<Map<JsonObject,JsonArray>> maps(@Nullable Map<JsonArray,JsonObject> in);
    
    
    Future<@vat.api.meta.Nullable  Byte> exObjectByte(@Nullable  Byte in);
    Future<@vat.api.meta.Nullable Short> exObjectShort(@Nullable Byte in);
    Future<@vat.api.meta.Nullable  Integer> exObjectInteger(@Nullable  Integer in);
    Future<@vat.api.meta.Nullable Long> exObjectLong(@Nullable Long in);
    Future<@vat.api.meta.Nullable  Float> exObjectFloat(@Nullable  Float in);
    Future<@vat.api.meta.Nullable Double> exObjectDouble(@Nullable Double in);
    
    Future<Long> exampleLong(Identifier identifier);
    Future<Integer> exampleInteger(@Nullable Identifier identifier);
}
"""
//endregion code
//@formatter:on
                )

        ));
    }

    @Test
    void generateActivityBinary() {
        testing(List.of(

                JavaFileObjects.forSourceString("vat.foundation.audits.api.Audits",
//@formatter:off
//region code
//language=java
                        """
                        package vat.foundation.audits.api;
                        import vat.api.Record;
                        import vat.api.Activities;
                        import vat.api.Event;
                        import vat.api.meta.*;
                        import io.vertx.core.json.JsonObject;
                        @Enhance
                        public interface Audits extends Activities {
                            @Enhance
                            interface Audit extends Record.Base{
                                @Column(max = 255)
                                String topic();
                                JsonObject request();
                                JsonObject response();
                            }
                            @Describe("审计类型")
                            enum Status{
                                REQUEST,RESPONSE,SURROUND
                            }
                            @Enhance
                            interface Auditing extends Event.EnumBased<Status>{
                                String topic();
                                JsonObject request();
                                JsonObject response();
                            }
                        }
                        """
//endregion code
//@formatter:on
                ),
                //endregion file
                JavaFileObjects.forSourceString("vat.foundation.users.api.Users",
//@formatter:off
//region code
//language=java
                        """
                        package vat.foundation.users.api;
                        
                        import vat.api.Record;
                        import vat.api.meta.*;
                        import vat.api.*;
                        import vat.foundation.audits.api.Audits;
                        import io.vertx.core.Future;
                        import io.vertx.core.buffer.Buffer;
                        import io.vertx.core.json.JsonArray;
                        import io.vertx.core.json.JsonObject;
                        import io.vertx.sqlclient.SqlConnection;
                        import org.jetbrains.annotations.Nullable;
                        
                        import java.util.List;
                        import java.util.Map;
                        import java.util.Optional;
                        import java.util.Set;
                        import java.util.function.Consumer;
                        
                        @Enhance
                        @Describe(value = "用户域",desc = "基础用户管理")
                        public interface Users extends Activities {
                            @Enhance
                            @Table("foundation_user")
                            @Describe(value = "用户", desc = "基础用户")
                            interface User extends Actor, Entity.Base,Data.Binary {
                                @Describe(value = "档案", desc = "共享档案")
                                JsonObject profile();
                            }
                       
                            @Enhance
                            @Table("foundation_certificate")
                            @Describe(value = "鉴权", desc = "鉴权档案")
                            interface Certificate extends Record, Entity.Base {
                                @Describe(value = "类型", desc = "鉴权类型")
                                int kind();
                        
                                @Describe(value = "用户", desc = "归属用户")
                                @Identity.Reference(value = User.class, provider = Users.class)
                                long user();
                        
                                @Column(max = 256)
                                @Describe(value = "凭据", desc = "类型下唯一凭据")
                                String identity();
                                 @Column(max = 256)
                                @Describe(value = "证明", desc = "身份证明")
                                Buffer prof();
                            }
                        
                            @Enhance
                            interface UserChanges extends Event {
                                int KIND_REGISTERED = 1;
                                int KIND_UNREGISTERED = 2;
                                int KIND_AUTHORIZED = 3;
                                int KIND_REVOKED = 4;
                        
                                int kind();
                            }
                        
                            @Enhance
                            interface Context extends Domain.Context,Users{
                                @Config
                            default Optional<String> port(){
                                return Optional.of("8080");
                            }
                            
                            @Storage
                            default Store<User> users(@Nullable SqlConnection tx){
                                return null;
                            }
                            
                            @Errors
                            default DomainError unauthorized(){
                                return  DomainError.System.unauthorized("");
                            }
                            @Errors
                            default DomainError unregistered(String user){
                                return  DomainError.System.unauthorized(user);
                            }
                            @Subscribe("/events/changes")
                            default void onChange(UserChanges evt) {
                                throw new UnsupportedOperationException("not yet implemented");
                            }
                            
                            @Publish
                            default void notifyChange(Consumer<? super UserChanges> evt) {
                                throw new UnsupportedOperationException("not yet implemented");
                            }
                            @Uses("/audits/address")
                            default Audits audits(){
                                throw new UnsupportedOperationException("not yet implemented");
                            }
                            }
                            @Describe(value = "用户身份", desc = "将用户ID转换为用户")
                            Future<User> identity(long id);
                            record Identifier(int kind,String identifier) implements Data{
                                public Identifier(JsonObject v) {
                                    this(v.getInteger("kind"),v.getString("identifier"));
                                }
                                @Override public JsonObject asJson() {
                                    return JsonObject.of("kind",kind,"identifier",identifier);
                                }
                            }
                            @Describe(value = "用户身份", desc = "将用户识别号转换为用户")
                            Future<User> identifier(Identifier identifier);
                            @Enhance
                            interface Authorize extends Data {
                                int kind();
                                
                                @Describe(value = "凭据", desc = "唯一凭据")
                                String identity();
                                
                                @Describe(value = "证明", desc = "身份证明")
                                String prof();
                            }
                            
                            @Describe(value = "用户鉴权", desc = "将用户ID转换为用户")
                            Future<User> authorize(Authorize auth);
                            
                            Future<Byte> exPrimByte(byte in);
                            Future<Short> exPrimShort(byte in);
                            Future<Integer> exPrimInteger(int in);
                            Future<Long> exPrimLong(long in);
                            Future<Float> exPrimFloat(float in);
                            Future<Double> exPrimDouble(double in);
                            
                            Future<Void> exNoneDouble(double in);
                            enum  Sample{
                                ONE,TWO,THREE
                            }
                            Future<Void> enumWrite(Sample in);
                            Future<Sample> enumRead(@Nullable Sample in);
                            Future<JsonObject> jsonObjectRead(@Nullable JsonObject in);
                            Future<JsonArray> jsonArrayRead(@Nullable JsonArray in);
                            Future<Set<JsonArray>> sets(@Nullable List<JsonObject> in);
                            Future<List<JsonObject>> lists(@Nullable Set<JsonArray> in);
                            Future<Map<JsonObject,JsonArray>> maps(@Nullable Map<JsonArray,JsonObject> in);
                            
                            
                            Future<@vat.api.meta.Nullable  Byte> exObjectByte(@Nullable  Byte in);
                            Future<@vat.api.meta.Nullable Short> exObjectShort(@Nullable Byte in);
                            Future<@vat.api.meta.Nullable  Integer> exObjectInteger(@Nullable  Integer in);
                            Future<@vat.api.meta.Nullable Long> exObjectLong(@Nullable Long in);
                            Future<@vat.api.meta.Nullable  Float> exObjectFloat(@Nullable  Float in);
                            Future<@vat.api.meta.Nullable Double> exObjectDouble(@Nullable Double in);
                            
                            Future<Long> exampleLong(Identifier identifier);
                            Future<Integer> exampleInteger(@Nullable Identifier identifier);
                        }
                        """
//endregion code
//@formatter:on
                )

        ));
    }

    @Test
    void generateFoundation() {
        testing(List.of(

                JavaFileObjects.forSourceString("vat.foundation.audits.api.Audits",
//@formatter:off
//region code
//language=java
"""
package vat.foundation.audits.api;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import lombok.With;
import org.jetbrains.annotations.Nullable;
import vat.api.*;
import vat.api.Record;
import vat.api.implement.PubSub;
import vat.api.meta.*;
import vat.api.trait.History;

import java.util.Optional;
import java.util.function.Function;

/**
 * @author Zen.Liu
 * @since 2025-10-20
 */
@Enhance
@Describe("_AUDITS")
public interface Audits extends Activities {
    @Describe("_AUDIT_RESULT")
    enum Result {
        @Describe("_AUDIT_RESULT_TODO")
        TODO,
        @Describe("_AUDIT_RESULT_SAFE")
        SAFE,
        @Describe("_AUDIT_RESULT_WARN")
        WARN,
        @Describe("_AUDIT_RESULT_ERROR")
        ERROR,
        @Describe("_AUDIT_RESULT_FATAL")
        FATAL,
        @Describe("_AUDIT_RESULT_DONE")
        DONE,
    }

    @Enhance
    @Describe(value = "_AUDITS_AUDIT", domain = Audit.class)
    @Table("foundation_audits_audit")
    interface Audit extends Record.Base, History {
        @Describe("_AUDITS_AUDIT_TOPIC")
        @Column(size = 255, indexed = {"topic"})
        String topic();

        @Describe("_AUDITS_AUDIT_REQUEST")
        @Nullable
        JsonObject request();

        @Describe("_AUDITS_AUDIT_RESPONSE")
        @Nullable
        JsonObject response();

        @Describe("_AUDITS_AUDIT_STATUS")
        @Column(indexed = {"status"})
        Status status();


        @Describe("_AUDITS_AUDIT_ACTOR")
        long actor();

        @Describe("_AUDITS_AUDIT_REPORTED")
        long reported();

        @Describe("_AUDITS_AUDIT_AUDITED")
        long audited();

        @Describe("_AUDITS_AUDIT_AUDITOR")
        long auditor();

        @Describe("_AUDITS_AUDIT_RESULT")
        @Column(indexed = {"result"})
        Result result();

        @Describe("_AUDITS_AUDIT_COMMENT")
        @Column(size = 1024)
        String comment();

    }

    @Describe("_AUDITS_STATUS")
    enum Status {
        @Describe("_AUDITS_STATUS_REQUEST")
        REQUEST,
        @Describe("_AUDITS_STATUS_RESPONSE")
        RESPONSE,
        @Describe("_AUDITS_STATUS_SUCCESS")
        SUCCESS,
        @Describe("_AUDITS_STATUS_FAILURE")
        FAILURE
    }

    @Enhance
    @Describe("_AUDITS_AUDIT_REQUEST")
    interface AuditRequest extends Event.EnumBased<Status> {
        @Describe("_AUDITS_AUDIT_TOPIC")
        String topic();

        @Describe("_AUDITS_AUDIT_REQUEST")
        JsonObject request();

        @Describe("_AUDITS_AUDIT_ACTOR")
        long actor();

        record Data(Status kind,
                    long actor,
                    String topic,
                    @With JsonObject request) implements AuditRequest {
            @Override
            public JsonObject asJson() {
                return JsonObject.of(
                        "kind", kind.ordinal(),
                        "topic", topic,
                        "actor", actor,
                        "request", request
                );
            }

            public AuditRequest add(Throwable ex) {
                return withRequest(request.put("error", DomainError.dumpJsonObject(ex)));
            }
        }

    }

    record AuditRequestAuditor(PubSub.Publish<AuditRequest> p) {
        public <R> Future<R> invoke(String topic, Long actor, JsonObject request, Future<R> action) {
            var b = new AuditRequest.Data(
                    Status.REQUEST
                    , actor == null ? -1 : actor
                    , topic
                    , JsonObject.of("request", request)
            );
            return action.onSuccess(v -> p.accept(b))
                    .onFailure(v -> p.accept(b.add(v)));
        }
    }

    @Enhance
    @Describe("_AUDITS_AUDIT_RESPONSE")
    interface AuditResponse extends Event.EnumBased<Status> {
        @Describe("_AUDITS_AUDIT_TOPIC")
        String topic();

        @Describe("_AUDITS_AUDIT_RESPONSE")
        JsonObject response();

        @Describe("_AUDITS_AUDIT_ACTOR")
        long actor();


        record Data(Status kind,
                    long actor,
                    String topic,
                    JsonObject response) implements AuditResponse {
            @Override
            public JsonObject asJson() {
                return JsonObject.of(
                        "kind", kind.ordinal(),
                        "topic", topic,
                        "actor", actor,
                        "response", response
                );
            }

            public Data respond(JsonObject response) {
                this.response.put("response", response);
                return this;
            }

            public Data failure(Throwable ex) {
                this.response.put("error", DomainError.dumpJsonObject(ex));
                return this;
            }
        }

    }

    record AuditResponseAuditor(PubSub.Publish<AuditResponse> p) {
        public <R extends Data> Future<R> invoke(String topic, Long actor, Future<R> action) {
            var b = new AuditResponse.Data(Status.RESPONSE, actor == null ? -1 : actor, topic, JsonObject.of());
            return action.onSuccess(v -> p.accept(b.respond(v.toJson())))
                    .onFailure(v -> p.accept(b.failure(v)));
        }

        public <R> Future<R> invoke(String topic, Long actor, Function<R, JsonObject> conv, Future<R> action) {
            var b = new AuditResponse.Data(Status.RESPONSE, actor == null ? -1 : actor, topic, JsonObject.of());
            return action.onSuccess(v -> p.accept(b.respond(conv.apply(v))))
                    .onFailure(v -> p.accept(b.failure(v)));
        }

    }

    @Enhance
    @Describe("_AUDITS_AUDIT_INVOKE")
    interface AuditInvoke extends Event.EnumBased<Status> {
        @Describe("_AUDITS_AUDIT_TOPIC")
        String topic();

        @Describe("_AUDITS_AUDIT_REQUEST")
        JsonObject request();

        @Describe("_AUDITS_AUDIT_RESPONSE")
        JsonObject response();

        @Describe("_AUDITS_AUDIT_ACTOR")
        long actor();

        record Data(
                @With Status kind,
                long actor,
                String topic,
                JsonObject request,
                JsonObject response) implements AuditInvoke {
            @Override
            public JsonObject asJson() {
                return JsonObject.of(
                        "kind", kind.ordinal(),
                        "topic", topic,
                        "actor", actor,
                        "request", request,
                        "response", response
                );
            }

            public AuditInvoke.Data respond(JsonObject response) {
                this.response.put("response", response);
                return this;
            }

            public AuditInvoke.Data failure(Throwable ex) {
                this.response.put("error", DomainError.dumpJsonObject(ex));
                return withKind(Status.FAILURE);
            }
        }
    }

    record AuditInvokeAuditor(PubSub.Publish<AuditInvoke> p) {
        public <R extends Data> Future<R> invoke(String topic, Long actor, JsonObject request, Future<R> action) {
            var b = new AuditInvoke.Data(Status.SUCCESS, actor == null ? -1 : actor, topic, JsonObject.of("request", request), JsonObject.of());
            return action.onSuccess(v -> p.accept(b.respond(v.toJson())))
                    .onFailure(v -> p.accept(b.failure(v)));
        }

        public <R> Future<R> invoke(String topic, Long actor, JsonObject request, Function<R, JsonObject> conv, Future<R> action) {
            var b = new AuditInvoke.Data(Status.SUCCESS, actor == null ? -1 : actor, topic, JsonObject.of("request", request), JsonObject.of());
            return action.onSuccess(v -> p.accept(b.respond(conv.apply(v))))
                    .onFailure(v -> p.accept(b.failure(v)));
        }

        public <R> Future<R> failure(String topic, Long actor, JsonObject request, Future<R> action) {
            var b = new AuditInvoke.Data(Status.FAILURE, actor == null ? -1 : actor, topic, JsonObject.of("request", request), JsonObject.of());
            return action.onFailure(v -> p.accept(b.failure(v)));
        }
    }

    Future<Optional<Audit>> identity(long id);

    @Enhance
    interface Contextual extends Audits,Domain.Context{
        @Describe("_AUDITS_ACT_SUBSCRIBE_REQUEST")
        @Subscribe
        default void onRequest(AuditRequest event) {
            throw new UnsupportedOperationException("not yet implemented");
        }

        @Describe("_AUDITS_ACT_SUBSCRIBE_RESPONSE")
        @Subscribe
        default void onResponse(AuditResponse event) {
            throw new UnsupportedOperationException("not yet implemented");
        }

        @Describe("_AUDITS_ACT_SUBSCRIBE_INVOKE")
        @Subscribe
        default void onInvoke(AuditInvoke event) {
            throw new UnsupportedOperationException("not yet implemented");
        }

        @Storage("/schema/audits/audit")
        default Store<Audit> audits() {
            throw new UnsupportedOperationException("not yet implemented");
        }
    }
}

"""
//endregion code
//@formatter:on
                ),
                //endregion file
                JavaFileObjects.forSourceString("vat.foundation.users.api.Users",
//@formatter:off
//region code
//language=java
 """
package vat.foundation.users.api;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.SqlConnection;
import org.jetbrains.annotations.Nullable;
import vat.api.*;
import vat.api.Record;
import vat.api.meta.*;
import vat.api.Entity.Entry;
import vat.api.trait.History;
import vat.api.utils.Fn;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

///
/// @author Zen.Liu
/// @since 2025-11-04

@Enhance
@Describe("_USERS")
public interface Users extends Activities {
    @Enhance
    @Describe(value = "_USERS_USER", domain = Users.class)
    @Table("foundation_users_user")
    interface User extends Actor.Base {
        @Describe("_USERS_USER_PROFILE")
        JsonObject profile();
    }

    @Enhance
    @Describe(value = "_USERS_CERTIFICATE", identity = "certIdentity", domain = Users.class)
    @Table("foundation_users_certificate")
    interface Certificate extends Record.Base, History {
        @Describe("_USERS_CERTIFICATE_USER")
        @Identity.Reference(value = User.class, provider = Users.class)
        @Column(indexed = {"user"})
        long user();

        @Describe("_USERS_CERTIFICATE_KIND")
        @Column(unique = {"identifier"})
        int kind();

        @Describe("_USERS_CERTIFICATE_IDENTIFIER")
        @Column(size = 128)
        String identifier();

        @Describe("_USERS_CERTIFICATE_CERTIFICATE")
        JsonObject certificate();

    }

    @Enhance
    @Describe("_USERS_CHANGES")
    interface Changes extends Event.EnumBased<Changes.ChangeKind> {
        @Describe("_USERS_CHANGES_KIND")
        enum ChangeKind {
            @Describe("_USERS_CHANGES_KIND_REMOVED")
            REMOVED,
            @Describe("_USERS_CHANGES_KIND_CREATED")
            CREATED,
            @Describe("_USERS_CHANGES_KIND_CHANGED")
            CHANGED,
            @Describe("_USERS_CHANGES_KIND_LOGOUT")
            LOGOUT,
            @Describe("_USERS_CHANGES_KIND_LOGIN")
            LOGIN,
            @Describe("_USERS_CHANGES_KIND_PROFILE_CHANGED")
            PROFILE_CHANGED
        }

        @Describe("_USERS_CHANGES_USER")
        long user();

        @Describe(value = "_USERS_CHANGES_CERT", desc = "_DESC_USERS_CHANGES_CERT")
        long cert();
    }

    //region action

    /// 标准方法
    @Describe("_USERS_ACT_USER_IDENTITY")
    Future<Optional<User>> identity(long id);

    @Describe("_USERS_ACT_CERT_IDENTITY")
    Future<Optional<Certificate>> certIdentity(long id);

    @Enhance
    @Describe("_USERS_CERT")
    interface Cert extends Data {
        @Describe("_USERS_CERTIFICATE_KIND")
        int kind();

        @Validate(value = "NONE_BLANK", construct = true)
        @Describe("_USERS_CERTIFICATE_IDENTIFIER")
        String identifier();

        @Validate(value = "NONE_EMPTY_JSON", construct = true)
        @Describe("_USERS_CERTIFICATE_CERTIFICATE")
        JsonObject secret();
    }

    @Auditing(mode = Auditing.Mode.FAILURE)
    @Describe("_USERS_REGISTER")
    Future<User> register(Cert cert);

    @Enhance
    @Describe("_USERS_CERT_UPDATE")
    interface CertUpdate extends Data {
        @Describe("_USERS_CERTIFICATE_KIND")
        int kind();

        @Validate(value = "NONE_BLANK", construct = true)
        @Describe("_USERS_CERTIFICATE_IDENTIFIER")
        String identifier();

        @Validate(value = "NONE_EMPTY_JSON", construct = true)
        @Describe("_USERS_CERT_UPDATE_OLD_CERTIFICATE")
        JsonObject secret();

        @Validate(value = "NONE_EMPTY_JSON", construct = true)
        @Describe("_USERS_CERT_UPDATE_NEW_CERTIFICATE")
        JsonObject newSecret();
    }

    @Describe("_USERS_CERTIFICATION_CHECK")
    Future<Optional<User>> check(Cert cert);

    @Describe("_USERS_CERTIFICATION")
    @Auditing(mode = Auditing.Mode.FAILURE)
    Future<User> certificate(Cert cert);

    @Enhance
    @Describe("_USERS_PROFILE_UPDATE")
    interface ProfileUpdate extends Data.Request<ProfileUpdate>, Entry {
        @Describe("_USERS_PROFILE_UPDATE_PATH")
        Optional<List<String>> path();

        @Describe("_USERS_PROFILE_UPDATE_DATA")
        JsonObject data();

        @Override
        default void doValidate() {
            if (data() == null) throw DomainError.System.badRequest("data required");
            if (version() < 0) throw DomainError.System.badRequest("version required");
            if (id() <= 0) throw DomainError.System.badRequest("id required");
        }
    }

    @Enhance
    @Describe("_USERS_ACT_PROFILE_READ")
    interface ProfileRead extends Data.Request<ProfileRead> {
        long id();
        @Describe(value = "_USERS_PROFILE_READ_PATH")
        Optional<JsonArray> path();


        @Override
        default void doValidate() {
            if (id() <= 0) throw DomainError.System.badRequest("id required");
        }
    }

    @Describe("_USERS_UPDATE_PROFILE")
    @Auditing(mode = Auditing.Mode.FAILURE)
    Future<User> updateProfile(ProfileUpdate req);

    @Describe("_USERS_ACT_READ_PROFILE")
    @Auditing(mode = Auditing.Mode.FAILURE)
    Future<JsonObject> readProfile(ProfileRead req);

    /// Wrapper to read a Data object
    default <T extends Data> Future<T> readProfile(ProfileRead req, Function<JsonObject, T> read) {
        return readProfile(req)
                .map(read)
                .map(Fn.peek(v -> {
                    if (v instanceof Data.Validation<?> val) val.doValidate();
                }));
    }
    //endregion

    @Enhance
    interface Contextual extends Users, Domain.Context {
        //region context
        @Storage("/schema/users/user")
        default Store<User> users(@Nullable SqlConnection tx) {
            throw new IllegalStateException("Not Implemented");
        }

        @Storage("/schema/users/certificate")
        default Store<Certificate> certs(@Nullable SqlConnection tx) {
            throw new IllegalStateException("Not Implemented");
        }
        @Config
        default boolean debug(){
            return  false;
        }
        @Publish
        default void changed(Consumer<Changes> consumer) {
        }

        @Errors
        default DomainError alreadyRegistered() {
            return DomainError.User.badRequestNotify("已经注册");
        }

        @Errors
        default DomainError unsupportedKind() {
            return DomainError.User.badRequestNotify("不支持的方式");
        }

        @Errors
        default DomainError invalidCertificate() {
            return DomainError.User.unauthorizedNotify("请检查重试");
        }
        //endregion

    }
}
"""
//endregion code
//@formatter:on
                )

        ));
    }

    @Test
    void generateAuditing() {
        testing(List.of(

                JavaFileObjects.forSourceString("vat.foundation.audits.api.Audits",
//@formatter:off
//language=java
"""
package vat.foundation.audits.api;

import vat.api.*;
import vat.api.Record;
import vat.api.meta.*;
import vat.api.implement.PubSub;
import vat.api.trait.History;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import lombok.With;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author Zen.Liu
 * @since 2025-10-20
 */
@Enhance
@Describe("_AUDITS")
public interface Audits extends Activities {
       @Describe("_AUDIT_RESULT")
    enum Result {
        @Describe("_AUDIT_RESULT_TODO")
        TODO,
        @Describe("_AUDIT_RESULT_SAFE")
        SAFE,
        @Describe("_AUDIT_RESULT_WARN")
        WARN,
        @Describe("_AUDIT_RESULT_ERROR")
        ERROR,
        @Describe("_AUDIT_RESULT_FATAL")
        FATAL,
        @Describe("_AUDIT_RESULT_DONE")
        DONE,
    }

    @Enhance
    @Describe(value = "_AUDITS_AUDIT", domain = Audit.class)
    @Table("foundation_audits_audit")
    interface Audit extends Record.Base, History {
        @Describe("_AUDITS_AUDIT_TOPIC")
        @Column(size = 255, indexed = {"topic"})
        String topic();

        @Describe("_AUDITS_AUDIT_REQUEST")
        @Nullable
        JsonObject request();

        @Describe("_AUDITS_AUDIT_RESPONSE")
        @Nullable
        JsonObject response();

        @Describe("_AUDITS_AUDIT_STATUS")
        @Column(indexed = {"status"})
        Status status();


        @Describe("_AUDITS_AUDIT_ACTOR")
        long actor();

        @Describe("_AUDITS_AUDIT_REPORTED")
        long reported();

        @Describe("_AUDITS_AUDIT_AUDITED")
        long audited();

        @Describe("_AUDITS_AUDIT_AUDITOR")
        long auditor();

        @Describe("_AUDITS_AUDIT_RESULT")
        @Column(indexed = {"result"})
        Result result();

        @Describe("_AUDITS_AUDIT_COMMENT")
        @Column(size = 1024)
        String comment();

    }

    @Describe("_AUDITS_STATUS")
    enum Status {
        @Describe("_AUDITS_STATUS_REQUEST")
        REQUEST,
        @Describe("_AUDITS_STATUS_RESPONSE")
        RESPONSE,
        @Describe("_AUDITS_STATUS_SUCCESS")
        SUCCESS,
        @Describe("_AUDITS_STATUS_FAILURE")
        FAILURE
    }

    @Enhance
    @Describe("_AUDITS_AUDIT_REQUEST")
    interface AuditRequest extends Event.EnumBased<Status> {
        @Describe("_AUDITS_AUDIT_TOPIC")
        String topic();

        @Describe("_AUDITS_AUDIT_REQUEST")
        JsonObject request();

        @Describe("_AUDITS_AUDIT_ACTOR")
        long actor();

        record Data(Status kind,
                    long actor,
                    String topic,
                    @With JsonObject request) implements AuditRequest {
            @Override
            public JsonObject asJson() {
                return JsonObject.of(
                        "kind", kind.ordinal(),
                        "topic", topic,
                        "actor", actor,
                        "request", request
                );
            }

            public AuditRequest add(Throwable ex) {
                return withRequest(request.put("error", DomainError.dumpJsonObject(ex)));
            }
        }

    }

    record AuditRequestAuditor(PubSub.Publish<AuditRequest> p) {
        public <R> Future<R> invoke(String topic, Long actor, JsonObject request, Future<R> action) {
            var b = new AuditRequest.Data(
                    Status.REQUEST
                    , actor == null ? -1 : actor
                    , topic
                    , JsonObject.of("request", request)
            );
            return action.onSuccess(v -> p.accept(b))
                    .onFailure(v -> p.accept(b.add(v)));
        }
    }

    @Enhance
    @Describe("_AUDITS_AUDIT_RESPONSE")
    interface AuditResponse extends Event.EnumBased<Status> {
        @Describe("_AUDITS_AUDIT_TOPIC")
        String topic();

        @Describe("_AUDITS_AUDIT_RESPONSE")
        JsonObject response();

        @Describe("_AUDITS_AUDIT_ACTOR")
        long actor();


        record Data(Status kind,
                    long actor,
                    String topic,
                    JsonObject response) implements AuditResponse {
            @Override
            public JsonObject asJson() {
                return JsonObject.of(
                        "kind", kind.ordinal(),
                        "topic", topic,
                        "actor", actor,
                        "response", response
                );
            }

            public Data respond(JsonObject response) {
                this.response.put("response", response);
                return this;
            }

            public Data failure(Throwable ex) {
                this.response.put("error", DomainError.dumpJsonObject(ex));
                return this;
            }
        }

    }

    record AuditResponseAuditor(PubSub.Publish<AuditResponse> p) {
        public <R extends Data> Future<R> invoke(String topic, Long actor, Future<R> action) {
            var b = new AuditResponse.Data(Status.RESPONSE, actor == null ? -1 : actor, topic, JsonObject.of());
            return action.onSuccess(v -> p.accept(b.respond(v.toJson())))
                    .onFailure(v -> p.accept(b.failure(v)));
        }

        public <R> Future<R> invoke(String topic, Long actor, Function<R, JsonObject> conv, Future<R> action) {
            var b = new AuditResponse.Data(Status.RESPONSE, actor == null ? -1 : actor, topic, JsonObject.of());
            return action.onSuccess(v -> p.accept(b.respond(conv.apply(v))))
                    .onFailure(v -> p.accept(b.failure(v)));
        }
    }

    @Enhance
    @Describe("_AUDITS_AUDIT_INVOKE")
    interface AuditInvoke extends Event.EnumBased<Status> {
        @Describe("_AUDITS_AUDIT_TOPIC")
        String topic();

        @Describe("_AUDITS_AUDIT_REQUEST")
        JsonObject request();

        @Describe("_AUDITS_AUDIT_RESPONSE")
        JsonObject response();

        @Describe("_AUDITS_AUDIT_ACTOR")
        long actor();

        record Data(
                @With Status kind,
                long actor,
                String topic,
                JsonObject request,
                JsonObject response) implements AuditInvoke {
            @Override
            public JsonObject asJson() {
                return JsonObject.of(
                        "kind", kind.ordinal(),
                        "topic", topic,
                        "actor", actor,
                        "request", request,
                        "response", response
                );
            }

            public AuditInvoke.Data respond(JsonObject response) {
                this.response.put("response", response);
                return this;
            }

            public AuditInvoke.Data failure(Throwable ex) {
                this.response.put("error", DomainError.dumpJsonObject(ex));
                return withKind(Status.FAILURE);
            }
        }
    }

    record AuditInvokeAuditor(PubSub.Publish<AuditInvoke> p) {
        public <R extends Data> Future<R> invoke(String topic, Long actor, JsonObject request, Future<R> action) {
            var b = new AuditInvoke.Data(Status.SUCCESS, actor == null ? -1 : actor, topic, JsonObject.of("request", request), JsonObject.of());
            return action.onSuccess(v -> p.accept(b.respond(v.toJson())))
                    .onFailure(v -> p.accept(b.failure(v)));
        }

        public <R> Future<R> invoke(String topic, Long actor, JsonObject request, Function<R, JsonObject> conv, Future<R> action) {
            var b = new AuditInvoke.Data(Status.SUCCESS, actor == null ? -1 : actor, topic, JsonObject.of("request", request), JsonObject.of());
            return action.onSuccess(v -> p.accept(b.respond(conv.apply(v))))
                    .onFailure(v -> p.accept(b.failure(v)));
        }

        public <R> Future<R> failure(String topic, Long actor, JsonObject request, Future<R> action) {
            var b = new AuditInvoke.Data(Status.FAILURE, actor == null ? -1 : actor, topic, JsonObject.of("request", request), JsonObject.of());
            return action.onFailure(v -> p.accept(b.failure(v)));
        }
    }

    @Describe("_AUDITS_ACT_SUBSCRIBE_REQUEST")
    @Subscribe
    default void onRequest(AuditRequest event) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Describe("_AUDITS_ACT_SUBSCRIBE_RESPONSE")
    @Subscribe
    default void onResponse(AuditResponse event) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Describe("_AUDITS_ACT_SUBSCRIBE_INVOKE")
    @Subscribe
    default void onInvoke(AuditInvoke event) {
        throw new UnsupportedOperationException("not yet implemented");
    }


    @Storage("/schema/audits/audit")
    default Store<Audit> audits() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    Future<Optional<Audit>> identity(long id);
    
    @Enhance
    @Describe("_AUDITS_AUDIT_INVOKE")
    interface Audited extends Data.Request<Audited>,Entity.Entry {
        
    }
    @Auditing(mode = Auditing.Mode.FAILURE,topic = "审计::审计错误")
    Future<Void> audit(Audited data);
     @Auditing(mode = Auditing.Mode.INVOKE,topic = "审计::审计调用")
    Future<Void> audited(Audited data);
    @Auditing(mode = Auditing.Mode.REQUEST,topic = "审计::审计请求")
    Future<Void> auditRequest(Audited data);
    @Auditing(mode = Auditing.Mode.REQUEST,topic = "审计::审计响应")
    Future<Audit> auditResponse(Audited data);
}

"""
//@formatter:on
                )
        ));
    }

    @Test
    void generateMeta() {
        testing(List.of(

                JavaFileObjects.forSourceString("vat.api.metadata.MetaData",
//@formatter:off
//region code
//language=java
"""
package vat.api.metadata;

import vat.api.Data;
import vat.api.meta.Computed;
import vat.api.meta.Enhance;
import io.vertx.core.json.JsonObject;

import java.util.List;

///
/// @author Zen.Liu
/// @since 2025-11-05


public interface MetaData {
    String identity();

    String name();

    String description();

    MetaData identity(String v);

    MetaData name(String v);

    MetaData description(String v);


    interface Type extends Data, MetaData {

    }


    record NumericType(int bits, boolean floatingPoint, String name, String identity,
                       String description) implements Type {
        public NumericType(JsonObject j) {
            this(j.getInteger("bits")
                    , j.getBoolean("floatingPoint", false)
                    , j.getString("name")
                    , j.getString("identity")
                    , j.getString("description"));
        }

        @Override
        public JsonObject asJson() {
            return JsonObject.of(
                    "name", name,
                    "identity", identity,
                    "description", description,
                    "bits", bits,
                    "floatingPoint", floatingPoint
            );
        }

        @Override
        public MetaData identity(String v) {
            throw new UnsupportedOperationException("not supported");
        }

        @Override
        public MetaData name(String v) {
            throw new UnsupportedOperationException("not supported");
        }

        @Override
        public MetaData description(String v) {
            throw new UnsupportedOperationException("not supported");
        }
    }

    NumericType BYTE = new NumericType(8, false, "byte", "byte", "");
    NumericType SHORT = new NumericType(16, false, "short", "short", "");
    NumericType INT = new NumericType(32, false, "int", "int", "");
    NumericType LONG = new NumericType(64, false, "long", "long", "");
    NumericType FLOAT = new NumericType(32, true, "float", "float", "");
    NumericType DOUBLE = new NumericType(64, true, "double", "double", "");

    record NormalType(String name, String identity, String description) implements Type {
        public NormalType(JsonObject j) {
            this(
                    j.getString("name")
                    , j.getString("identity")
                    , j.getString("description"));
        }

        @Override
        public JsonObject asJson() {
            return JsonObject.of(
                    "name", name,
                    "identity", identity,
                    "description", description
            );
        }

        @Override
        public MetaData identity(String v) {
            throw new UnsupportedOperationException("not supported");
        }

        @Override
        public MetaData name(String v) {
            throw new UnsupportedOperationException("not supported");
        }

        @Override
        public MetaData description(String v) {
            throw new UnsupportedOperationException("not supported");
        }
    }

    NormalType BOOLEAN = new NormalType("boolean", "boolean", "");
    NormalType STRING = new NormalType("String", "String", "");
    NormalType BINARY = new NormalType("byte[]", "byte[]", "");
    NormalType BUFFER = new NormalType("Buffer", "Buffer", "");

    interface GenericType extends Type {

    }


    interface RepeatType extends GenericType {
        Type element();

        boolean nullable();
    }

    @Enhance
    interface ListType extends RepeatType {
        boolean unique();

    }

    @Enhance
    interface ArrayType extends RepeatType {

    }

    @Enhance
    interface ProjectionType extends GenericType {
        Type key();

        Type value();

    }

    @Enhance
    interface ReferenceType extends Data, Type {
        String provider();
    }

    @Enhance
    interface EnumerationEntry extends Data, Type {

    }

    @Enhance
    interface EnumerationType extends Data, Type {
        List<EnumerationEntry> candidates();


    }
    @Enhance
    interface Functor extends Data,MetaData{
        boolean construct();
    }
    @Enhance
    interface Property extends Data, MetaData {
        List<Functor> interceptors();

        List<Functor> validators();

        String mappings();

        Type product();

    }

    @Enhance
    interface Column extends Data, MetaData {
        String column();

        List<String> unique();

        List<String> index();

        Integer size();

        Type product();

    }

    interface Properties {
        List<Property> properties();
    }

    interface Entity extends MetaData {
        List<Column> columns();
    }

    @Enhance
    interface Actor extends Data, Entity {
        @Computed
        default String role() {
            return "actor";
        }


    }

    @Enhance
    interface Ability extends Data, Entity {
        @Computed
        default String role() {
            return "ability";
        }
    }

    @Enhance
    interface Record extends Data, Entity {
        @Computed
        default String role() {
            return "record";
        }

    }

    @Enhance
    interface Object extends Data, MetaData, Properties {

    }

    @Enhance
    interface EventKind extends Data, MetaData {
    }

    @Enhance
    interface Event extends Data, MetaData, Properties {
        @Computed
        default String role() {
            return "event";
        }

        List<EventKind> kinds();
    }

    @Enhance
    interface Action extends Data, MetaData {
        Type input();

        Type output();


    }


    interface ConfigEntry extends Data, MetaData {
        String path();

    }

    @Enhance
    interface ErrorEntry extends Data, ConfigEntry {
        default String codePath() {
            return path() + "/code";
        }

        default String userPath() {
            return path() + "/user";
        }

        default String modePath() {
            return path() + "/mode";
        }

        default String systemPath() {
            return path() + "/system";
        }

        List<Type> parameters();
    }

    @Enhance
    interface ValueEntry extends Data, ConfigEntry {
        Type type();

    }


    @Enhance
    interface Config extends Data, MetaData {
        List<ConfigEntry> properties();

    }

    @Enhance
    interface Uses extends Data, MetaData {
        String address();
        String configPath();
    }

    @Enhance
    interface Publish extends Data, MetaData {
        String address();
        String configPath();
    }

    @Enhance
    interface Subscribe extends Data, MetaData {
        String address();
        String configPath();
    }

    @Enhance
    interface Domain extends Data, MetaData {
        List<Actor> actors();

        List<Ability> abilities();

        List<Record> records();

        List<Event> events();

        List<Object> data();

        List<Action> actions();

        List<Publish> publish();

        List<Subscribe> subscribe();

        List<Uses> uses();

        Config config();
        
    }
    
}

"""
//endregion code
//@formatter:on
                )


        ));
    }

    @Test
    void generateConfig() {
        testing(List.of(

                JavaFileObjects.forSourceString("vat.core.ApplicationConfiguration",
//@formatter:off
//region code
//language=java
"""
package vat.core;

import io.vertx.core.json.JsonObject;
import vat.api.Data;
import vat.api.meta.Enhance;

import java.util.List;
import java.util.Optional;

///
/// @author Zen.Liu
/// @since 2025-11-20

@Enhance
public interface ApplicationConfiguration extends Data {

    /// Disabled activities' className
    Optional<List<String>> disabled();

    /// Manually provided activities' className without SPI
    Optional<List<String>> manual();


    Optional<JsonObject> retriever();
}

"""
//endregion code
//@formatter:on
                )


        ));
    }


    @Test
    void any() {
        testing("vat.foundation.audits.api.Audits",
//@formatter:off
//language=java
"""
package vat.foundation.audits.api;

import io.vertx.core.Future;
import vat.api.Activities;
import vat.api.Domain;
import vat.api.trait.Any;
import vat.api.meta.Describe;
import vat.api.meta.Enhance;

/**
* @author Zen.Liu
* @since 2025-10-20
*/
@Enhance
@Describe("_AUDITS")
public interface Audits extends Activities {
    Future<Void> union(@Describe("value of String and number ") Any union);
    @Enhance
    interface Context extends Audits,Domain.Context{
    
    }
}

"""
//@formatter:on
        );
    }

    @Test
    void copier() {
        testing("vat.foundation.audits.api.Audits",
//@formatter:off
//language=java
                """
                package vat.foundation.audits.api;
                
                import io.vertx.core.Future;
                import io.vertx.core.json.JsonObject;
                import vat.api.Activities;
                import vat.api.Domain;
                import vat.api.Data;
                import vat.api.Entity;
                import vat.api.meta.Copier;
                import vat.api.meta.Describe;
                import vat.api.meta.Enhance;
                
                /**
                * @author Zen.Liu
                * @since 2025-10-20
                */
                @Enhance
                @Describe("_AUDITS")
                public interface Audits extends Activities {
       
                    @Enhance
                    @Copier(Entity.Entry.class)
                    interface Target extends Data{
                        @Copier.Process
                        Long id();
                        @Copier.Process(withDefault = true)
                        JsonObject profile();
                    }
                    Future<Void> union(@Describe("value of String and number ") Entity.Entry.entry union);
                    @Enhance
                    interface Context extends Audits,Domain.Context{
                    }
                }
                
                """
//@formatter:on
        );
    }
    @Test
    void crud() {
        testing("vat.foundation.audits.api.Actions",
//@formatter:off
//language=java
"""
package vat.foundation.audits.api;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.SqlConnection;
import net.bytebuddy.build.CachedReturnPlugin;
import vat.api.*;
import vat.api.trait.*;
import vat.api.Record;
import vat.api.meta.*;

import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

@Enhance
@Describe("SomeDomain")
public interface Actions extends Activities {
    @Enhance
    @Table("some_ability")
    interface Role extends Ability.Base{
        JsonObject profile();
    }
    @Enhance
    @Table("some_record")
    @Copier(DataUpdate.class)
    @Copier(value = DataUpdate2.class,name = "update2")
    interface DataRecord extends Record.Base{
        Long user();
        JsonObject profile();
    }

    @Access
    Future<Optional<Role>> authorizeRole(long userId);
    @Access
    Future<Optional<Role>> identityRole(long eid);

    @Enhance
    interface RoleRemove extends Accessor.Remover<RoleRemove>{}

    @Access(entity = Role.class)
    @Authorized(allowSystem = true,value = "REMOVE_AUTHORIZATION")
    Future<Void> removeRole(RoleRemove data);

    @Enhance
    interface DataRemove extends Accessor.Remover<DataRemove>{}

    @Access(entity = DataRecord.class)
    @Authorized(allowSystem = true,value = "DATA_AUTHORIZATION")
    Future<Void> removeDataRecord(DataRemove data);
    @Enhance
    interface DataUpdate extends Accessor.Modificator<DataUpdate>{
           JsonObject profile();
    }
    @Access(entity = DataRecord.class)
    @Authorized("DATA_AUTHORIZATION")
    Future<Void> updateDataRecord(DataUpdate data);
      @Enhance
    interface DataUpdate2 extends Accessor.Modificator<DataUpdate2>{
           long activeUser();
    }
    @Access(entity = DataRecord.class,value = "update2")
    @Authorized("DATA_AUTHORIZATION")
    Future<Void> updateDataRecord2(DataUpdate2 data);
    @Enhance
    interface Context extends Actions,Domain.Context{
        BiPredicate<RoleRemove,Role> REMOVE_AUTHORIZATION=(d,r)->r.profile()!=null&&r.profile().getInteger("permit")>1;
        BiPredicate<Accessor<?>,Role> DATA_AUTHORIZATION=(d,r)->r.profile()!=null&&r.profile().getInteger("permit")>1;

        @Storage
        default Store<DataRecord> records(SqlConnection tx) {
            throw new IllegalStateException();
        }
        @Storage
        default Store<Role> roles(SqlConnection tx) {
            throw new IllegalStateException();
        }

    }
}
"""
//@formatter:on
        );
    }
    Lazy<JavaFileObject> AUDIT=Lazy.of(()->  JavaFileObjects.forSourceString("vat.foundation.audits.api.Audits",
//region CODE
//@formatter:off
//language=java
            """
            package vat.foundation.audits.api;
            
            import io.vertx.core.Future;
            import io.vertx.core.json.JsonObject;
            import lombok.With;
            import org.jetbrains.annotations.Nullable;
            import vat.api.*;
            import vat.api.Record;
            import vat.api.implement.PubSub;
            import vat.api.meta.*;
            import vat.api.trait.History;
            
            import java.util.Optional;
            import java.util.function.Function;
            
            /**
             * @author Zen.Liu
             * @since 2025-10-20
             */
            @Enhance
            @Describe("_AUDITS")
            public interface Audits extends Activities {
                @Describe("_AUDIT_RESULT")
                enum Result {
                    @Describe("_AUDIT_RESULT_TODO")
                    TODO,
                    @Describe("_AUDIT_RESULT_SAFE")
                    SAFE,
                    @Describe("_AUDIT_RESULT_WARN")
                    WARN,
                    @Describe("_AUDIT_RESULT_ERROR")
                    ERROR,
                    @Describe("_AUDIT_RESULT_FATAL")
                    FATAL,
                    @Describe("_AUDIT_RESULT_DONE")
                    DONE,
                }
            
                @Enhance
                @Describe(value = "_AUDITS_AUDIT", domain = Audit.class)
                @Table("foundation_audits_audit")
                interface Audit extends Record.Base, History {
                    @Describe("_AUDITS_AUDIT_TOPIC")
                    @Column(size = 255, indexed = {"topic"})
                    String topic();
            
                    @Describe("_AUDITS_AUDIT_REQUEST")
                    @Nullable
                    JsonObject request();
            
                    @Describe("_AUDITS_AUDIT_RESPONSE")
                    @Nullable
                    JsonObject response();
            
                    @Describe("_AUDITS_AUDIT_STATUS")
                    @Column(indexed = {"status"})
                    Status status();
            
            
                    @Describe("_AUDITS_AUDIT_ACTOR")
                    long actor();
            
                    @Describe("_AUDITS_AUDIT_REPORTED")
                    long reported();
            
                    @Describe("_AUDITS_AUDIT_AUDITED")
                    long audited();
            
                    @Describe("_AUDITS_AUDIT_AUDITOR")
                    long auditor();
            
                    @Describe("_AUDITS_AUDIT_RESULT")
                    @Column(indexed = {"result"})
                    Result result();
            
                    @Describe("_AUDITS_AUDIT_COMMENT")
                    @Column(size = 1024)
                    String comment();
            
                }
            
                @Describe("_AUDITS_STATUS")
                enum Status {
                    @Describe("_AUDITS_STATUS_REQUEST")
                    REQUEST,
                    @Describe("_AUDITS_STATUS_RESPONSE")
                    RESPONSE,
                    @Describe("_AUDITS_STATUS_SUCCESS")
                    SUCCESS,
                    @Describe("_AUDITS_STATUS_FAILURE")
                    FAILURE
                }
            
                @Enhance
                @Describe("_AUDITS_AUDIT_REQUEST")
                interface AuditRequest extends Event.EnumBased<Status> {
                    @Describe("_AUDITS_AUDIT_TOPIC")
                    String topic();
            
                    @Describe("_AUDITS_AUDIT_REQUEST")
                    JsonObject request();
            
                    @Describe("_AUDITS_AUDIT_ACTOR")
                    long actor();
            
                    record Data(Status kind,
                                long actor,
                                String topic,
                                @With JsonObject request) implements AuditRequest {
                        @Override
                        public JsonObject asJson() {
                            return JsonObject.of(
                                    "kind", kind.ordinal(),
                                    "topic", topic,
                                    "actor", actor,
                                    "request", request
                            );
                        }
            
                        public AuditRequest add(Throwable ex) {
                            return withRequest(request.put("error", DomainError.dumpJsonObject(ex)));
                        }
                    }
            
                }
            
                record AuditRequestAuditor(PubSub.Publish<AuditRequest> p) {
                    public <R> Future<R> invoke(String topic, Long actor, JsonObject request, Future<R> action) {
                        var b = new AuditRequest.Data(
                                Status.REQUEST
                                , actor == null ? -1 : actor
                                , topic
                                , JsonObject.of("request", request)
                        );
                        return action.onSuccess(v -> p.accept(b))
                                .onFailure(v -> p.accept(b.add(v)));
                    }
                }
            
                @Enhance
                @Describe("_AUDITS_AUDIT_RESPONSE")
                interface AuditResponse extends Event.EnumBased<Status> {
                    @Describe("_AUDITS_AUDIT_TOPIC")
                    String topic();
            
                    @Describe("_AUDITS_AUDIT_RESPONSE")
                    JsonObject response();
            
                    @Describe("_AUDITS_AUDIT_ACTOR")
                    long actor();
            
            
                    record Data(Status kind,
                                long actor,
                                String topic,
                                JsonObject response) implements AuditResponse {
                        @Override
                        public JsonObject asJson() {
                            return JsonObject.of(
                                    "kind", kind.ordinal(),
                                    "topic", topic,
                                    "actor", actor,
                                    "response", response
                            );
                        }
            
                        public Data respond(JsonObject response) {
                            this.response.put("response", response);
                            return this;
                        }
            
                        public Data failure(Throwable ex) {
                            this.response.put("error", DomainError.dumpJsonObject(ex));
                            return this;
                        }
                    }
            
                }
            
                record AuditResponseAuditor(PubSub.Publish<AuditResponse> p) {
                    public <R extends Data> Future<R> invoke(String topic, Long actor, Future<R> action) {
                        var b = new AuditResponse.Data(Status.RESPONSE, actor == null ? -1 : actor, topic, JsonObject.of());
                        return action.onSuccess(v -> p.accept(b.respond(v.toJson())))
                                .onFailure(v -> p.accept(b.failure(v)));
                    }
            
                    public <R> Future<R> invoke(String topic, Long actor, Function<R, JsonObject> conv, Future<R> action) {
                        var b = new AuditResponse.Data(Status.RESPONSE, actor == null ? -1 : actor, topic, JsonObject.of());
                        return action.onSuccess(v -> p.accept(b.respond(conv.apply(v))))
                                .onFailure(v -> p.accept(b.failure(v)));
                    }
            
                }
            
                @Enhance
                @Describe("_AUDITS_AUDIT_INVOKE")
                interface AuditInvoke extends Event.EnumBased<Status> {
                    @Describe("_AUDITS_AUDIT_TOPIC")
                    String topic();
            
                    @Describe("_AUDITS_AUDIT_REQUEST")
                    JsonObject request();
            
                    @Describe("_AUDITS_AUDIT_RESPONSE")
                    JsonObject response();
            
                    @Describe("_AUDITS_AUDIT_ACTOR")
                    long actor();
            
                    record Data(
                            @With Status kind,
                            long actor,
                            String topic,
                            JsonObject request,
                            JsonObject response) implements AuditInvoke {
                        @Override
                        public JsonObject asJson() {
                            return JsonObject.of(
                                    "kind", kind.ordinal(),
                                    "topic", topic,
                                    "actor", actor,
                                    "request", request,
                                    "response", response
                            );
                        }
            
                        public AuditInvoke.Data respond(JsonObject response) {
                            this.response.put("response", response);
                            return this;
                        }
            
                        public AuditInvoke.Data failure(Throwable ex) {
                            this.response.put("error", DomainError.dumpJsonObject(ex));
                            return withKind(Status.FAILURE);
                        }
                    }
                }
            
                record AuditInvokeAuditor(PubSub.Publish<AuditInvoke> p) {
                    public <R extends Data> Future<R> invoke(String topic, Long actor, JsonObject request, Future<R> action) {
                        var b = new AuditInvoke.Data(Status.SUCCESS, actor == null ? -1 : actor, topic, JsonObject.of("request", request), JsonObject.of());
                        return action.onSuccess(v -> p.accept(b.respond(v.toJson())))
                                .onFailure(v -> p.accept(b.failure(v)));
                    }
            
                    public <R> Future<R> invoke(String topic, Long actor, JsonObject request, Function<R, JsonObject> conv, Future<R> action) {
                        var b = new AuditInvoke.Data(Status.SUCCESS, actor == null ? -1 : actor, topic, JsonObject.of("request", request), JsonObject.of());
                        return action.onSuccess(v -> p.accept(b.respond(conv.apply(v))))
                                .onFailure(v -> p.accept(b.failure(v)));
                    }
            
                    public <R> Future<R> failure(String topic, Long actor, JsonObject request, Future<R> action) {
                        var b = new AuditInvoke.Data(Status.FAILURE, actor == null ? -1 : actor, topic, JsonObject.of("request", request), JsonObject.of());
                        return action.onFailure(v -> p.accept(b.failure(v)));
                    }
                }
                @Access
                Future<Optional<Audit>> identity(long id);
            
                @Enhance
                interface Contextual extends Audits,Domain.Context{
                    @Describe("_AUDITS_ACT_SUBSCRIBE_REQUEST")
                    @Subscribe
                    default void onRequest(AuditRequest event) {
                        throw new UnsupportedOperationException("not yet implemented");
                    }
            
                    @Describe("_AUDITS_ACT_SUBSCRIBE_RESPONSE")
                    @Subscribe
                    default void onResponse(AuditResponse event) {
                        throw new UnsupportedOperationException("not yet implemented");
                    }
            
                    @Describe("_AUDITS_ACT_SUBSCRIBE_INVOKE")
                    @Subscribe
                    default void onInvoke(AuditInvoke event) {
                        throw new UnsupportedOperationException("not yet implemented");
                    }
            
                    @Storage("/schema/audits/audit")
                    default Store<Audit> audits() {
                        throw new UnsupportedOperationException("not yet implemented");
                    }
                }
            }
            """
//@formatter:on
//endregion
    ));
    @Test
    void sample2(){
        testing(List.of(AUDIT.get(),
                JavaFileObjects.forSourceString(
                        "config.config.Configs",
//region CODE
//@formatter:off
//language=java
"""
package config.config;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.SqlConnection;
import vat.api.*;
import vat.api.Record;
import vat.api.meta.*;
import vat.api.trait.Accessor;
import vat.api.trait.ActorUser;
import vat.api.trait.History;
import vat.api.trait.Profile;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;


@Enhance
public interface Configs extends Activities {


    record App(long id, String name) implements Data {
        public App(JsonObject v) {
            this(v.getLong("id"), v.getString("name"));
        }

        @Override
        public JsonObject asJson() {
            return JsonObject.of(
                    "id", id,
                    "name", name
            );
        }
    }

    enum Roles {
        USER, MAINTENANCE, APP, SYSTEM
    }

    @Enhance
    @Table("vat_config_roles")
    interface Role extends Ability.Base, Profile {
        Roles role();

        @Virtual("profile")
        Set<Long> permits();
    }

    @Enhance
    @Table("vat_config_application")
    @Copier(AppCreate.class)
    interface Application extends Record.Base, History, Profile {
        @Column(unique = {"app_id"}, max = 128)
        String appId();

        @Column(max = 64)
        String name();

        @Column(max = 128)
        String description();

        @Virtual("profile")
        Map<String, Configuration> deployment();

        @Copier.Process(withDefault = true)
        @Override
        JsonObject profile();

        @Copier.Process(withDefault = true)
        @Override
        @Nullable
        JsonObject history();
    }

    @Enhance
    @Table("vat_config_secret")
    interface Secret extends Record.Base {
        @Column(unique = {"app_id"}, max = 32)
        String name();

        @Column(max = 128)
        String value();
    }

    @Enhance
    interface Configuration extends Data {
        String deployKey();

        String hash();

        String inputType();

        List<String> arguments();

        String code();

    }

    //region API
    @Enhance
    interface AppCreate extends Accessor.Creator<AppCreate> {
        String appId();

        String name();

        String description();
    }

    @Describe("identity of Role")
    @Access
    Future<Optional<Role>> identityRole(long id);

    @Describe("authorize of Role")
    @Access
    Future<Optional<Role>> authorizeRole(long user);

    @Auditing(mode = Auditing.Mode.FAILURE)
    @Describe("create application")
    @Authorized(allowSystem = true, value = "AUTHORIZE")
    @Access
    Future<Application> create(AppCreate data);
    
    @Auditing(mode = Auditing.Mode.FAILURE)
    @Describe("create application")
    @Authorized(allowSystem = true, value = "AUTHORIZE")
    Future<List<Role>> users(ActorUser data);

    @Enhance
    interface ConfigDeploy extends Accessor.Modificator<ConfigDeploy> {
        Configuration config();
    }

    @Auditing(mode = Auditing.Mode.FAILURE)
    @Describe("deploy application config")
    @Authorized(allowSystem = true, value = "AUTHORIZE")
    Future<Application> deployConfig(ConfigDeploy data);

    //endregion
    @Enhance
    interface Context extends Configs, Domain.Context {
        BiPredicate<Data.Request<?>, Role> AUTHORIZE = (v, i) -> {
            if (i == null) return true;
            return switch (v) {
                case AppCreate a when i.role().ordinal() >= Roles.APP.ordinal() -> true;
                case ConfigDeploy a when i.role().ordinal() >= Roles.MAINTENANCE.ordinal() && i.permits().contains(a.id()) ->
                        true;
                default -> false;
            };
        };
        @Storage
        default Store<Role> roles(@Nullable SqlConnection tx) {
            throw new IllegalStateException("");
        }
        @Storage
        default Store<Application> apps(@Nullable SqlConnection tx) {
            throw new IllegalStateException("");
        }
    }

}
"""
//@formatter:on
//endregion
                )
        ));
    }
}

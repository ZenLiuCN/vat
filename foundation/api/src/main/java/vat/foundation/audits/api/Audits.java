package vat.foundation.audits.api;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import lombok.With;
import org.jspecify.annotations.Nullable;
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
    @Describe(value = "_AUDITS_AUDIT")
    @Identity.Refer(domain = Audits.class)
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
        public <R> Future<R> invoke(String topic, @Nullable Long actor, JsonObject request, Future<R> action) {
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
        public <R extends Data> Future<R> invoke(String topic, @Nullable Long actor, Future<R> action) {
            var b = new AuditResponse.Data(Status.RESPONSE, actor == null ? -1 : actor, topic, JsonObject.of());
            return action.onSuccess(v -> p.accept(b.respond(v.toJson())))
                    .onFailure(v -> p.accept(b.failure(v)));
        }

        public <R> Future<R> invoke(String topic, @Nullable Long actor, Function<R, JsonObject> conv, Future<R> action) {
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
        public <R extends Data> Future<R> invoke(String topic, @Nullable Long actor, JsonObject request, Future<R> action) {
            var b = new AuditInvoke.Data(Status.SUCCESS, actor == null ? -1 : actor, topic, JsonObject.of("request", request), JsonObject.of());
            return action.onSuccess(v -> p.accept(b.respond(v.toJson())))
                    .onFailure(v -> p.accept(b.failure(v)));
        }

        public <R> Future<R> invoke(String topic, @Nullable Long actor, JsonObject request, Function<R, JsonObject> conv, Future<R> action) {
            var b = new AuditInvoke.Data(Status.SUCCESS, actor == null ? -1 : actor, topic, JsonObject.of("request", request), JsonObject.of());
            return action.onSuccess(v -> p.accept(b.respond(conv.apply(v))))
                    .onFailure(v -> p.accept(b.failure(v)));
        }

        public <R> Future<R> failure(String topic, @Nullable Long actor, JsonObject request, Future<R> action) {
            var b = new AuditInvoke.Data(Status.FAILURE, actor == null ? -1 : actor, topic, JsonObject.of("request", request), JsonObject.of());
            return action.onFailure(v -> p.accept(b.failure(v)));
        }
    }

    @Access
    Future<Optional<Audit>> identity(long id);

    @Enhance
    interface Context extends Audits, Domain.Context {
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

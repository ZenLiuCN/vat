package vat.foundation.domain.audits;

import com.google.auto.service.AutoService;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Pool;
import vat.api.Activities;
import vat.api.meta.Activity;
import vat.api.store.Dialect;
import vat.foundation.audits.api.AuditsDomain;
import vat.foundation.audits.api.Codecs;


///
/// @author Zen.Liu
/// @since 2025-11-04
@AutoService(Activities.class)
@Activity(mode = Activity.Mode.FOUNDATION)
public class AuditsImpl extends AuditsDomain<AuditsImpl> {

    public AuditsImpl() {
        super();
    }

    public AuditsImpl(Vertx vertx, String address, Pool sql, Dialect dialect, JsonObject conf) {
        super(vertx, address, sql, dialect, conf);
    }

    @Override
    protected AuditsImpl _this() {
        return this;
    }

    @Override
    public void onInvoke(AuditInvoke event) {
        audits().justPut(event.actor(),
                        Codecs.AUDIT_DATA.get(Codecs.AUDIT_INVOKE_DATA.from(event)
                                        .asJson())
                                .status(event.kind())
                                .asJson()
                )
                .onFailure(ex -> log.error("store event {} failure", event, ex));
    }

    @Override
    public void onRequest(AuditRequest event) {
        audits().justPut(event.actor(),
                        Codecs.AUDIT_DATA.get(Codecs.AUDIT_REQUEST_DATA.from(event).asJson())
                                .status(event.kind())
                                .asJson()
                )
                .onFailure(ex -> log.error("store event {} failure", event, ex));
    }

    @Override
    public void onResponse(AuditResponse event) {
        audits().justPut(event.actor(), Codecs.AUDIT_DATA.get(Codecs.AUDIT_RESPONSE_DATA.from(event).asJson())
                        .status(event.kind())
                        .asJson()
                )
                .onFailure(ex -> log.error("store event {} failure", event, ex));
    }


}

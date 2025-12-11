package vat.core.implement;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Pool;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import vat.api.Activities;
import vat.api.Domain;
import vat.api.implement.BaseActivities;
import vat.api.implement.Web;
import vat.api.store.Dialect;

/// Five activities basic model for manual implements.
/// @author Zen.Liu
/// @since 2025-12-10
public interface Basic {

    abstract class Actor<T extends Actor<T>> extends BaseActivities<T, T> implements Activities {
        protected Actor() {
            super();
        }

        /// The Activities contract is `constructor(vertx,address?)`
        protected Actor(Vertx vertx, @NotNull String address, Logger log) {
            super(vertx, address, log);
        }

        @Override
        public Class<? extends Domain> domainIdentity() {
            return this.getClass();
        }

    }

    abstract class Config<T extends Config<T>> extends BaseActivities<T, T> implements Activities {
        protected Config() {
            super();
            conf = null;
        }

        protected JsonObject conf;

        /// The Activities contract is `constructor(vertx,address?,JsonObject conf)`
        protected Config(Vertx vertx, @NotNull String address, JsonObject conf, Logger log) {
            super(vertx, address, log);
            this.conf = conf;
        }

        @Override
        public Class<? extends Domain> domainIdentity() {
            return this.getClass();
        }

    }

    abstract class Endpoint<T extends Endpoint<T>> extends Config<T> {
        protected Endpoint() {
            super();
        }

        /// The Activities contract is `constructor(vertx,address?,web.factory,conf)`
        protected Endpoint(Vertx vertx, @NotNull String address, Web.Factory web, JsonObject conf, Logger log) {
            super(vertx, address, conf, log);
            routing(web.apply(this));
        }

        abstract protected void routing(Web web);
    }

    abstract class Store<T extends Store<T>> extends Config<T> {
        protected Store() {
            super();
            this.sql = null;
            this.dialect = null;
        }

        protected final Pool sql;
        protected final Dialect dialect;

        /// The Activities contract is `constructor(vertx,address?,sql,dialect,conf)`
        protected Store(Vertx vertx, @NotNull String address, Pool sql, Dialect dialect, JsonObject conf, Logger log) {
            super(vertx, address, conf, log);
            this.sql = sql;
            this.dialect = dialect;

        }


    }

    abstract class EndpointStore<T extends EndpointStore<T>> extends Store<T> {
        protected EndpointStore() {
            super();
        }


        /// The Activities contract is `constructor(vertx,address?, web.factory,sql,dialect,conf)`
        protected EndpointStore(Vertx vertx, @NotNull String address, Web.Factory web, Pool sql, Dialect dialect, JsonObject conf, Logger log) {
            super(vertx, address, sql,dialect,conf, log);
            routing(web.apply(this));

        }
        abstract protected void routing(Web web);

    }
}

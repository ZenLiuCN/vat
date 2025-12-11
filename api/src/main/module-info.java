open module vat.api {
    exports vat.api;
    exports vat.api.utils;
    exports vat.api.implement;
    exports vat.api.store;
    exports vat.api.metadata;
    exports vat.api.trait;
    exports vat.api.meta;
    requires io.vertx.core;
    requires io.netty.buffer;
    requires static com.google.auto.service;
    requires static io.vertx.serviceproxy;
    requires static io.vertx.sql.client;
    requires static io.vertx.web;
    requires static io.netty.codec;
    requires static io.vertx.sql.client.templates;
    requires static lombok;
    requires static org.jetbrains.annotations;
    requires static io.netty.codec.http;
    requires static io.vertx.auth.common;
    requires org.jooq.jool;
    requires org.slf4j;
    requires static io.vertx.auth.jwt;
    requires io.netty.common;
    requires com.carrotsearch.hppc;

}

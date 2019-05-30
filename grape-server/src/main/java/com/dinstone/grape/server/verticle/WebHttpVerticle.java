/*
 * Copyright (C) 2016~2019 dinstone<dinstone@163.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dinstone.grape.server.verticle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dinstone.grape.server.ApplicationContext;
import com.dinstone.grape.server.handler.AccessLogHandler;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

public class WebHttpVerticle extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(WebHttpVerticle.class);

    private ApplicationContext context;

    private JsonObject config;

    public WebHttpVerticle(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public void init(Vertx vertx, Context context) {
        super.init(vertx, context);
        this.config = config();
    }

    @Override
    public void start(Future<Void> startFuture) {
        Router mainRouter = Router.router(vertx);
        mainRouter.route().failureHandler(rc -> {
            LOG.error("failure handle for {}, {}:{}", rc.request().path(), rc.statusCode(), rc.failure());
            if (rc.failure() != null) {
                if (rc.statusCode() == 200) {
                    rc.response().setStatusCode(500).end(rc.failure().getMessage());
                } else {
                    rc.response().end(rc.failure().getMessage());
                }
            } else {
                rc.response().setStatusCode(rc.statusCode()).end();
            }
        });
        mainRouter.route().handler(new AccessLogHandler());
        mainRouter.route("/admin/*").handler(StaticHandler.create().setCachingEnabled(false));

        int serverPort = config.getJsonObject("web", new JsonObject()).getInteger("http.port", 9595);
        HttpServerOptions serverOptions = new HttpServerOptions().setIdleTimeout(180);
        vertx.createHttpServer(serverOptions).connectionHandler(new Handler<HttpConnection>() {
            @Override
            public void handle(HttpConnection hc) {
                LOG.info("Connection {} opened", hc.remoteAddress());
                hc.exceptionHandler(new Handler<Throwable>() {

                    @Override
                    public void handle(Throwable error) {
                        LOG.warn("Connection {} throws : {}", hc.remoteAddress(),
                                error != null ? error.getMessage() : "");
                    }
                });
                hc.closeHandler(new Handler<Void>() {

                    @Override
                    public void handle(Void event) {
                        LOG.info("Connection {} closed", hc.remoteAddress());
                    }
                });
            }
        }).requestHandler(mainRouter).listen(serverPort, ar -> {
            if (ar.succeeded()) {
                LOG.info("start web http success, web.http.port={}", serverPort);
                startFuture.complete();
            } else {
                LOG.error("start web http failed, web.http.port={}", serverPort);
                startFuture.fail(ar.cause());
            }
        });
    }
}

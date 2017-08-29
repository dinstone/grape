/*
 * Copyright (C) 2014~2017 dinstone<dinstone@163.com>
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
import com.dinstone.grape.server.handler.DelayJobHandler;
import com.dinstone.vertx.web.RouterBuilder;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class HttpServerVerticle extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(HttpServerVerticle.class);

    private ApplicationContext context;

    private JsonObject config;

    public HttpServerVerticle(ApplicationContext context) {
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
            LOG.error("handler logic occur error", rc.failure());
            rc.response().end();
        });

        mainRouter.route().handler(new AccessLogHandler());
        mainRouter.route().handler(BodyHandler.create());

        RouterBuilder routerBuilder = RouterBuilder.create(vertx);
        routerBuilder.handler(new DelayJobHandler(context));
        mainRouter.mountSubRouter("/api", routerBuilder.build());

        int serverPort = config.getJsonObject("verticle", new JsonObject()).getInteger("http.port", 9521);
        HttpServerOptions serverOptions = new HttpServerOptions().setIdleTimeout(180);
        vertx.createHttpServer(serverOptions).requestHandler(mainRouter::accept).listen(serverPort, ar -> {
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

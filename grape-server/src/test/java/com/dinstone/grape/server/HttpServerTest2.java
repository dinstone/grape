/*
 * Copyright (C) 2016~2024 dinstone<dinstone@163.com>
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
package com.dinstone.grape.server;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dinstone.grape.server.handler.AccessLogHandler;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class HttpServerTest2 {

    private static final Logger LOG = LoggerFactory.getLogger(HttpServerTest2.class);

    public static void main(String[] args) throws Exception {
        System.setProperty("vertx.disableH2c", "true");

        VertxOptions vertxOptions = new VertxOptions().setWorkerPoolSize(40).setEventLoopPoolSize(4);
        Vertx vertx = VertxHelper.createVertx(vertxOptions);

        for (int i = 0; i < 4; i++) {
            createHttpServerVerticles(vertx);
        }

        System.in.read();
    }

    private static void createHttpServerVerticles(Vertx vertx) {
//        WorkerExecutor workx = vertx.createSharedWorkerExecutor("wbp", 40);

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
        // mainRouter.route().handler(TimeoutHandler.create());
        mainRouter.route().handler(new Handler<RoutingContext>() {

            @Override
            public void handle(RoutingContext rc) {
                HttpServerRequest request = rc.request();
                LOG.info("request = {}, {}", request.path(), request.remoteAddress());

                Handler<Promise<Object>> blockingHandler = f -> {
                    LOG.info("handle = {}, {}", request.path(), request.remoteAddress());
                    f.complete();

                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    LOG.info("release = {}, {}", request.path(), request.remoteAddress());
                };

                Handler<AsyncResult<Object>> resultHandler = ar -> {
                    if (!request.response().closed() && !request.response().ended()) {
                        request.response().end("OK");
                    }
                    LOG.info("response = {}, {}", request.path(), request.remoteAddress());
                };

                vertx.executeBlocking(blockingHandler, false, resultHandler);
            }
        });

        HttpServerOptions serverOptions = new HttpServerOptions().setIdleTimeout(30).setAcceptBacklog(1024);
        HttpServer server = vertx.createHttpServer(serverOptions);
        server.connectionHandler(new Handler<HttpConnection>() {
            AtomicInteger count = new AtomicInteger();

            @Override
            public void handle(HttpConnection hc) {
                LOG.info("connection {} opened, {}", hc.remoteAddress(), count.incrementAndGet());
                hc.exceptionHandler(new Handler<Throwable>() {

                    @Override
                    public void handle(Throwable error) {
                        LOG.warn("connection {} throws : {}", hc.remoteAddress(),
                                error != null ? error.getMessage() : "");
                    }
                });
                hc.closeHandler(new Handler<Void>() {

                    @Override
                    public void handle(Void event) {
                        LOG.info("connection {} closed, {}", hc.remoteAddress(), count.decrementAndGet());
                    }
                });

            }
        });

        server.requestHandler(mainRouter).listen(8081);
        LOG.info("connect to http://localhost:8081");
    }

}

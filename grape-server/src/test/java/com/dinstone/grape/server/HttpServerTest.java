package com.dinstone.grape.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dinstone.grape.server.handler.AccessLogHandler;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class HttpServerTest {

    private static final Logger LOG = LoggerFactory.getLogger(HttpServerTest.class);

    public static void main(String[] args) throws Exception {
        System.setProperty("vertx.disableH2c", "true");

        VertxOptions vertxOptions = new VertxOptions().setWorkerPoolSize(2).setEventLoopPoolSize(3);
        Vertx vertx = VertxHelper.createVertx(vertxOptions);

        Router mainRouter = Router.router(vertx);
        mainRouter.route().failureHandler(rc -> {
            LOG.error("handler logic occur error", rc.failure());
            rc.response().end();
        });
        mainRouter.route().handler(new AccessLogHandler());
        mainRouter.route().handler(new Handler<RoutingContext>() {

            @Override
            public void handle(RoutingContext rc) {
                HttpServerRequest request = rc.request();
                LOG.info("request = " + request.path());
                vertx.executeBlocking(f -> {
                    LOG.info("handle = " + request.path());
                    f.complete();

                    try {
                        Thread.sleep(13000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    LOG.info("release = " + request.path());
                }, false, rh -> {
                    request.response().end("OK");
                    LOG.info("response = " + request.path());
                });
            }
        });

        HttpServerOptions serverOptions = new HttpServerOptions().setIdleTimeout(30).setAcceptBacklog(5);
        HttpServer server = vertx.createHttpServer(serverOptions);
        server.connectionHandler(new Handler<HttpConnection>() {
            @Override
            public void handle(HttpConnection hc) {
                LOG.info("Connection {} opened ", hc.remoteAddress());
                hc.exceptionHandler(new Handler<Throwable>() {

                    @Override
                    public void handle(Throwable error) {
                        LOG.warn("Connection {} errord : {}", hc.remoteAddress(),
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
        });

        server.requestHandler(mainRouter::accept).listen(8081);

        LOG.info("connect to http://localhost:8081");

    }

}

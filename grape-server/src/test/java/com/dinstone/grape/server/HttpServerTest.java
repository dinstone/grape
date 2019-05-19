package com.dinstone.grape.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dinstone.grape.server.handler.AccessLogHandler;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.WorkerExecutor;
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

		VertxOptions vertxOptions = new VertxOptions().setWorkerPoolSize(80).setEventLoopPoolSize(8);
		Vertx vertx = VertxHelper.createVertx(vertxOptions);

		for (int i = 0; i < 2; i++) {
			createHttpServerVerticles(vertx);
		}

		System.in.read();
	}

	private static void createHttpServerVerticles(Vertx vertx) {
		WorkerExecutor workx = vertx.createSharedWorkerExecutor("bwp", 80);

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

				Handler<Future<Object>> blockingHandler = f -> {
					LOG.info("handle = {}, {}", request.path(), request.remoteAddress());
					f.complete();

					try {
						Thread.sleep(1500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					LOG.info("release = {}, {}", request.path(), request.remoteAddress());
				};

				Handler<AsyncResult<Object>> resultHandler = ar -> {
					if (!request.response().ended()) {
						request.response().end("OK");
					}
					LOG.info("response = {}, {}", request.path(), request.remoteAddress());
				};

				if (request.path().contains("wbp")) {
					workx.executeBlocking(blockingHandler, false, resultHandler);
				} else if (request.path().contains("cbp")) {
					vertx.executeBlocking(blockingHandler, false, resultHandler);
				} else {
					if (!request.response().ended()) {
						request.response().end("OK");
					}
					LOG.info("response = {}, {}", request.path(), request.remoteAddress());
				}
			}
		});

		HttpServerOptions serverOptions = new HttpServerOptions().setIdleTimeout(30).setAcceptBacklog(1024);
		HttpServer server = vertx.createHttpServer(serverOptions);
		server.connectionHandler(new Handler<HttpConnection>() {
			@Override
			public void handle(HttpConnection hc) {
				LOG.info("connection {} opened ", hc.remoteAddress());
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
						LOG.info("connection {} closed", hc.remoteAddress());
					}
				});

			}
		});

		server.requestHandler(mainRouter::accept).listen(8081);
		LOG.info("connect to http://localhost:8081");
	}

}

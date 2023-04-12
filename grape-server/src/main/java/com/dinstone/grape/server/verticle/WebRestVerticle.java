/*
 * Copyright (C) 2016~2023 dinstone<dinstone@163.com>
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
import com.dinstone.grape.server.handler.JobApiHandler;
import com.dinstone.grape.server.handler.TubeApiHandler;
import com.dinstone.vertx.web.RouterBuilder;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class WebRestVerticle extends AbstractVerticle {

	private static final Logger LOG = LoggerFactory.getLogger(WebRestVerticle.class);

	private ApplicationContext context;

	private JsonObject config;

	public WebRestVerticle(ApplicationContext context) {
		this.context = context;
	}

	@Override
	public void init(Vertx vertx, Context context) {
		super.init(vertx, context);
		this.config = config();
	}

	@Override
	public void start(Promise<Void> startPromise) {
		Router mainRouter = Router.router(vertx);
		mainRouter.errorHandler(404, rc -> {
			// Send back default 404
			rc.response().setStatusMessage("Not Found").setStatusCode(404);
			if (rc.request().method() == HttpMethod.HEAD) {
				// HEAD responses don't have a body
				rc.response().end();
			} else {
				JsonObject jo = new JsonObject().put("code", 404).put("desc", "invalid path:" + rc.request().path());
				rc.response().putHeader(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=utf-8")
						.end(jo.encode());
			}
		});
		mainRouter.route().failureHandler(failureHandler()).handler(new AccessLogHandler());

		RouterBuilder routerBuilder = RouterBuilder.create(vertx);
		routerBuilder.handler(new JobApiHandler(context));
		routerBuilder.handler(new TubeApiHandler(context));
		mainRouter.mountSubRouter("/api", routerBuilder.build());

		int serverPort = config.getJsonObject("web", new JsonObject()).getInteger("rest.port", 9521);
		HttpServerOptions serverOptions = new HttpServerOptions().setIdleTimeout(180);
		vertx.createHttpServer(serverOptions).requestHandler(mainRouter).listen(serverPort, ar -> {
			if (ar.succeeded()) {
				LOG.info("start web rest success, web.rest.port={}", serverPort);
				startPromise.complete();
			} else {
				LOG.error("start web rest failed, web.rest.port={}", serverPort);
				startPromise.fail(ar.cause());
			}
		});
	}

	private Handler<RoutingContext> failureHandler() {
		return rc -> {
			LOG.error("failure handle for {}, {}:{}", rc.request().path(), rc.statusCode(), rc.failure());
			if (rc.failure() != null) {
				if (rc.statusCode() == 200) {
					rc.response().setStatusCode(500).end(rc.failure().getMessage());
				} else {
					rc.response().setStatusCode(rc.statusCode()).end(rc.failure().getMessage());
				}
			} else {
				rc.response().setStatusCode(rc.statusCode()).end();
			}
		};
	}
}

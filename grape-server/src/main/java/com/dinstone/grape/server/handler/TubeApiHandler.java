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
package com.dinstone.grape.server.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.dinstone.grape.core.Stats;
import com.dinstone.grape.server.ApplicationContext;
import com.dinstone.vertx.web.annotation.Context;
import com.dinstone.vertx.web.annotation.Get;
import com.dinstone.vertx.web.annotation.PathParam;
import com.dinstone.vertx.web.annotation.Post;
import com.dinstone.vertx.web.annotation.Produces;
import com.dinstone.vertx.web.annotation.WebHandler;

import io.vertx.ext.web.RoutingContext;

@WebHandler("/tube")
@Produces({ "application/json" })
public class TubeApiHandler extends RestApiHandler {

	public TubeApiHandler(@Context ApplicationContext context) {
		super(context);
	}

	@Post("/create")
	public void create(@Context RoutingContext ctx) {
		ctx.vertx().executeBlocking(future -> {
			String tubeName = ctx.request().getParam("tube");
			broker.createTube(tubeName);
			future.complete(true);
		}, false, ar -> {
			if (ar.succeeded()) {
				success(ctx, ar.result());
			} else {
				failed(ctx, ar.cause());
			}
		});
	}

	@Post("/delete")
	public void delete(@Context RoutingContext ctx) {
		ctx.vertx().executeBlocking(future -> {
			String tubeName = ctx.request().getParam("tube");
			broker.deleteTube(tubeName);
			future.complete(true);
		}, false, ar -> {
			if (ar.succeeded()) {
				success(ctx, ar.result());
			} else {
				failed(ctx, ar.cause());
			}
		});
	}

	@Get("/list")
	public void list(@Context RoutingContext ctx) {
		ctx.vertx().executeBlocking(future -> {
			future.complete(broker.tubeSet());
		}, false, ar -> {
			if (ar.succeeded()) {
				success(ctx, ar.result());
			} else {
				failed(ctx, ar.cause());
			}
		});
	}

	@Get("/stats")
	public void stats(@Context RoutingContext ctx) {
		ctx.vertx().executeBlocking(future -> {
			List<Stats> stats = new ArrayList<>();
			Set<String> tubes = broker.tubeSet();
			for (String tubeName : tubes) {
				stats.add(broker.stats(tubeName));
			}
			future.complete(stats);
		}, false, ar -> {
			if (ar.succeeded()) {
				success(ctx, ar.result());
			} else {
				failed(ctx, ar.cause());
			}
		});
	}

	@Get("/stats/:tubeName")
	public void stats(@Context RoutingContext ctx, @PathParam("tubeName") String tubeName) {
		ctx.vertx().executeBlocking(future -> {
			future.complete(broker.stats(tubeName));
		}, false, ar -> {
			if (ar.succeeded()) {
				success(ctx, ar.result());
			} else {
				failed(ctx, ar.cause());
			}
		});
	}

}
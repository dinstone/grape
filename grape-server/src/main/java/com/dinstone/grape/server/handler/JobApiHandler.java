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

import java.util.List;

import com.dinstone.grape.core.Job;
import com.dinstone.grape.exception.BusinessException;
import com.dinstone.grape.exception.JobErrorCode;
import com.dinstone.grape.server.ApplicationContext;
import com.dinstone.vertx.web.annotation.Context;
import com.dinstone.vertx.web.annotation.Delete;
import com.dinstone.vertx.web.annotation.Get;
import com.dinstone.vertx.web.annotation.Post;
import com.dinstone.vertx.web.annotation.Produces;
import com.dinstone.vertx.web.annotation.Put;
import com.dinstone.vertx.web.annotation.WebHandler;

import io.vertx.ext.web.RoutingContext;

@WebHandler("/job")
@Produces({ "application/json" })
public class JobApiHandler extends RestApiHandler {

	public JobApiHandler(ApplicationContext context) {
		super(context);
	}

	@Post("/produce")
	public void produce(@Context RoutingContext ctx) {
		String tubeName = ctx.request().getParam("tube");

		String jid = ctx.request().getParam("jid");

		long dtr = 0;
		if (ctx.request().getParam("dtr") != null) {
			try {
				dtr = Long.parseLong(ctx.request().getParam("dtr"));
			} catch (Exception e) {
				failed(ctx, JobErrorCode.DTR_INVALID, "job dtr is invalid");
				return;
			}
		}

		long ttr = 0;
		if (ctx.request().getParam("ttr") != null) {
			try {
				ttr = Long.parseLong(ctx.request().getParam("ttr"));
			} catch (Exception e) {
				failed(ctx, JobErrorCode.TTR_INVALID, "job ttr is invalid");
				return;
			}
		}

		byte[] data = ctx.getBody().getBytes();
		Job job = new Job(jid, dtr, ttr, data);

		ctx.vertx().executeBlocking(future -> {
			future.complete(broker.produce(tubeName, job));
		}, false, ar -> {
			if (ar.succeeded()) {
				success(ctx, ar.result());
			} else {
				failed(ctx, ar.cause());
			}
		});

	}

	@Delete("/delete")
	public void delete(@Context RoutingContext ctx) {
		ctx.vertx().executeBlocking(future -> {
			String tubeName = ctx.request().getParam("tube");
			String jobId = ctx.request().getParam("jid");
			future.complete(broker.delete(tubeName, jobId));
		}, false, ar -> {
			if (ar.succeeded()) {
				success(ctx, ar.result());
			} else {
				failed(ctx, ar.cause());
			}
		});
	}

	@Get("/consume")
	public void consume(@Context RoutingContext ctx) {
		ctx.vertx().executeBlocking(future -> {
			int maxParam = 1;
			try {
				String param = ctx.request().getParam("max");
				if (param != null && param.length() > 0) {
					maxParam = Integer.parseInt(param);
				}
			} catch (Exception e) {
				// ignore
			}
			String tubeName = ctx.request().getParam("tube");
			List<Job> jobs = broker.consume(tubeName, maxParam);
			future.complete(jobs);
		}, false, ar -> {
			if (ar.succeeded()) {
				success(ctx, ar.result());
			} else {
				failed(ctx, ar.cause());
			}
		});
	}

	@Delete("/finish")
	public void finish(@Context RoutingContext ctx) {
		ctx.vertx().executeBlocking(future -> {
			String tubeName = ctx.request().getParam("tube");
			String jobid = ctx.request().getParam("jid");
			future.complete(broker.finish(tubeName, jobid));
		}, false, ar -> {
			if (ar.succeeded()) {
				success(ctx, ar.result());
			} else {
				failed(ctx, ar.cause());
			}
		});
	}

	@Put("/release")
	public void release(@Context RoutingContext ctx) {
		ctx.vertx().executeBlocking(future -> {
			String tubeName = ctx.request().getParam("tube");
			String jobid = ctx.request().getParam("jid");

			long dtr = 0;
			if (ctx.request().getParam("dtr") != null) {
				try {
					dtr = Long.parseLong(ctx.request().getParam("dtr"));
				} catch (Exception e) {
					throw new BusinessException(JobErrorCode.DTR_INVALID, "job dtr is invalid");
				}
			}

			future.complete(broker.release(tubeName, jobid, dtr));
		}, false, ar -> {
			if (ar.succeeded()) {
				success(ctx, ar.result());
			} else {
				failed(ctx, ar.cause());
			}
		});

	}

	@Deprecated
	@Put("/failure")
	public void failure(@Context RoutingContext ctx) {
		bury(ctx);
	}

	@Put("/bury")
	public void bury(@Context RoutingContext ctx) {
		ctx.vertx().executeBlocking(future -> {
			String tubeName = ctx.request().getParam("tube");
			String jobid = ctx.request().getParam("jid");
			future.complete(broker.bury(tubeName, jobid));
		}, false, ar -> {
			if (ar.succeeded()) {
				success(ctx, ar.result());
			} else {
				failed(ctx, ar.cause());
			}
		});
	}

	@Get("/peek")
	public void peek(@Context RoutingContext ctx) {
		ctx.vertx().executeBlocking(future -> {
			String tubeName = ctx.request().getParam("tube");

			long maxParam = 1;
			try {
				String param = ctx.request().getParam("max");
				if (param != null && param.length() > 0) {
					maxParam = Long.parseLong(param);
				}
			} catch (Exception e) {
				// ignore
			}
			List<Job> jobs = broker.peek(tubeName, maxParam);
			future.complete(jobs);
		}, false, ar -> {
			if (ar.succeeded()) {
				success(ctx, ar.result());
			} else {
				failed(ctx, ar.cause());
			}
		});
	}

	@Put("/kick")
	public void kick(@Context RoutingContext ctx) {
		ctx.vertx().executeBlocking(future -> {
			String tubeName = ctx.request().getParam("tube");
			String jid = ctx.request().getParam("jid");

			long dtr = 0;
			if (ctx.request().getParam("dtr") != null) {
				try {
					dtr = Long.parseLong(ctx.request().getParam("dtr"));
				} catch (Exception e) {
					throw new BusinessException(JobErrorCode.DTR_INVALID, "job dtr is invalid");
				}
			}
			try {
				future.complete(broker.kick(tubeName, jid, dtr));
			} catch (Exception e) {
				future.fail(e);
			}
		}, false, ar -> {
			if (ar.succeeded()) {
				success(ctx, ar.result());
			} else {
				failed(ctx, ar.cause());
			}
		});

	}

	@Delete("/discard")
	public void discard(@Context RoutingContext ctx) {
		ctx.vertx().executeBlocking(future -> {
			String tubeName = ctx.request().getParam("tube");
			String jid = ctx.request().getParam("jid");
			future.complete(broker.discard(tubeName, jid));
		}, false, ar -> {
			if (ar.succeeded()) {
				success(ctx, ar.result());
			} else {
				failed(ctx, ar.cause());
			}
		});
	}

}
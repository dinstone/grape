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

package com.dinstone.grape.server.handler;

import java.util.List;

import com.dinstone.grape.core.Broker;
import com.dinstone.grape.core.Job;
import com.dinstone.grape.server.ApplicationContext;
import com.dinstone.vertx.web.annotation.Delete;
import com.dinstone.vertx.web.annotation.Get;
import com.dinstone.vertx.web.annotation.Path;
import com.dinstone.vertx.web.annotation.Post;
import com.dinstone.vertx.web.annotation.Produces;
import com.dinstone.vertx.web.annotation.Put;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@Path("/job")
@Produces({ "application/json" })
public class DelayJobHandler {

    private Broker broker;

    public DelayJobHandler(ApplicationContext context) {
        broker = context.getBroker();
    }

    @Post("/produce")
    public void produce(RoutingContext ctx) {
        String tubeName = ctx.request().getParam("tube");
        if (tubeName == null || tubeName.length() == 0) {
            failed(ctx, "tube is empty");
            return;
        }

        String id = ctx.request().getParam("id");
        if (id == null || id.length() == 0) {
            failed(ctx, "id is empty");
            return;
        }

        long dtr = 0;
        if (ctx.request().getParam("dtr") != null) {
            try {
                dtr = Long.parseLong(ctx.request().getParam("dtr"));
            } catch (Exception e) {
                failed(ctx, "dtr is invalid");
                return;
            }
        }

        long ttr = 0;
        if (ctx.request().getParam("ttr") != null) {
            try {
                ttr = Long.parseLong(ctx.request().getParam("ttr"));
            } catch (Exception e) {
                failed(ctx, "ttr is invalid");
                return;
            }
        }

        byte[] data = ctx.getBody().getBytes();
        Job job = new Job(id, dtr, ttr, data);

        ctx.vertx().executeBlocking(future -> {
            try {
                broker.produce(tubeName, job);

                future.complete();
            } catch (Exception e) {
                future.fail(e);
            }
        }, false, ar -> {
            if (ar.succeeded()) {
                success(ctx);
            } else {
                failed(ctx, ar.cause());
            }
        });

    }

    @SuppressWarnings("unchecked")
    @Get("/consume")
    public void consume(RoutingContext ctx) {
        String tubeName = ctx.request().getParam("tube");
        if (tubeName == null || tubeName.length() == 0) {
            failed(ctx, "tube is empty");
            return;
        }

        long maxParam = 1;
        try {
            String param = ctx.request().getParam("max");
            if (param != null && param.length() > 0) {
                maxParam = Long.parseLong(param);
            }
        } catch (Exception e) {
            // ignore
        }

        final long maxCount = maxParam;
        ctx.vertx().executeBlocking(future -> {
            try {
                List<Job> jobs = broker.consume(tubeName, maxCount);
                future.complete(jobs);
            } catch (Exception e) {
                future.fail(e);
            }
        }, false, ar -> {
            if (ar.succeeded()) {
                JsonArray result = new JsonArray();
                List<Job> jobs = (List<Job>) ar.result();
                if (jobs != null) {
                    result = new JsonArray(Json.encode(jobs));
                }
                success(ctx, result);
            } else {
                failed(ctx, ar.cause());
            }
        });
    }

    @Delete("/finish")
    public void finish(RoutingContext ctx) {
        String tubeName = ctx.request().getParam("tube");
        if (tubeName == null || tubeName.length() == 0) {
            failed(ctx, "tube is empty");
            return;
        }

        String id = ctx.request().getParam("id");
        if (id == null || id.length() == 0) {
            failed(ctx, "id is empty");
            return;
        }

        ctx.vertx().executeBlocking(future -> {
            try {
                broker.finish(tubeName, id);
                future.complete();
            } catch (Exception e) {
                future.fail(e);
            }
        }, false, ar -> {
            if (ar.succeeded()) {
                success(ctx);
            } else {
                failed(ctx, ar.cause());
            }
        });
    }

    @Put("/release")
    public void release(RoutingContext ctx) {
        String tubeName = ctx.request().getParam("tube");
        if (tubeName == null || tubeName.length() == 0) {
            failed(ctx, "tube is empty");
            return;
        }

        String id = ctx.request().getParam("id");
        if (id == null || id.length() == 0) {
            failed(ctx, "id is empty");
            return;
        }

        long dtr = 0;
        if (ctx.request().getParam("dtr") != null) {
            try {
                dtr = Long.parseLong(ctx.request().getParam("dtr"));
            } catch (Exception e) {
                failed(ctx, "dtr is invalid");
                return;
            }
        }

        final long dtrParam = dtr;
        ctx.vertx().executeBlocking(future -> {
            try {
                broker.release(tubeName, id, dtrParam);

                future.complete();
            } catch (Exception e) {
                future.fail(e);
            }
        }, false, ar -> {
            if (ar.succeeded()) {
                success(ctx);
            } else {
                failed(ctx, ar.cause());
            }
        });

    }

    @Put("/failure")
    public void failure(RoutingContext ctx) {
        String tubeName = ctx.request().getParam("tube");
        if (tubeName == null || tubeName.length() == 0) {
            failed(ctx, "tube is empty");
            return;
        }

        String id = ctx.request().getParam("id");
        if (id == null || id.length() == 0) {
            failed(ctx, "id is empty");
            return;
        }

        ctx.vertx().executeBlocking(future -> {
            try {
                broker.failure(tubeName, id);
                future.complete();
            } catch (Exception e) {
                future.fail(e);
            }
        }, false, ar -> {
            if (ar.succeeded()) {
                success(ctx);
            } else {
                failed(ctx, ar.cause());
            }
        });
    }

    private void success(RoutingContext ctx) {
        JsonObject res = new JsonObject().put("code", "1");
        ctx.response().end(res.encode());
    }

    private void success(RoutingContext ctx, JsonArray result) {
        JsonObject res = new JsonObject().put("code", "1");
        if (result != null) {
            res.put("result", result);
        }
        ctx.response().end(res.encode());
    }

    private void failed(RoutingContext ctx, String message) {
        JsonObject res = new JsonObject().put("code", "-1").put("message", message);
        ctx.response().end(res.encode());
    }

    private void failed(RoutingContext ctx, Throwable throwable) {
        JsonObject res = new JsonObject().put("code", "-1").put("message",
            throwable == null ? "" : throwable.getMessage());
        ctx.response().end(res.encode());
    }
}
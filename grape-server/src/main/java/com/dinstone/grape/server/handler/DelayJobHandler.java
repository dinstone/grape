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
package com.dinstone.grape.server.handler;

import java.util.List;

import com.dinstone.grape.core.Job;
import com.dinstone.grape.server.ApplicationContext;
import com.dinstone.vertx.web.annotation.Delete;
import com.dinstone.vertx.web.annotation.Get;
import com.dinstone.vertx.web.annotation.Path;
import com.dinstone.vertx.web.annotation.Post;
import com.dinstone.vertx.web.annotation.Produces;
import com.dinstone.vertx.web.annotation.Put;

import io.vertx.ext.web.RoutingContext;

@Path("/job")
@Produces({ "application/json" })
public class DelayJobHandler extends RestApiHandler {

    public DelayJobHandler(ApplicationContext context) {
        super(context);
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

    @Delete("/delete")
    public void delete(RoutingContext ctx) {
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
                broker.delete(tubeName, id);
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
                success(ctx, ar.result());
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

    @Get("/peek")
    public void peek(RoutingContext ctx) {
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
                List<Job> jobs = broker.peek(tubeName, maxCount);
                future.complete(jobs);
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

    @Put("/kick")
    public void kick(RoutingContext ctx) {
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
                broker.kick(tubeName, id, dtrParam);

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

    @Delete("/discard")
    public void discard(RoutingContext ctx) {
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
                broker.discard(tubeName, id);
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

    @Get("/tubes")
    public void tubes(RoutingContext ctx) {
        ctx.vertx().executeBlocking(future -> {
            try {
                future.complete(broker.tubeSet());
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
}
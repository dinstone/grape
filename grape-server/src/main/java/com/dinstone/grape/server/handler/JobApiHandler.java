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
        if (tubeName == null || tubeName.length() == 0) {
            failed(ctx, ValidErrorCode.TUBE_NAME_EMPTY, "tube name is empty");
            return;
        }

        String id = ctx.request().getParam("id");
        if (id == null || id.length() == 0) {
            failed(ctx, ValidErrorCode.JOB_ID_EMPTY, "job id is empty");
            return;
        }

        long dtr = 0;
        if (ctx.request().getParam("dtr") != null) {
            try {
                dtr = Long.parseLong(ctx.request().getParam("dtr"));
            } catch (Exception e) {
                failed(ctx, ValidErrorCode.JOB_DTR_INVALID, "job dtr is invalid");
                return;
            }
        }

        long ttr = 0;
        if (ctx.request().getParam("ttr") != null) {
            try {
                ttr = Long.parseLong(ctx.request().getParam("ttr"));
            } catch (Exception e) {
                failed(ctx, ValidErrorCode.JOB_TTR_INVALID, "job ttr is invalid");
                return;
            }
        }

        byte[] data = ctx.getBody().getBytes();
        Job job = new Job(id, dtr, ttr, data);

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
        String tubeName = ctx.request().getParam("tube");
        if (tubeName == null || tubeName.length() == 0) {
            failed(ctx, ValidErrorCode.TUBE_NAME_EMPTY, "tube name is empty");
            return;
        }

        String id = ctx.request().getParam("id");
        if (id == null || id.length() == 0) {
            failed(ctx, ValidErrorCode.JOB_ID_EMPTY, "job id is empty");
            return;
        }

        ctx.vertx().executeBlocking(future -> {
            future.complete(broker.delete(tubeName, id));
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
        String tubeName = ctx.request().getParam("tube");
        if (tubeName == null || tubeName.length() == 0) {
            failed(ctx, ValidErrorCode.TUBE_NAME_EMPTY, "tube name is empty");
            return;
        }

        int maxParam = 1;
        try {
            String param = ctx.request().getParam("max");
            if (param != null && param.length() > 0) {
                maxParam = Integer.parseInt(param);
            }
        } catch (Exception e) {
            // ignore
        }

        final int maxCount = maxParam;
        ctx.vertx().executeBlocking(future -> {
            List<Job> jobs = broker.consume(tubeName, maxCount);
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
        String tubeName = ctx.request().getParam("tube");
        if (tubeName == null || tubeName.length() == 0) {
            failed(ctx, ValidErrorCode.TUBE_NAME_EMPTY, "tube name is empty");
            return;
        }

        String id = ctx.request().getParam("id");
        if (id == null || id.length() == 0) {
            failed(ctx, ValidErrorCode.JOB_ID_EMPTY, "job id is empty");
            return;
        }

        ctx.vertx().executeBlocking(future -> {
            future.complete(broker.finish(tubeName, id));
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
        String tubeName = ctx.request().getParam("tube");
        if (tubeName == null || tubeName.length() == 0) {
            failed(ctx, ValidErrorCode.TUBE_NAME_EMPTY, "tube name is empty");
            return;
        }

        String id = ctx.request().getParam("id");
        if (id == null || id.length() == 0) {
            failed(ctx, ValidErrorCode.JOB_ID_EMPTY, "job id is empty");
            return;
        }

        long dtr = 0;
        if (ctx.request().getParam("dtr") != null) {
            try {
                dtr = Long.parseLong(ctx.request().getParam("dtr"));
            } catch (Exception e) {
                failed(ctx, ValidErrorCode.JOB_DTR_INVALID, "job dtr is invalid");
                return;
            }
        }

        final long dtrParam = dtr;
        ctx.vertx().executeBlocking(future -> {
            future.complete(broker.release(tubeName, id, dtrParam));
        }, false, ar -> {
            if (ar.succeeded()) {
                success(ctx, ar.result());
            } else {
                failed(ctx, ar.cause());
            }
        });

    }

    @Put("/failure")
    public void failure(@Context RoutingContext ctx) {
        String tubeName = ctx.request().getParam("tube");
        if (tubeName == null || tubeName.length() == 0) {
            failed(ctx, ValidErrorCode.TUBE_NAME_EMPTY, "tube name is empty");
            return;
        }

        String id = ctx.request().getParam("id");
        if (id == null || id.length() == 0) {
            failed(ctx, ValidErrorCode.JOB_ID_EMPTY, "job id is empty");
            return;
        }

        ctx.vertx().executeBlocking(future -> {
            future.complete(broker.failure(tubeName, id));
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
        String tubeName = ctx.request().getParam("tube");
        if (tubeName == null || tubeName.length() == 0) {
            failed(ctx, ValidErrorCode.TUBE_NAME_EMPTY, "tube name is empty");
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
            List<Job> jobs = broker.peek(tubeName, maxCount);
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
        String tubeName = ctx.request().getParam("tube");
        if (tubeName == null || tubeName.length() == 0) {
            failed(ctx, ValidErrorCode.TUBE_NAME_EMPTY, "tube name is empty");
            return;
        }

        String id = ctx.request().getParam("id");
        if (id == null || id.length() == 0) {
            failed(ctx, ValidErrorCode.JOB_ID_EMPTY, "job id is empty");
            return;
        }

        long dtr = 0;
        if (ctx.request().getParam("dtr") != null) {
            try {
                dtr = Long.parseLong(ctx.request().getParam("dtr"));
            } catch (Exception e) {
                failed(ctx, ValidErrorCode.JOB_DTR_INVALID, "job dtr is invalid");
                return;
            }
        }

        final long dtrParam = dtr;
        ctx.vertx().executeBlocking(future -> {
            try {
                future.complete(broker.kick(tubeName, id, dtrParam));
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
        String tubeName = ctx.request().getParam("tube");
        if (tubeName == null || tubeName.length() == 0) {
            failed(ctx, ValidErrorCode.TUBE_NAME_EMPTY, "tube name is empty");
            return;
        }

        String id = ctx.request().getParam("id");
        if (id == null || id.length() == 0) {
            failed(ctx, ValidErrorCode.JOB_ID_EMPTY, "job id is empty");
            return;
        }

        ctx.vertx().executeBlocking(future -> {
            future.complete(broker.discard(tubeName, id));
        }, false, ar -> {
            if (ar.succeeded()) {
                success(ctx, ar.result());
            } else {
                failed(ctx, ar.cause());
            }
        });
    }

}
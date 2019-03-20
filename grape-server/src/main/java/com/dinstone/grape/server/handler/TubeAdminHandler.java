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

import com.dinstone.grape.server.ApplicationContext;
import com.dinstone.vertx.web.annotation.Get;
import com.dinstone.vertx.web.annotation.Path;
import com.dinstone.vertx.web.annotation.Produces;

import io.vertx.ext.web.RoutingContext;

@Path("/tube")
@Produces({ "application/json" })
public class TubeAdminHandler extends RestApiHandler {

    public TubeAdminHandler(ApplicationContext context) {
        super(context);
    }

    @Get("/set")
    public void set(RoutingContext ctx) {
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

    @Get("/stats")
    public void stats(RoutingContext ctx) {
        String tubeName = ctx.request().getParam("tube");
        if (tubeName == null || tubeName.length() == 0) {
            failed(ctx, "tube is empty");
            return;
        }
        ctx.vertx().executeBlocking(future -> {
            try {
                future.complete(broker.stats(tubeName));
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
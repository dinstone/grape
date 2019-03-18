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
package com.dinstone.grape.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dinstone.grape.server.verticle.GrapeVerticleFactory;
import com.dinstone.grape.server.verticle.HttpServerVerticle;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;

public class ApplicationActivator {

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationActivator.class);

    private Vertx vertx;

    private ApplicationContext context;

    public ApplicationActivator() {
    }

    public void start() throws Exception {
        JsonObject config = ConfigHelper.loadConfig("config-app.json");
        LOG.info("application config :\r\n{}", config.encodePrettily());

        vertx = VertxHelper.createVertx(loadVertxOptions(config));

        context = new ApplicationContext(config);

        GrapeVerticleFactory factory = new GrapeVerticleFactory(context);
        JsonObject vconfig = config.getJsonObject("verticle", new JsonObject());
        int instances = vconfig.getInteger("http.instances", Runtime.getRuntime().availableProcessors());
        DeploymentOptions hvOptions = new DeploymentOptions().setConfig(config).setInstances(instances);
        VertxHelper.deployVerticle(vertx, hvOptions, factory, factory.getVerticleName(HttpServerVerticle.class));
    }

    public void stop() {
        if (vertx != null) {
            vertx.close(ar -> {
                if (context != null) {
                    context.destroy();
                }
            });
        }
    }

    private VertxOptions loadVertxOptions(JsonObject config) {
        JsonObject vertxConfig = config.getJsonObject("vertx", new JsonObject());
        VertxOptions vertxOptions = new VertxOptions().setClustered(false);
        int blockedThreadCheckInterval = vertxConfig.getInteger("blockedThreadCheckInterval", 0);
        if (blockedThreadCheckInterval > 0) {
            vertxOptions.setBlockedThreadCheckInterval(blockedThreadCheckInterval);
        }

        int workerPoolSize = vertxConfig.getInteger("workerPoolSize", Runtime.getRuntime().availableProcessors() + 1);
        if (workerPoolSize > 0) {
            vertxOptions.setWorkerPoolSize(workerPoolSize);
        }

        return vertxOptions;
    }

}

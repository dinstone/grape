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
import com.dinstone.grape.server.verticle.WebHttpVerticle;
import com.dinstone.grape.server.verticle.WebRestVerticle;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;

public class ApplicationActivator {

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationActivator.class);

    private Vertx vertx;

    private ApplicationContext context;

    public void start() throws Exception {
        JsonObject config = ConfigHelper.loadConfig("config.json");
        LOG.info("application config :\r\n{}", config.encodePrettily());

        JsonObject webConfig = config.getJsonObject("web", new JsonObject());
        int restPort = webConfig.getInteger("rest.port", 9521);
        int httpPort = webConfig.getInteger("http.port", 9595);
        if (httpPort == restPort) {
            throw new IllegalStateException("rest.port==http.port");
        }

        config.put("users", ConfigHelper.loadConfig("user.json"));

        context = new ApplicationContext(config);

        vertx = VertxHelper.createVertx(loadVertxOptions(config));

        GrapeVerticleFactory factory = new GrapeVerticleFactory(context);
        JsonObject vconfig = config.getJsonObject("verticle", new JsonObject());

        int instances = vconfig.getInteger("rest.instances", Runtime.getRuntime().availableProcessors());
        DeploymentOptions wrOptions = new DeploymentOptions().setConfig(config).setInstances(instances);
        VertxHelper.deployVerticle(vertx, wrOptions, factory, factory.getVerticleName(WebRestVerticle.class));

        instances = vconfig.getInteger("http.instances", Runtime.getRuntime().availableProcessors());
        if (instances > 0) {
            DeploymentOptions whOptions = new DeploymentOptions().setConfig(config).setInstances(instances);
            VertxHelper.deployVerticle(vertx, whOptions, factory, factory.getVerticleName(WebHttpVerticle.class));
        }
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
        VertxOptions vertxOptions = new VertxOptions();
        vertxOptions.getEventBusOptions().setClustered(false);

        JsonObject vertxConfig = config.getJsonObject("vertx", new JsonObject());
        int blockedThreadCheckInterval = vertxConfig.getInteger("blockedThreadCheckInterval", 1000);
        if (blockedThreadCheckInterval > 0) {
            vertxOptions.setBlockedThreadCheckInterval(blockedThreadCheckInterval);
        }

        int eventLoopPoolSize = vertxConfig.getInteger("eventLoopPoolSize", Runtime.getRuntime().availableProcessors());
        if (eventLoopPoolSize > 0) {
            vertxOptions.setEventLoopPoolSize(eventLoopPoolSize);
        }

        int workerPoolSize = vertxConfig.getInteger("workerPoolSize", Runtime.getRuntime().availableProcessors() + 1);
        if (workerPoolSize > 0) {
            vertxOptions.setWorkerPoolSize(workerPoolSize);
        }

        return vertxOptions;
    }

}

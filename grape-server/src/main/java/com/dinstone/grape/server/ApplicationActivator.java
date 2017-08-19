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

package com.dinstone.grape.server;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dinstone.grape.server.verticle.HttpServerVerticle;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;

public class ApplicationActivator {

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationActivator.class);

    private static final String APPLICATION_HOME = "application.home";

    private Vertx vertx;

    private ApplicationContext context;

    public static void main(String[] args) throws IOException {
        // init log delegate
        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");

        // init applicationHome dir
        String applicationHome = System.getProperty(APPLICATION_HOME);
        if (applicationHome == null || applicationHome.isEmpty()) {
            applicationHome = System.getProperty("user.dir");
            System.setProperty(APPLICATION_HOME, applicationHome);
        }
        LOG.info("application home is {}", applicationHome);

        // launch application activator
        ApplicationActivator vertxApp = new ApplicationActivator();
        try {
            long s = System.currentTimeMillis();
            vertxApp.start();
            long e = System.currentTimeMillis();
            LOG.info("application startup in {} ms.", (e - s));
        } catch (Exception e) {
            LOG.error("application startup error.", e);
            vertxApp.stop();

            System.exit(-1);
        }
    }

    public ApplicationActivator() {
    }

    public void start() throws Exception {
        JsonObject config = ConfigHelper.loadConfig("config-app.json");
        context = new ApplicationContext(config);

        vertx = VertxHelper.createVertx(loadVertxOptions());

        vertx.sharedData().getLocalMap("service").put(ApplicationContext.class.getName(), context);

        int instances = config.getInteger("http.verticle.instances", Runtime.getRuntime().availableProcessors());
        DeploymentOptions hvOptions = new DeploymentOptions().setConfig(config).setInstances(instances);
        VertxHelper.deployVerticle(vertx, hvOptions, HttpServerVerticle.class.getName());
    }

    public void stop() {
        if (vertx != null) {
            vertx.close();
        }
        if (context != null) {
            context.destroy();
        }
    }

    private VertxOptions loadVertxOptions() {
        JsonObject config = ConfigHelper.loadConfig("config-vertx.json");

        JsonObject zkConfig = new JsonObject();
        zkConfig.put("zookeeperHosts", config.getString("zk.hosts"));
        zkConfig.put("rootPath", config.getString("zk.root.path", "vertx/co-channel"));
        JsonObject defRetry = new JsonObject().put("initialSleepTime", 1000).put("maxTimes", 3);
        zkConfig.put("retry", config.getJsonObject("zk.retry", defRetry));

        VertxOptions vertxOptions = new VertxOptions().setClustered(false);
        int blockedThreadCheckInterval = config.getInteger("vertx.blockedThreadCheckInterval", 0);
        if (blockedThreadCheckInterval > 0) {
            vertxOptions.setBlockedThreadCheckInterval(blockedThreadCheckInterval);
        }

        int workerPoolSize = config.getInteger("vertx.workerPoolSize", 0);
        if (workerPoolSize > 0) {
            vertxOptions.setWorkerPoolSize(workerPoolSize);
        }

        return vertxOptions;
    }

}

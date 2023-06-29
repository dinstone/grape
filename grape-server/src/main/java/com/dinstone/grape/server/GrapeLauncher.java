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
package com.dinstone.grape.server;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.logging.SLF4JLogDelegateFactory;

public class GrapeLauncher {

    private static final Logger LOG = LoggerFactory.getLogger(GrapeLauncher.class);

    private static final String APPLICATION_HOME = "application.home";

    public static void main(String[] args) throws IOException {
        // init log delegate
        System.setProperty("vertx.logger-delegate-factory-class-name", SLF4JLogDelegateFactory.class.getName());

        // init applicationHome dir
        String applicationHome = System.getProperty(APPLICATION_HOME);
        if (applicationHome == null || applicationHome.isEmpty()) {
            applicationHome = System.getProperty("user.dir");
            System.setProperty(APPLICATION_HOME, applicationHome);
        }
        LOG.info("application home is {}", applicationHome);

        // launch application activator
        ApplicationActivator activator = new ApplicationActivator();
        try {
            long s = System.currentTimeMillis();
            activator.start();
            long e = System.currentTimeMillis();
            LOG.info("application startup in {} ms.", (e - s));
        } catch (Exception e) {
            LOG.error("application startup error.", e);
            activator.stop();

            System.exit(-1);
        }
    }
}

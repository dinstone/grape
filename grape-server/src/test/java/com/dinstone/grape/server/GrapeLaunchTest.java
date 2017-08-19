
package com.dinstone.grape.server;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrapeLaunchTest {

    private static final Logger LOG = LoggerFactory.getLogger(GrapeLaunch.class);

    private static final String APPLICATION_HOME = "application.home";

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

/*
 * Copyright (C) 2016~2024 dinstone<dinstone@163.com>
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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonObject;

public class ConfigHelper {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigHelper.class);

    public static JsonObject loadConfig(String resourceLocation) {
        BufferedReader reader = null;
        try {
            InputStream resourceStream = getResourceStream(resourceLocation);
            if (resourceStream == null) {
                resourceStream = new FileInputStream(resourceLocation);
            }

            reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(resourceStream), "utf-8"));
            String line = null;
            StringBuilder sb = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            return new JsonObject(sb.toString());
        } catch (IOException e) {
            LOG.error("failed to load config : " + resourceLocation, e);
            throw new RuntimeException("failed to load config : " + resourceLocation, e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private static InputStream getResourceStream(String resource) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = ConfigHelper.class.getClassLoader();
        }
        return classLoader.getResourceAsStream(resource);
    }

}

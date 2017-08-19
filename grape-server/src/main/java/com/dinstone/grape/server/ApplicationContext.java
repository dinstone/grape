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

import com.dinstone.grape.core.Broker;
import com.dinstone.grape.core.Scheduler;

import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.Shareable;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

public class ApplicationContext implements Shareable {

    private final JsonObject config;

    private JedisPool jedisPool;

    private Scheduler scheduler;

    private Broker broker;

    public ApplicationContext(JsonObject config) {
        this.config = config;

        this.jedisPool = initJedisPool(config);

        this.broker = new Broker(jedisPool);

        this.scheduler = new Scheduler(jedisPool);
        this.scheduler.start();
    }

    private JedisPool initJedisPool(JsonObject config) {
        JsonObject redisConfig = config.getJsonObject("redis");

        JedisPoolConfig jedisConfig = new JedisPoolConfig();
        jedisConfig.setMaxTotal(redisConfig.getInteger("MaxTotal", 4));
        jedisConfig.setMinIdle(redisConfig.getInteger("MinIdle", 1));
        jedisConfig.setMaxWaitMillis(redisConfig.getLong("MaxWaitMillis", 5000L));
        jedisConfig.setTestOnBorrow(redisConfig.getBoolean("TestOnBorrow", true));

        jedisConfig.setTestWhileIdle(true);
        jedisConfig.setMinEvictableIdleTimeMillis(redisConfig.getLong("MinEvictableIdleTimeMillis", 60000L));
        jedisConfig.setTimeBetweenEvictionRunsMillis(redisConfig.getLong("TimeBetweenEvictionRunsMillis", 30000L));
        jedisConfig.setNumTestsPerEvictionRun(redisConfig.getInteger("NumTestsPerEvictionRun", -1));

        if (redisConfig.containsKey("Auth")) {
            return new JedisPool(jedisConfig, redisConfig.getString("Host"), redisConfig.getInteger("Port"),
                redisConfig.getInteger("Timeout", Protocol.DEFAULT_TIMEOUT), redisConfig.getString("Auth"));
        } else {
            return new JedisPool(jedisConfig, redisConfig.getString("Host"), redisConfig.getInteger("Port"),
                redisConfig.getInteger("Timeout", Protocol.DEFAULT_TIMEOUT));
        }
    }

    public void destroy() {
        if (scheduler != null) {
            scheduler.stop();
        }
        if (jedisPool != null) {
            jedisPool.destroy();
        }
    }

    public JedisPool getJedisPool() {
        return jedisPool;
    }

    public void setJedisPool(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public Broker getBroker() {
        return broker;
    }

    public void setBroker(Broker broker) {
        this.broker = broker;
    }

    public JsonObject getConfig() {
        return config;
    }

}

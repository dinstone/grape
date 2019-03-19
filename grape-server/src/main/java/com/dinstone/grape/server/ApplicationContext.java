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

import com.dinstone.grape.core.Broker;

import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.Shareable;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

public class ApplicationContext implements Shareable {

	private final JedisPool jedisPool;

	private final Broker broker;

	public ApplicationContext(JsonObject config) {

		this.jedisPool = initJedisPool(config);

		this.broker = new Broker(jedisPool);
		this.broker.start();
	}

	private JedisPool initJedisPool(JsonObject config) {
		JsonObject redisConfig = config.getJsonObject("redis");

		JedisPoolConfig jedisConfig = new JedisPoolConfig();
		jedisConfig.setTestOnBorrow(true);
		jedisConfig.setTestWhileIdle(true);
		jedisConfig.setMinIdle(redisConfig.getInteger("minIdle", 1));
		jedisConfig.setMaxTotal(redisConfig.getInteger("maxTotal", 4));
		jedisConfig.setMaxWaitMillis(redisConfig.getLong("maxWaitMillis", 3000L));
		jedisConfig.setNumTestsPerEvictionRun(redisConfig.getInteger("numTestsPerEvictionRun", -1));
		jedisConfig.setMinEvictableIdleTimeMillis(redisConfig.getLong("minEvictableIdleTimeMillis", 60000L));
		jedisConfig.setTimeBetweenEvictionRunsMillis(redisConfig.getLong("timeBetweenEvictionRunsMillis", 30000L));

		if (redisConfig.containsKey("auth")) {
			return new JedisPool(jedisConfig, redisConfig.getString("host"), redisConfig.getInteger("port"),
					redisConfig.getInteger("timeout", Protocol.DEFAULT_TIMEOUT), redisConfig.getString("auth"));
		} else {
			return new JedisPool(jedisConfig, redisConfig.getString("host"), redisConfig.getInteger("port"),
					redisConfig.getInteger("timeout", Protocol.DEFAULT_TIMEOUT));
		}
	}

	public void destroy() {
		if (broker != null) {
			broker.stop();
		}
		if (jedisPool != null) {
			jedisPool.destroy();
		}
	}

	public JedisPool getJedisPool() {
		return jedisPool;
	}

	public Broker getBroker() {
		return broker;
	}

}

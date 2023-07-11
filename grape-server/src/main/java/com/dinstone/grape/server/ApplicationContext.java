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

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import com.dinstone.grape.core.Broker;
import com.dinstone.grape.exception.ApplicationException;
import com.dinstone.grape.redis.ClusterClient;
import com.dinstone.grape.redis.PooledClient;
import com.dinstone.grape.redis.RedisClient;
import com.dinstone.grape.server.authen.AuthenProvider;
import com.dinstone.grape.server.authen.LocalAuthenProvider;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.Shareable;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

public class ApplicationContext implements Shareable {

	private final Broker broker;

	private final RedisClient redisClient;

	private final AuthenProvider authenProvider;

	public ApplicationContext(JsonObject config) {
		redisClient = initRedisClient(config);

		JsonObject brokerConfig = config.getJsonObject("broker");
		if (brokerConfig == null) {
			this.broker = new Broker(redisClient).start();
		} else {
			String namespace = brokerConfig.getString("namespace");
			int defaultSize = Runtime.getRuntime().availableProcessors();
			int scheduledSize = brokerConfig.getInteger("scheduledSize", defaultSize);
			this.broker = new Broker(redisClient, namespace, scheduledSize).start();
		}

		authenProvider = new LocalAuthenProvider(config.getJsonObject("users"));
	}

	private RedisClient initRedisClient(JsonObject config) {
		JsonObject redisConfig = config.getJsonObject("redis");
		// check nodes
		JsonArray nodes = redisConfig.getJsonArray("nodes");
		if (nodes == null || nodes.isEmpty()) {
			throw new ApplicationException("redis nodes is empty");
		}

		// check jedis pool config
		JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
		jedisPoolConfig.setTestOnBorrow(false);
		jedisPoolConfig.setTestWhileIdle(true);
		jedisPoolConfig.setMinIdle(redisConfig.getInteger("minIdle", 1));
		jedisPoolConfig.setMaxTotal(redisConfig.getInteger("maxTotal", 4));
		jedisPoolConfig.setMaxWait(Duration.ofMillis(redisConfig.getLong("maxWaitMillis", 3000L)));
		jedisPoolConfig.setNumTestsPerEvictionRun(redisConfig.getInteger("numTestsPerEvictionRun", -1));
		jedisPoolConfig
				.setMinEvictableIdleTime(Duration.ofMillis(redisConfig.getLong("minEvictableIdleTimeMillis", 60000L)));
		jedisPoolConfig.setTimeBetweenEvictionRuns(
				Duration.ofMillis(redisConfig.getLong("timeBetweenEvictionRunsMillis", 30000L)));

		// check redis model: cluster or pooled
		String auth = redisConfig.getString("auth");
		int timeout = redisConfig.getInteger("timeout", Protocol.DEFAULT_TIMEOUT);
		if ("cluster".equalsIgnoreCase(redisConfig.getString("model"))) {
			Set<HostAndPort> clusterNodes = new HashSet<HostAndPort>();
			for (int i = 0; i < nodes.size(); i++) {
				JsonObject node = nodes.getJsonObject(i);
				clusterNodes.add(new HostAndPort(node.getString("host"), node.getInteger("port")));
			}

			JedisCluster jedisCluster;
			if (auth == null || auth.isEmpty()) {
				jedisCluster = new JedisCluster(clusterNodes, timeout, timeout, 3, jedisPoolConfig);
			} else {
				jedisCluster = new JedisCluster(clusterNodes, timeout, timeout, 3, auth, jedisPoolConfig);
			}
			return new ClusterClient(jedisCluster);
		} else {
			JsonObject node = nodes.getJsonObject(0);
			JedisPool jedisPool;
			if (auth == null || auth.isEmpty()) {
				jedisPool = new JedisPool(jedisPoolConfig, node.getString("host"), node.getInteger("port"), timeout);
			} else {
				jedisPool = new JedisPool(jedisPoolConfig, node.getString("host"), node.getInteger("port"), timeout,
						auth);
			}
			return new PooledClient(jedisPool);
		}
	}

	public void destroy() {
		if (broker != null) {
			broker.stop();
		}
		if (redisClient != null) {
			redisClient.destroy();
		}
	}

	public Broker getBroker() {
		return broker;
	}

	public AuthenProvider getAuthenProvider() {
		return authenProvider;
	}

}

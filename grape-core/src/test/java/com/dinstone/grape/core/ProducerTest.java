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
package com.dinstone.grape.core;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dinstone.grape.redis.ClusterClient;
import com.dinstone.grape.redis.PooledClient;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class ProducerTest {

	private static final Logger LOG = LoggerFactory.getLogger(ProducerTest.class);

	public static void main(String[] args) throws IOException {
//		testCluster();
		 testPooled();
	}

	public static void testPooled() throws IOException {
		JedisPool jedisPool = new JedisPool("192.168.1.120", 6379);
		Broker broker = new Broker(new PooledClient(jedisPool));
		broker.createTube("test");

		LOG.info("produce job for test start");
		long s = System.currentTimeMillis();
		byte[] bytes = "hello,haha".getBytes();
		for (int i = 0; i < 1000; i++) {
			int dtr = ((i / 100) + 3) * 1000;
			broker.produce("test", new Job("Job-" + i, dtr, 30000, bytes));
		}
		long e = System.currentTimeMillis();
		LOG.info("produce job for test finish, take's {}ms", (e - s));

		jedisPool.destroy();
	}

	public static void testCluster() throws IOException {
		Set<HostAndPort> jedisClusterNode = new HashSet<HostAndPort>();
		jedisClusterNode.add(new HostAndPort("192.168.1.120", 7001));
		jedisClusterNode.add(new HostAndPort("192.168.1.120", 7002));
		jedisClusterNode.add(new HostAndPort("192.168.1.120", 7003));

		JedisPoolConfig config = new JedisPoolConfig();
		config.setMaxTotal(100);
		config.setMaxIdle(10);

		JedisCluster jedisCluster = new JedisCluster(jedisClusterNode, 1000, 3, config);

		Broker broker = new Broker(new ClusterClient(jedisCluster), "grape", 4);
		broker.createTube("test");

		LOG.info("produce job for test start");
		long s = System.currentTimeMillis();
		byte[] bytes = "hello,haha".getBytes();
		for (int i = 0; i < 1000; i++) {
			int dtr = ((i / 100) + 3) * 1000;
			broker.produce("test", new Job("Job-" + i, dtr, 30000, bytes));
		}
		long e = System.currentTimeMillis();
		LOG.info("produce job for test finish, take's {}ms", (e - s));

	}

}

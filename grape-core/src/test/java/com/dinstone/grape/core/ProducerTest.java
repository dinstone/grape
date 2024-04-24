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
package com.dinstone.grape.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dinstone.grape.redis.ClusterClient;
import com.dinstone.grape.redis.PooledClient;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class ProducerTest {

	private static class Producer extends Thread {

		private final AtomicBoolean closed = new AtomicBoolean(false);

		private Broker broker;

		private String tubeName;

		private int index;

		private CountDownLatch downLatch;

		public Producer(int index, String tubeName, Broker broker, CountDownLatch downLatch) {
			this.index = index;
			this.tubeName = tubeName;
			this.broker = broker;
			this.downLatch = downLatch;

			setName("Producer-" + index);
		}

		public void shutdown() {
			closed.set(true);
		}

		@Override
		public void run() {
			int count = 1000;
			byte[] bytes = "hello,haha".getBytes();

			try {
				long s = System.currentTimeMillis();
				String prefix = Long.toString(s) + "-" + index + "-";

				for (int i = 0; i < count; i++) {
					int dtr = ((i / 100) + 3) * count;
					broker.produce(tubeName, new Job(prefix + i, dtr, 30000, bytes));

					if (closed.get()) {
						break;
					}
				}

				long t = System.currentTimeMillis() - s;
				LOG.info("producer[{}] test finish, take's {}ms, tps {}", index, t, count * 1000 / t);

			} finally {
				downLatch.countDown();
			}
		}

	}

	private static final Logger LOG = LoggerFactory.getLogger(ProducerTest.class);

	public static void main(String[] args) throws IOException {
		testCluster();
//		testPooled();
	}

	public static void testPooled() throws IOException {
		JedisPool jedisPool = new JedisPool("192.168.1.120", 6379);
		Broker broker = new Broker(new PooledClient(jedisPool));

		multi(broker);

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
	
		multi(broker);
	
//		single(broker);
	
		jedisCluster.close();
	}

	private static void multi(Broker broker) {
		broker.createTube("test");

		Stats stats = broker.stats("test");
		System.out.println(stats);

		LOG.info("produce start");
		long s = System.currentTimeMillis();

		int count = 10;
		List<Producer> producers = new ArrayList<>(count);
		CountDownLatch downLatch = new CountDownLatch(count);
		for (int i = 0; i < count; i++) {
			Producer target = new Producer(i, "test", broker, downLatch);
			target.start();
			producers.add(target);
		}
		try {
			downLatch.await();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		long t = System.currentTimeMillis() - s;
		LOG.info("produce finish, take's {}ms, tps {}", t, count * 1000 * 1000 / t);

		stats = broker.stats("test");
		System.out.println(stats);

		for (Producer producer : producers) {
			try {
				producer.shutdown();
				producer.join();
			} catch (InterruptedException e) {
			}
		}
	}

	private static void single(Broker broker) {
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

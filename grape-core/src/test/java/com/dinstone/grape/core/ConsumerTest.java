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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dinstone.grape.redis.ClusterClient;
import com.dinstone.grape.redis.PooledClient;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class ConsumerTest {

	public static class Consumer extends Thread {

		private static final Logger LOG = LoggerFactory.getLogger(ConsumerTest.Consumer.class);

		private final AtomicBoolean closed = new AtomicBoolean(false);

		private Broker broker;

		private String tubeName;

		private int index;

		public Consumer(int index, String tubeName, Broker tubeManager) {
			this.index = index;
			this.tubeName = tubeName;
			this.broker = tubeManager;

			setName("Consumer-" + index);
		}

		public void shutdown() {
			closed.set(true);
		}

		@Override
		public void run() {
			while (!closed.get()) {
				List<Job> jobs = broker.consume(tubeName, 50);
				if (jobs == null || jobs.size() == 0) {
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						break;
					}
					continue;
				}

				for (Job job : jobs) {
					broker.finish(tubeName, job.getId());
					LOG.info("consumer:{} handle job[{}:{}]", index, tubeName, job.getId());
				}
			}
		}

	}

	public static void main(String[] args) {
//		clusterBroker();
		pooledBroker();
	}

	private static void clusterBroker() {
		Set<HostAndPort> jedisClusterNode = new HashSet<HostAndPort>();
		jedisClusterNode.add(new HostAndPort("192.168.1.120", 7001));
		jedisClusterNode.add(new HostAndPort("192.168.1.120", 7002));
		jedisClusterNode.add(new HostAndPort("192.168.1.120", 7003));

		JedisPoolConfig config = new JedisPoolConfig();
		config.setMaxTotal(100);
		config.setMaxIdle(10);

		JedisCluster jedisCluster = new JedisCluster(jedisClusterNode, 1000, 3, config);
		Broker tubeManager = new Broker(new ClusterClient(jedisCluster), "grape", 4);

		System.out.println("tubes = " + tubeManager.tubeSet());

		List<Consumer> consumers = new ArrayList<>(3);
		for (int i = 0; i < 3; i++) {
			Consumer target = new Consumer(i, "test", tubeManager);
			target.start();
			consumers.add(target);
		}

		try {
			System.in.read();
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (Consumer consumer : consumers) {
			try {
				consumer.shutdown();
				consumer.join();
			} catch (InterruptedException e) {
			}
		}
	}

	private static void pooledBroker() {
		JedisPool jedisPool = new JedisPool("192.168.1.120", 6379);
		Broker tubeManager = new Broker(new PooledClient(jedisPool));

		List<Consumer> consumers = new ArrayList<>(3);
		for (int i = 0; i < 3; i++) {
			Consumer target = new Consumer(i, "test", tubeManager);
			target.start();
			consumers.add(target);
		}

		try {
			System.in.read();
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (Consumer consumer : consumers) {
			try {
				consumer.shutdown();
				consumer.join();
			} catch (InterruptedException e) {
			}
		}

		jedisPool.destroy();
	}

}

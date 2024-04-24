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
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.dinstone.grape.redis.PooledClient;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class BrokerTest {

	public static void main(String[] args) {
		try {
			new BrokerTest().test();
		} catch (IOException e) {
		}
	}

	@SuppressWarnings("deprecation")
	public void test() throws IOException {
		JedisPool jedisPool = new JedisPool("192.168.1.120", 6379);

		Jedis jedis = jedisPool.getResource();
		try {
			jedis.flushDB();
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}

		Broker broker = new Broker(new PooledClient(jedisPool)).start();

		String tubeName = "flow-test";
		broker.createTube(tubeName);

		boolean b = broker.produce(tubeName, new Job("Job-0", 1000000, 30000, "first job".getBytes()));
		if (!b) {
			System.err.println("produce failed");
			return;
		}
		b = broker.delete(tubeName, "Job-0");
		if (!b) {
			System.err.println("delete failed");
			return;
		}

		b = broker.produce(tubeName, new Job("Job-1", 1000, 30000, "1 job".getBytes()));
		b = broker.produce(tubeName, new Job("Job-2", 1000, 30000, "2 job".getBytes()));
		b = broker.produce(tubeName, new Job("Job-3", 1000, 30000, "3 job".getBytes()));
		b = broker.produce(tubeName, new Job("Job-4", 3000, 3000, "4 job".getBytes()));

		try {
			TimeUnit.SECONDS.sleep(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		List<Job> jobs = broker.consume(tubeName, 10);
		Job job = jobs.get(0);
		broker.finish(tubeName, job.getId());

		job = jobs.get(1);
		broker.bury(tubeName, job.getId());

		job = jobs.get(2);
		broker.release(tubeName, job.getId(), 2000);

		System.out.println(broker.stats(tubeName));

		try {
			TimeUnit.SECONDS.sleep(3);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		System.out.println(broker.stats(tubeName));

		broker.stop();
		jedisPool.destroy();
	}

}

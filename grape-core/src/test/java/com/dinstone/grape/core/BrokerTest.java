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
package com.dinstone.grape.core;

import java.io.IOException;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.JedisPool;

public class BrokerTest {

	private static final Logger LOG = LoggerFactory.getLogger(BrokerTest.class);

	@Test
	public void test() throws IOException {
		JedisPool jedisPool = new JedisPool("127.0.0.1", 6379);
		Broker tubeManager = new Broker(jedisPool);
		tubeManager.start();

		LOG.info("produce job for test,bt01,bt02 begin");

		for (int i = 0; i < 100000; i++) {
			int dtr = ((i / 1000) + 3) * 1000;
			tubeManager.produce("test", new Job("Job-" + i, dtr, 30000, "hello,haha".getBytes()));
			tubeManager.produce("bt01", new Job("Job-" + i, dtr, 30000, "hello,bt01".getBytes()));
			tubeManager.produce("bt02", new Job("Job-" + i, dtr, 30000, "hello,bt01".getBytes()));

			System.out.println(tubeManager.tubeStats("test").toString());
		}

		LOG.info("produce job for test,bt01,bt02 finish");

		tubeManager.stop();
		jedisPool.destroy();
	}

}

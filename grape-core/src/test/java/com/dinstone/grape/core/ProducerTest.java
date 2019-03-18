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

import com.dinstone.grape.core.Broker;
import com.dinstone.grape.core.Job;

import redis.clients.jedis.JedisPool;

public class ProducerTest {

    private static final Logger LOG = LoggerFactory.getLogger(ProducerTest.class);

    @Test
    public void test() throws IOException {
        JedisPool jedisPool = new JedisPool("127.0.0.1", 6379);
        Broker tubeManager = new Broker(jedisPool);

        LOG.info("produce job for test,bt01,bt02");
        for (int i = 0; i < 10000; i++) {
            int dtr = ((i / 100) + 3) * 1000;
            System.out.println("dtr = " + dtr);
            tubeManager.produce("test", new Job("Job-" + i, dtr, 30000, "hello,haha".getBytes()));
        }

        jedisPool.destroy();
    }

}

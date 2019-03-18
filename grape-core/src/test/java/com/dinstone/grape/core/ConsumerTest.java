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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dinstone.grape.core.Broker;
import com.dinstone.grape.core.Job;

import redis.clients.jedis.JedisPool;

public class ConsumerTest {

    public static class Consumer extends Thread {

        private static final Logger LOG = LoggerFactory.getLogger(ConsumerTest.Consumer.class);

        private final AtomicBoolean closed = new AtomicBoolean(false);

        private Broker tubeManager;

        private String tubeName;

        private int index;

        public Consumer(int index, String tubeName, Broker tubeManager) {
            this.index = index;
            this.tubeName = tubeName;
            this.tubeManager = tubeManager;

            setName("Consumer-" + index);
        }

        public void shutdown() {
            closed.set(true);
        }

        @Override
        public void run() {
            while (!closed.get()) {
                List<Job> jobs = tubeManager.consume(tubeName, 50);
                if (jobs == null || jobs.size() == 0) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        break;
                    }
                    continue;
                }

                for (Job job : jobs) {
                    tubeManager.finish(tubeName, job.getId());
                    LOG.info("consumer:{} handle job[{}:{}]", index, tubeName, job.getId());
                }
            }
        }

    }

    public static void main(String[] args) {
        JedisPool jedisPool = new JedisPool("127.0.0.1", 6379);
        Broker tubeManager = new Broker(jedisPool);

        List<Consumer> consumers = new ArrayList<>(3);
        for (int i = 0; i < 3; i++) {
            Consumer target = new Consumer(i, "test", tubeManager);
            target.start();
            consumers.add(target);
        }

        for (int i = 0; i < 2; i++) {
            Consumer target = new Consumer(i, "bt01", tubeManager);
            target.start();
            consumers.add(target);
        }

        for (int i = 0; i < 1; i++) {
            Consumer target = new Consumer(i, "bt02", tubeManager);
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

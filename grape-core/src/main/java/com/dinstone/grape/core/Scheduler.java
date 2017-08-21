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

package com.dinstone.grape.core;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class Scheduler {

    private static final Logger LOG = LoggerFactory.getLogger(Scheduler.class);

    private static final AtomicInteger index = new AtomicInteger();

    private final JedisPool jedisPool;

    private final SchedulerRunner schedulerRunner;

    public Scheduler(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
        this.schedulerRunner = new SchedulerRunner();
    }

    public void start() {
        schedulerRunner.start();
        LOG.info("Scheduler is started");
    }

    public void stop() {
        schedulerRunner.shutdown();
        try {
            schedulerRunner.join();
        } catch (InterruptedException e) {
        }
        LOG.info("Scheduler is shutdown");
    }

    public void schedule() {
        Jedis jedis = jedisPool.getResource();
        try {
            Set<String> tubeSet = jedis.smembers(Tube.TUBE_SET);
            for (String tubeName : tubeSet) {
                RedisLock scheduleLock = new RedisLock(jedisPool, "lock:schedule:" + tubeName, 30);
                if (scheduleLock.acquire()) {
                    try {
                        new Tube(tubeName, jedisPool).schedule();
                    } finally {
                        scheduleLock.release();
                    }
                }
            }
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    private class SchedulerRunner extends Thread {

        private final AtomicBoolean closed = new AtomicBoolean(false);

        public SchedulerRunner() {
            setName("Job-Scheduler-" + index.incrementAndGet());
        }

        @Override
        public void run() {
            while (!closed.get()) {
                try {
                    long s = System.currentTimeMillis();
                    schedule();
                    long e = System.currentTimeMillis();

                    long elapsed = e - s;
                    // LOG.info("schedule take's {}ms", elapsed);

                    if (elapsed < 900) {
                        Thread.sleep(1000 - elapsed);
                    }
                } catch (Exception e) {
                    // Ignore exception if closing
                    if (!closed.get()) {
                        LOG.warn("we captain an exception but scheduler not closed, {}", e.getMessage());
                    }
                }
            }
        }

        // Shutdown hook which can be called from a separate thread
        public void shutdown() {
            closed.set(true);
        }

    }

}

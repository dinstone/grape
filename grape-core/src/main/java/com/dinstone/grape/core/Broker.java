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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dinstone.grape.util.RedisLock;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class Broker {

    private static final Logger LOG = LoggerFactory.getLogger(Broker.class);

    private static final String TUBE_SET = "tube:set";

    private final JedisPool jedisPool;

    private final Map<String, Tube> tubeMap;

    private final Map<String, Runnable> taskMap;

    private final ScheduledExecutorService executor;

    public Broker(JedisPool jedisPool) {
        this(jedisPool, 2);
    }

    public Broker(JedisPool jedisPool, int scheduledSize) {
        if (jedisPool == null) {
            throw new NullPointerException("jedisPool is null");
        }
        if (scheduledSize <= 0) {
            throw new IllegalArgumentException("scheduledSize must be greater than 0");
        }
        this.jedisPool = jedisPool;
        this.tubeMap = new ConcurrentHashMap<>();
        this.taskMap = new ConcurrentHashMap<>();
        this.executor = Executors.newScheduledThreadPool(scheduledSize);
    }

    public Set<String> tubeSet() {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.smembers(TUBE_SET);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    public Stats stats(String tubeName) {
        return loadTube(tubeName).stats();
    }

    private Tube loadTube(String tubeName) {
        Tube tube = tubeMap.get(tubeName);
        if (tube != null) {
            return tube;
        }
        Set<String> tubeSet = tubeSet();
        if (tubeSet.contains(tubeName)) {
            return createTube(tubeName);
        }

        throw new RuntimeException("unkown tube '" + tubeName + "'");
    }

    public Tube createTube(String tubeName) {
        Tube tube = tubeMap.get(tubeName);
        if (tube == null) {
            addTube(tubeName);
            tube = new Tube(tubeName, jedisPool);
            tubeMap.putIfAbsent(tubeName, tube);
            tube = tubeMap.get(tubeName);
        }
        return tube;
    }

    private void addTube(String tubeName) {
        LOG.info("add tube {}", tubeName);
        Jedis jedis = jedisPool.getResource();
        try {
            jedis.sadd(TUBE_SET, tubeName);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    public boolean produce(String tubeName, Job job) {
        if (tubeName == null || tubeName.length() == 0) {
            throw new IllegalArgumentException("tubeName is empty");
        }
        if (job.getTtr() < 1000) {
            job.setTtr(1000);
        }
        return createTube(tubeName).produce(job);
    }

    public boolean delete(String tubeName, String jobId) {
        return loadTube(tubeName).delete(jobId);
    }

    @SuppressWarnings("unchecked")
    public List<Job> consume(String tubeName, long max) {
        if (max < 1) {
            max = 1;
        }

        Tube tube = loadTube(tubeName);

        RedisLock consumeLock = new RedisLock(jedisPool, "lock:consume:" + tubeName, 30);
        if (consumeLock.acquire()) {
            try {
                return tube.consume(max);
            } finally {
                consumeLock.release();
            }
        }

        return Collections.EMPTY_LIST;
    }

    public boolean finish(String tubeName, String jobId) {
        return loadTube(tubeName).finish(jobId);
    }

    public boolean discard(String tubeName, String jobId) {
        return loadTube(tubeName).discard(jobId);
    }

    public List<Job> peek(String tubeName, long max) {
        return loadTube(tubeName).peek(max);
    }

    public boolean kick(String tubeName, String jobId, long dtr) {
        return loadTube(tubeName).kick(jobId, dtr);
    }

    /**
     * move the job of tube to delay queue from reserved.
     * 
     * @param tubeName
     * @param jobId
     * @param dtr
     * @return
     */
    public boolean release(String tubeName, String jobId, long dtr) {
        return loadTube(tubeName).release(jobId, dtr);
    }

    public boolean failure(String tubeName, String jobId) {
        return loadTube(tubeName).failure(jobId);
    }

    public Broker start() {
        executor.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                try {
                    dispatch();
                } catch (Exception e) {
                    LOG.warn("dispatch error: " + e.getMessage());
                }
            }
        }, 1, 2, TimeUnit.SECONDS);

        LOG.info("Broker is started");
        return this;
    }

    public Broker stop() {
        executor.shutdown();
        try {
            executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.HOURS);
        } catch (InterruptedException e) {
        }
        LOG.info("Broker is shutdown");
        return this;
    }

    private void dispatch() {
        Set<String> tubeSet = tubeSet();
        for (String tubeName : tubeSet) {
            if (!taskMap.containsKey(tubeName)) {
                Runnable task = new ScheduledTask(createTube(tubeName), jedisPool);
                executor.scheduleWithFixedDelay(task, 0, 1, TimeUnit.SECONDS);
                taskMap.put(tubeName, task);
            }
        }
    }

    private final class ScheduledTask implements Runnable {
        private final Tube tube;
        private final RedisLock lock;

        private ScheduledTask(Tube tube, JedisPool jedisPool) {
            this.tube = tube;
            this.lock = new RedisLock(jedisPool, "lock:schedule:" + tube.name(), 30);
        }

        @Override
        public void run() {
            if (lock.acquire()) {
                try {
                    tube.schedule();
                } finally {
                    lock.release();
                }
            }
        }
    }

}

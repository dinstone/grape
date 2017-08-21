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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class Broker {

    private static final Logger LOG = LoggerFactory.getLogger(Broker.class);

    private final JedisPool jedisPool;

    private final Map<String, Tube> tubeMap;

    public Broker(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
        this.tubeMap = new ConcurrentHashMap<>();
    }

    public Tube getTube(String tubeName) {
        Tube tube = tubeMap.get(tubeName);
        if (tube == null) {
            createTube(tubeName);
            tube = new Tube(tubeName, jedisPool);
            tubeMap.putIfAbsent(tubeName, tube);
            tube = tubeMap.get(tubeName);
        }
        return tube;
    }

    public void createTube(String tubeName) {
        LOG.info("create tube {}", tubeName);
        Jedis jedis = jedisPool.getResource();
        try {
            jedis.sadd(Tube.TUBE_SET, tubeName);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    public void produce(String tubeName, Job job) {
        if (tubeName == null || tubeName.length() == 0) {
            throw new IllegalArgumentException("tubeName is empty");
        }

        getTube(tubeName).produce(job);
    }

    public void delete(String tubeName, String jobId) {
        getTube(tubeName).delete(jobId);
    }

    public List<Job> consume(String tubeName, long max) {
        if (max < 1) {
            max = 1;
        }

        RedisLock consumeLock = new RedisLock(jedisPool, "lock:consume:" + tubeName, 30);
        if (consumeLock.acquire()) {
            try {
                Tube tube = getTube(tubeName);
                return tube.consume(max);
            } finally {
                consumeLock.release();
            }
        }

        return null;
    }

    public void finish(String tubeName, String jobId) {
        getTube(tubeName).delete(jobId);
    }

    public void release(String tubeName, String id, long dtr) {
        getTube(tubeName).release(id, dtr);
    }

    public void failure(String tubeName, String jobId) {
        getTube(tubeName).failure(jobId);
    }

}

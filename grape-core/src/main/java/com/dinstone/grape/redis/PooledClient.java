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
package com.dinstone.grape.redis;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

public class PooledClient implements RedisClient {

    private final JedisPool jedisPool;

    public PooledClient(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    @Override
    public boolean setNxEx(String key, String value, long lockTimeout) {
        Jedis jedis = jedisPool.getResource();
        try {
            SetParams params = SetParams.setParams().nx().ex(lockTimeout);
            return jedis.set(key, value, params) != null;
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    @Override
    public Long expire(String key, long seconds) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.expire(key, seconds);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    @Override
    public Long ttl(String key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.ttl(key);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    @Override
    public String get(String key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.get(key);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    @Override
    public Long del(String key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.del(key);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    @Override
    public Long sadd(String key, String... members) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.sadd(key, members);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    @Override
    public Boolean sismember(String key, String member) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.sismember(key, member);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    @Override
    public Long srem(String key, String... members) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.srem(key, members);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    @Override
    public Set<String> smembers(String key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.smembers(key);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    @Override
    public Set<String> zrangeByScore(String key, String min, String max, int offset, int count) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrangeByScore(key, min, max, offset, count);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    @Override
    public Long zrem(String key, String... members) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrem(key, members);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    @Override
    public Long zadd(String key, double score, String member) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zadd(key, score, member);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    @Override
    public Long hincrBy(String key, String field, long value) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.hincrBy(key, field, value);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    @Override
    public Set<String> zrange(String key, long start, long stop) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrange(key, start, stop);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    @Override
    public Double zscore(String key, String member) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zscore(key, member);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    @Override
    public Boolean exists(String key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.exists(key);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    @Override
    public Long hset(String key, String field, String value) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.hset(key, field, value);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    @Override
    public String hmset(String key, Map<String, String> hash) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.hmset(key, hash);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    @Override
    public Map<String, String> hgetAll(String key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.hgetAll(key);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    @Override
    public Long zcard(String key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zcard(key);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    @Override
    public String hget(String key, String field) {
        return execute(jedis -> jedis.hget(key, field));
    }

    public <R> R execute(Function<Jedis, R> f) {
        Jedis jedis = jedisPool.getResource();
        try {
            return f.apply(jedis);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    @Override
    public void destroy() {
        jedisPool.destroy();
    }
}

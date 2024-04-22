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

import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.params.SetParams;

public class ClusterClient implements RedisClient {

	private final JedisCluster jedisCluster;

	public ClusterClient(JedisCluster jedisCluster) {
		this.jedisCluster = jedisCluster;
	}

	@Override
	public boolean setNxEx(String key, String value, long lockTimeout) {
		SetParams params = SetParams.setParams().nx().ex(lockTimeout);
		return jedisCluster.set(key, value, params) != null;
	}

	@Override
	public Long expire(String key, long seconds) {
		return jedisCluster.expire(key, seconds);
	}

	@Override
	public Long ttl(String key) {
		return jedisCluster.ttl(key);
	}

	@Override
	public String get(String key) {
		return jedisCluster.get(key);
	}

	@Override
	public Long del(String key) {
		return jedisCluster.del(key);
	}

	@Override
	public Long sadd(String key, String... members) {
		return jedisCluster.sadd(key, members);
	}

	@Override
	public Boolean sismember(String key, String member) {
		return jedisCluster.sismember(key, member);
	}

	@Override
	public Long srem(String key, String... members) {
		return jedisCluster.srem(key, members);
	}

	@Override
	public Set<String> smembers(String key) {
		return jedisCluster.smembers(key);
	}

	@Override
	public Set<String> zrangeByScore(String key, String min, String max, int offset, int count) {
		return jedisCluster.zrangeByScore(key, min, max, offset, count);
	}

	@Override
	public Long zrem(String key, String... members) {
		return jedisCluster.zrem(key, members);
	}

	@Override
	public Long zadd(String key, double score, String member) {
		return jedisCluster.zadd(key, score, member);
	}

	@Override
	public Long hincrBy(String key, String field, long value) {
		return jedisCluster.hincrBy(key, field, value);
	}

	@Override
	public Set<String> zrange(String key, long start, long stop) {
		return jedisCluster.zrange(key, start, stop);
	}

	@Override
	public Double zscore(String key, String member) {
		return jedisCluster.zscore(key, member);
	}

	@Override
	public Boolean exists(String key) {
		return jedisCluster.exists(key);
	}

	@Override
	public Long hset(String key, String field, String value) {
		return jedisCluster.hset(key, field, value);
	}

	@Override
	public String hmset(String key, Map<String, String> hash) {
		return jedisCluster.hmset(key, hash);
	}

	@Override
	public Map<String, String> hgetAll(String key) {
		return jedisCluster.hgetAll(key);
	}

	@Override
	public Long zcard(String key) {
		return jedisCluster.zcard(key);
	}

	@Override
	public String hget(String key, String field) {
		return jedisCluster.hget(key, field);
	}

	@Override
	public void destroy() {
		jedisCluster.close();
	}

}

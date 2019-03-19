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

import java.util.UUID;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisLock {

	private final JedisPool jedisPool;

	private final int lockTimeout;

	private final String lockKey;

	private final String lockValue = UUID.randomUUID().toString();

	public RedisLock(JedisPool jedisPool, String lockKey, int lockTimeout) {
		if (jedisPool == null) {
			throw new IllegalArgumentException("jedisPool is null");
		}
		if (lockKey == null || lockKey.length() == 0) {
			throw new IllegalArgumentException("lockKey is empty");
		}

		this.jedisPool = jedisPool;
		this.lockKey = lockKey;
		this.lockTimeout = lockTimeout;
	}

	public boolean acquire() {
		Jedis jedis = null;
		try {
			jedis = jedisPool.getResource();
			if (jedis.setnx(lockKey, lockValue) == 1) {
				jedis.expire(lockKey, lockTimeout);
				return true;
			}

			if (jedis.ttl(lockKey) == -1) {
				jedis.expire(lockKey, lockTimeout);
			}
			return false;
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}
	}

	public boolean release() {
		if (lockValue == null) {
			return false;
		}

		Jedis jedis = null;
		try {
			jedis = jedisPool.getResource();
			if (lockValue.equals(jedis.get(lockKey))) {
				jedis.del(lockKey);
				return true;
			}
			return false;
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}

	}

}

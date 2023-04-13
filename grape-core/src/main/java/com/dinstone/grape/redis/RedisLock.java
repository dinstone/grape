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

import java.util.UUID;

/**
 * permit lock based on redis. If permit is not acquired, do not wait.
 * 
 * @author dinstone
 *
 */
public class RedisLock {

	private final RedisClient redisClient;

	private final long lockTimeout;

	private final String lockKey;

	private final String lockValue = UUID.randomUUID().toString();

	public RedisLock(RedisClient redisClient, String lockKey, long lockTimeout) {
		if (redisClient == null) {
			throw new IllegalArgumentException("jedisPool is null");
		}
		if (lockKey == null || lockKey.length() == 0) {
			throw new IllegalArgumentException("lockKey is empty");
		}

		this.redisClient = redisClient;
		this.lockKey = lockKey;
		this.lockTimeout = lockTimeout;
	}

	public boolean acquire() {
		if (redisClient.setnx(lockKey, lockValue) == 1) {
			redisClient.expire(lockKey, lockTimeout);
			return true;
		}

		if (redisClient.ttl(lockKey) == -1) {
			redisClient.expire(lockKey, lockTimeout);
		}
		return false;
	}

	public boolean release() {
		if (lockValue.equals(redisClient.get(lockKey))) {
			redisClient.del(lockKey);
			return true;
		}
		return false;
	}

}

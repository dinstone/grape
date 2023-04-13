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

public interface RedisClient {

	public Long setnx(final String key, final String value);

	public Long expire(final String key, final long seconds);

	public Long ttl(final String key);

	public String get(final String key);

	public Long del(final String key);

	public Long sadd(final String key, final String... members);

	public Boolean sismember(final String key, final String member);

	public Long srem(final String key, final String... members);

	public Set<String> smembers(final String key);

	public Set<String> zrangeByScore(final String key, final String min, final String max, final int offset,
			final int count);

	public Long zrem(final String key, final String... members);

	public Long zadd(final String key, final double score, final String member);

	public Long hincrBy(final String key, final String field, final long value);

	public Set<String> zrange(final String key, final long start, final long stop);

	public Double zscore(final String key, final String member);

	public Long zcard(final String key);

	public Boolean exists(final String key);

	public Long hset(final String key, final String field, final String value);

	public String hget(final String key, final String field);

	public String hmset(final String key, final Map<String, String> hash);

	public Map<String, String> hgetAll(final String key);

	public void destroy();

}

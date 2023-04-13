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
package com.dinstone.grape.core;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPoolConfig;

public class JedisClusterTest {

	public static void main(String[] args) throws IOException {
		Set<HostAndPort> jedisClusterNode = new HashSet<HostAndPort>();
		jedisClusterNode.add(new HostAndPort("192.168.1.120", 7001));
		jedisClusterNode.add(new HostAndPort("192.168.1.120", 7002));
		jedisClusterNode.add(new HostAndPort("192.168.1.120", 7003));

		JedisPoolConfig config = new JedisPoolConfig();
		config.setMaxTotal(100);
		config.setMaxIdle(10);

		JedisCluster jedisCluster = new JedisCluster(jedisClusterNode, 1000, 3, config);

		long s = System.currentTimeMillis();
		for (int i = 0; i < 1000; i++) {
			jedisCluster.set("test", "tv");
		}
		long e = System.currentTimeMillis();
		System.out.println("set take's " + (e - s) + "ms");

		s = System.currentTimeMillis();
		for (int i = 0; i < 1000; i++) {
			jedisCluster.get("test");
		}
		e = System.currentTimeMillis();
		System.out.println("get take's " + (e - s) + "ms");

		String v = jedisCluster.get("test");
		System.out.println(v);

		jedisCluster.close();
	}

}

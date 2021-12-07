package com.dinstone.grape.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;

public class JedisClusterTest {

    public static void main(String[] args) throws IOException {
        Set<HostAndPort> jedisClusterNode = new HashSet<HostAndPort>();
        jedisClusterNode.add(new HostAndPort("127.0.0.1", 7001));
        jedisClusterNode.add(new HostAndPort("127.0.0.1", 7002));
        jedisClusterNode.add(new HostAndPort("127.0.0.1", 7003));
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(100);
        config.setMaxIdle(10);

        JedisCluster jedisCluster = new JedisCluster(jedisClusterNode, 1000, 3, config);

        jedisCluster.set("test", "tv");
        String v = jedisCluster.get("test");
        System.out.println(v);

        jedisCluster.close();
    }

    private static void shareding() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(200);
        config.setMaxIdle(50);
        config.setMaxWaitMillis(1000 * 100);
        config.setTestOnBorrow(false);
        List<JedisShardInfo> shareInfos = new ArrayList<JedisShardInfo>();
        shareInfos.add(new JedisShardInfo("127.0.0.1", 7001));
        shareInfos.add(new JedisShardInfo("127.0.0.1", 7002));
        shareInfos.add(new JedisShardInfo("127.0.0.1", 7003));
        ShardedJedisPool jedisPool = new ShardedJedisPool(config, shareInfos);

        try {
            ShardedJedis j = jedisPool.getResource();

            j.set("test", "tv");

            String v = j.get("test");
            System.out.println(v);
            j.close();
        } catch (Exception e) {
        }
        jedisPool.close();
    }

}

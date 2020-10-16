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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dinstone.grape.exception.ApplicationException;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Tuple;

public class Tube {

    private static final Logger LOG = LoggerFactory.getLogger(Tube.class);

    private final JedisPool jedisPool;

    private final String tubeName;

    /**
     * tube stats type : hashmap
     */
    private final String tubeStats;

    /**
     * job type : hashmap
     */
    private final String jobPrefix;

    /**
     * delay queue type : zset
     */
    private final String delayQueue;

    /**
     * ready queue type : zset
     */
    private final String readyQueue;

    /**
     * retain queue type : zset
     */
    private final String retainQueue;

    /**
     * failed queue type : zset
     */
    private final String failedQueue;

    public Tube(String group, String tubeName, JedisPool jedisPool) {
        this.jedisPool = jedisPool;
        this.tubeName = tubeName;

        String tubePrefix;
        if (group != null && !group.isEmpty()) {
            tubePrefix = group + ":tube:";
        } else {
            tubePrefix = "tube:";
        }

        this.jobPrefix = tubePrefix + tubeName + ":job:";
        this.tubeStats = tubePrefix + tubeName + ":stats";

        this.delayQueue = tubePrefix + tubeName + ":queue:delay";
        this.readyQueue = tubePrefix + tubeName + ":queue:ready";
        this.retainQueue = tubePrefix + tubeName + ":queue:retain";
        this.failedQueue = tubePrefix + tubeName + ":queue:failed";
    }

    /**
     * tube's name
     * 
     * @return
     */
    public String name() {
        return tubeName;
    }

    /**
     * tube's stats info
     * 
     * @return
     */
    public Stats stats() {
        Jedis jedis = jedisPool.getResource();
        try {
            Stats stats = new Stats();
            stats.setTubeName(tubeName);

            stats.setDelayQueueSize(jedis.zcard(delayQueue));
            stats.setReadyQueueSize(jedis.zcard(readyQueue));
            stats.setRetainQueueSize(jedis.zcard(retainQueue));
            stats.setFailedQueueSize(jedis.zcard(failedQueue));

            Map<String, String> tsMap = jedis.hgetAll(tubeStats);
            String tjs = tsMap.get("tjs");
            if (tjs != null) {
                stats.setTotalJobSize(Long.parseLong(tjs));
            }
            String fjs = tsMap.get("fjs");
            if (fjs != null) {
                stats.setFinishJobSize(Long.parseLong(fjs));
            }

            return stats;
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    /**
     * schedule job to ready queue
     */
    public void schedule() {
        Jedis jedis = jedisPool.getResource();
        try {
            delayToReady(jedis);
            retainToReady(jedis);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    private void retainToReady(Jedis jedis) {
        long expireTime = System.currentTimeMillis() + 10000;
        boolean stop = false;
        while (!stop) {
            Set<Tuple> jobScoreSet = jedis.zrangeWithScores(retainQueue, 0, 100);
            if (jobScoreSet == null || jobScoreSet.size() == 0) {
                break;
            }

            long currentTime = System.currentTimeMillis();
            if (currentTime > expireTime) {
                break;
            }

            for (Tuple jobScore : jobScoreSet) {
                if (jobScore.getScore() < currentTime) {
                    String jobId = jobScore.getElement();
                    String dtr = jedis.hget(jobPrefix + jobId, "dtr");
                    if (dtr == null) {
                        LOG.warn("job[{}:{}] is invalid, remove from retain queue.", tubeName, jobId);
                        jedis.zrem(retainQueue, jobId);
                        continue;
                    }

                    jedis.zadd(readyQueue, currentTime, jobId);
                    jedis.zrem(retainQueue, jobId);
                } else {
                    stop = true;
                    break;
                }
            }
        }
    }

    private void delayToReady(Jedis jedis) {
        long expireTime = System.currentTimeMillis() + 10000;
        boolean stop = false;
        while (!stop) {
            Set<Tuple> jobScoreSet = jedis.zrangeWithScores(delayQueue, 0, 100);
            if (jobScoreSet == null || jobScoreSet.size() == 0) {
                break;
            }

            long currentTime = System.currentTimeMillis();
            if (currentTime >= expireTime) {
                break;
            }

            for (Tuple jobScore : jobScoreSet) {
                if (jobScore.getScore() < currentTime) {
                    String jobId = jobScore.getElement();
                    String dtr = jedis.hget(jobPrefix + jobId, "dtr");
                    if (dtr == null) {
                        LOG.warn("job[{}:{}] is invalid, remove from delay queue.", tubeName, jobId);
                        jedis.zrem(delayQueue, jobId);
                        continue;
                    }

                    jedis.zadd(readyQueue, currentTime, jobId);
                    jedis.zrem(delayQueue, jobId);
                } else {
                    stop = true;
                    break;
                }
            }
        }
    }

    /**
     * add a job to tube's delay queue.
     * 
     * @param job
     * @return
     */
    public boolean produce(Job job) {
        Jedis jedis = jedisPool.getResource();
        try {
            if (jedis.zscore(delayQueue, job.getId()) == null) {
                Map<String, String> hashJob = job2Map(job);
                jedis.hmset(jobPrefix + job.getId(), hashJob);

                double score = System.currentTimeMillis() + job.getDtr();
                long uc = jedis.zadd(delayQueue, score, job.getId());
                if (uc > 0) {
                    jedis.hincrBy(tubeStats, "tjs", 1);
                    return true;
                }
            }
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }

        return false;
    }

    /**
     * the job is success, delete job from tube's delay queue.
     * 
     * @param jobId
     * @return
     */
    public boolean delete(String jobId) {
        Jedis jedis = jedisPool.getResource();
        try {
            // delete from delay queue
            if (jedis.zrem(delayQueue, jobId) > 0) {
                jedis.del(jobPrefix + jobId);
                jedis.hincrBy(tubeStats, "fjs", 1);
                return true;
            }
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }

        return false;
    }

    /**
     * consume some jobs.
     * 
     * @param max
     * @return
     */
    public List<Job> consume(long max) {
        List<Job> jobs = new ArrayList<>();
        Jedis jedis = jedisPool.getResource();
        try {
            Set<String> jobSet = jedis.zrange(readyQueue, 0, max - 1);
            for (String jobId : jobSet) {
                Map<String, String> jobMap = jedis.hgetAll(jobPrefix + jobId);
                if (jobMap == null || jobMap.get("id") == null) {
                    LOG.warn("job[{}:{}] is invalid, remove from ready queue.", tubeName, jobId);
                    // delete from delay queue
                    // jedis.zrem(delayQueue, jobId);
                    jedis.zrem(readyQueue, jobId);
                } else {
                    jedis.hincrBy(jobPrefix + jobId, "noe", 1);

                    Job job = null;
                    try {
                        job = map2Job(jobMap);
                    } catch (Exception e) {
                        LOG.warn("job[{}:{}] is invalid, remove to failure queue.", tubeName, jobId);
                        failure(jobId);
                        continue;
                    }

                    double score = System.currentTimeMillis() + job.getTtr();
                    jedis.zadd(retainQueue, score, jobId);
                    jedis.zrem(readyQueue, jobId);

                    jobs.add(job);
                }
            }
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return jobs;
    }

    /**
     * release a job to delay queue from retain queue.
     * 
     * @param jobId
     * @param dtr
     * @return
     */
    public boolean release(String jobId, long dtr) {
        Jedis jedis = jedisPool.getResource();
        try {
            if (jedis.zscore(retainQueue, jobId) == null) {
                return false;
            }

            if (jedis.exists(jobPrefix + jobId)) {
                jedis.hset(jobPrefix + jobId, "dtr", Long.toString(dtr));
                double score = System.currentTimeMillis() + dtr;
                jedis.zadd(delayQueue, score, jobId);
                jedis.zrem(retainQueue, jobId);
                return true;
            }
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }

        return false;
    }

    /**
     * the job is success, finish the job.
     * 
     * @param jobId
     * @return
     */
    public boolean finish(String jobId) {
        Jedis jedis = jedisPool.getResource();
        try {
            if (jedis.zrem(retainQueue, jobId) > 0) {
                jedis.del(jobPrefix + jobId);
                jedis.hincrBy(tubeStats, "fjs", 1);
                return true;
            }
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return false;
    }

    /**
     * the job is failure, move the job from retain to failed queue.
     * 
     * @param jobId
     * @return
     */
    public boolean failure(String jobId) {
        Jedis jedis = jedisPool.getResource();
        try {
            if (jedis.zscore(retainQueue, jobId) == null) {
                return false;
            }

            if (jedis.exists(jobPrefix + jobId)) {
                double score = System.currentTimeMillis();
                jedis.zadd(failedQueue, score, jobId);
                jedis.zrem(retainQueue, jobId);
                return true;
            }
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return false;
    }

    /**
     * fetch the failed job to peek it.
     * 
     * @param max
     * @return
     */
    public List<Job> peek(long max) {
        List<Job> jobs = new ArrayList<>();
        Jedis jedis = jedisPool.getResource();
        try {
            Set<String> jobSet = jedis.zrange(failedQueue, 0, max - 1);
            for (String jobId : jobSet) {
                Map<String, String> jobMap = jedis.hgetAll(jobPrefix + jobId);
                if (jobMap == null || jobMap.get("id") == null) {
                    LOG.warn("job[{}:{}] is invalid, remove from failed queue.", tubeName, jobId);
                    discard(jobId);
                } else {
                    Job job = null;
                    try {
                        job = map2Job(jobMap);
                    } catch (Exception e) {
                        LOG.warn("job[{}:{}] is invalid, discard the job.", tubeName, jobId);
                        discard(jobId);
                        continue;
                    }
                    jobs.add(job);
                }
            }
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return jobs;
    }

    /**
     * discard job from tube's failed queue.
     * 
     * @param jobId
     * @return
     */
    public boolean discard(String jobId) {
        Jedis jedis = jedisPool.getResource();
        try {
            if (jedis.zrem(failedQueue, jobId) > 0) {
                jedis.del(jobPrefix + jobId);
                jedis.hincrBy(tubeStats, "fjs", 1);
                return true;
            }
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return false;
    }

    /**
     * kick the failed job to delay queue.
     * 
     * @param jobId
     * @param dtr
     * @return
     */
    public boolean kick(String jobId, long dtr) {
        Jedis jedis = jedisPool.getResource();
        try {
            if (jedis.zscore(failedQueue, jobId) == null) {
                return false;
            }

            if (jedis.exists(jobPrefix + jobId)) {
                jedis.hset(jobPrefix + jobId, "dtr", Long.toString(dtr));
                double score = System.currentTimeMillis() + dtr;
                jedis.zadd(delayQueue, score, jobId);
                jedis.zrem(failedQueue, jobId);
                return true;
            }
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }

        return false;
    }

    private Map<String, String> job2Map(Job job) {
        Map<String, String> res = new HashMap<>();
        res.put("id", job.getId());
        res.put("dtr", Long.toString(job.getDtr()));
        res.put("ttr", Long.toString(job.getTtr()));
        res.put("noe", Long.toString(job.getNoe()));
        try {
            res.put("data", encode(job.getData()));
        } catch (UnsupportedEncodingException e) {
            throw new ApplicationException(e);
        }
        return res;
    }

    private Job map2Job(Map<String, String> map) {
        Job job = new Job();
        String id = map.get("id");
        if (id != null) {
            job.setId(id);
        }
        String dtr = map.get("dtr");
        if (dtr != null) {
            job.setDtr(Long.parseLong(dtr));
        }
        String ttr = map.get("ttr");
        if (ttr != null) {
            job.setTtr(Long.parseLong(ttr));
        }
        String noe = map.get("noe");
        if (noe != null) {
            job.setNoe(Long.parseLong(noe));
        }
        String data = map.get("data");
        if (data != null) {
            try {
                job.setData(decode(data));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        return job;
    }

    private String encode(byte[] data) throws UnsupportedEncodingException {
        return new String(Base64.getEncoder().encode(data), "utf-8");
    }

    private byte[] decode(String data) throws UnsupportedEncodingException {
        return Base64.getDecoder().decode(data.getBytes("utf-8"));
    }

}

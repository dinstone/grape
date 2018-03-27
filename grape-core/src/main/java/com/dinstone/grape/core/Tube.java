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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Tuple;

public class Tube {

    private static final Logger LOG = LoggerFactory.getLogger(Tube.class);

    public static final String TUBE_PREFIX = "tube:";

    public static final String TUBE_SET = TUBE_PREFIX + "set";

    private static final long ONE_YEAR_MS = 31536000000L;

    private final JedisPool jedisPool;

    private final String tubeName;

    private final String delayQuene;

    private final String readyQuene;

    private final String failedQuene;

    private final String jobKeyPrefix;

    public Tube(String tubeName, JedisPool jedisPool) {
        this.jedisPool = jedisPool;
        this.tubeName = tubeName;

        this.delayQuene = TUBE_PREFIX + tubeName + ":queue:delay";
        this.readyQuene = TUBE_PREFIX + tubeName + ":queue:ready";
        this.failedQuene = TUBE_PREFIX + tubeName + ":queue:failed";
        this.jobKeyPrefix = TUBE_PREFIX + tubeName + ":job:";
    }

    public String getTubeName() {
        return tubeName;
    }

    public Tube produce(Job job) {
        Jedis jedis = jedisPool.getResource();
        try {
            if (jedis.zscore(delayQuene, job.getId()) == null) {
                Map<String, String> hashJob = job2Map(job);
                jedis.hmset(jobKeyPrefix + job.getId(), hashJob);

                double score = System.currentTimeMillis() + job.getDtr();
                jedis.zadd(delayQuene, score, job.getId());
            }
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }

        return this;
    }

    public Tube delete(String jobId) {
        Jedis jedis = jedisPool.getResource();
        try {
            jedis.del(jobKeyPrefix + jobId);
            // delete from delay queue
            jedis.zrem(delayQuene, jobId);
            jedis.zrem(readyQuene, jobId);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }

        return this;
    }

    public List<Job> consume(long max) {
        List<Job> jobs = new ArrayList<>();
        Jedis jedis = jedisPool.getResource();
        try {
            Set<String> jobSet = jedis.zrange(readyQuene, 0, max - 1);
            for (String jobId : jobSet) {
                Map<String, String> jobMap = jedis.hgetAll(jobKeyPrefix + jobId);
                if (jobMap == null || jobMap.get("id") == null) {
                    LOG.warn("job[{}:{}] is invalid, remove from delay and ready queue.", tubeName, jobId);
                    // delete from delay queue
                    jedis.zrem(delayQuene, jobId);
                    jedis.zrem(readyQuene, jobId);
                } else {
                    jedis.hincrBy(jobKeyPrefix + jobId, "noe", 1);

                    Job job = null;
                    try {
                        job = map2Job(jobMap);
                    } catch (Exception e) {
                        LOG.warn("job[{}:{}] is invalid, remove to failure queue.", tubeName, jobId);
                        failure(jobId);
                        continue;
                    }

                    double score = System.currentTimeMillis() + job.getTtr();
                    jedis.zadd(delayQuene, score, jobId);
                    jedis.zrem(readyQuene, jobId);

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

    public void schedule() {
        int maxScheduleCount = 10;
        while (maxScheduleCount > 0) {
            Jedis jedis = jedisPool.getResource();
            try {
                Set<Tuple> jobScoreSet = jedis.zrangeWithScores(delayQuene, 0, 100);
                if (jobScoreSet == null || jobScoreSet.size() == 0) {
                    break;
                }

                maxScheduleCount--;

                long currentTime = System.currentTimeMillis();
                for (Tuple jobScore : jobScoreSet) {
                    if (jobScore.getScore() < currentTime) {
                        String jobId = jobScore.getElement();
                        String dtr = jedis.hget(jobKeyPrefix + jobId, "dtr");
                        if (dtr == null) {
                            LOG.warn("job[{}:{}] is invalid, remove from delay queue.", tubeName, jobId);
                            jedis.zrem(delayQuene, jobId);
                            continue;
                        }

                        double score = currentTime + ONE_YEAR_MS + Long.parseLong(dtr);
                        jedis.zadd(delayQuene, score, jobId);

                        jedis.zadd(readyQuene, currentTime, jobId);
                    } else {
                        break;
                    }
                }
            } finally {
                if (jedis != null) {
                    jedis.close();
                }
            }
        }
    }

    public Tube release(String jobId, long dtr) {
        Jedis jedis = jedisPool.getResource();
        try {
            if (jedis.exists(jobKeyPrefix + jobId)) {
                jedis.hset(jobKeyPrefix + jobId, "dtr", "" + dtr);
                double score = System.currentTimeMillis() + dtr;
                jedis.zadd(delayQuene, score, jobId);
            }
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }

        return this;
    }

    public Tube failure(String jobId) {
        Jedis jedis = jedisPool.getResource();
        try {
            if (jedis.exists(jobKeyPrefix + jobId)) {
                double score = System.currentTimeMillis();
                jedis.zadd(failedQuene, score, jobId);

                // delete from delay queue
                jedis.zrem(delayQuene, jobId);
                jedis.zrem(readyQuene, jobId);
            }
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return this;
    }

    private Map<String, String> job2Map(Job job) {
        Map<String, String> res = new HashMap<>();
        res.put("id", job.getId());
        res.put("dtr", "" + job.getDtr());
        res.put("ttr", "" + job.getTtr());
        res.put("noe", "" + job.getNoe());
        try {
            res.put("data", encode(job.getData()));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
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

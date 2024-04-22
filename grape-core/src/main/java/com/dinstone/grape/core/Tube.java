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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dinstone.grape.redis.RedisClient;
import com.dinstone.grape.redis.RedisLock;

public class Tube {

    private static final Logger LOG = LoggerFactory.getLogger(Tube.class);

    private static final Charset UTF_8 = StandardCharsets.UTF_8;

    private static final String JOB_ID = "id";

    private static final String JOB_NOE = "noe";

    private static final String JOB_TTR = "ttr";

    private static final String JOB_DTR = "dtr";

    private static final String JOB_DATA = "data";

    /**
     * lock timeout : SECONDS
     */
    private static final int LOCK_TIMEOUT = 10;

    private final RedisClient redisClient;

    private final String tubeName;

    /**
     * job type : hashmap
     */
    private final String jobPrefix;

    /**
     * delay queue type : zset
     */
    private final String delayQueue;

    /**
     * retain queue type : zset
     */
    private final String retainQueue;

    /**
     * failed queue type : zset
     */
    private final String failedQueue;

    private final RedisLock consumeLock;

    private final RedisLock scheduleLock;

    public Tube(String namespace, String tubeName, RedisClient redisClient) {
        this.redisClient = redisClient;
        this.tubeName = tubeName;

        String tubePrefix;
        if (namespace != null && !namespace.isEmpty()) {
            tubePrefix = namespace + ":tube:";
        } else {
            tubePrefix = "tube:";
        }

        this.jobPrefix = tubePrefix + tubeName + ":job:";

        this.delayQueue = tubePrefix + tubeName + ":queue:delay";
        this.retainQueue = tubePrefix + tubeName + ":queue:retain";
        this.failedQueue = tubePrefix + tubeName + ":queue:failed";

        String consumeLockKey = tubePrefix + tubeName + ":lock:consume";
        this.consumeLock = new RedisLock(redisClient, consumeLockKey, LOCK_TIMEOUT);
        String scheduleLockKey = tubePrefix + tubeName + ":lock:schedule";
        this.scheduleLock = new RedisLock(redisClient, scheduleLockKey, LOCK_TIMEOUT);
    }

    /**
     * tube's name
     */
    public String name() {
        return tubeName;
    }

    /**
     * tube's stats info
     */
    public Stats stats() {
        Stats stats = new Stats();
        stats.setTubeName(tubeName);

        stats.setDelayQueueSize(redisClient.zcard(delayQueue));
        stats.setRetainQueueSize(redisClient.zcard(retainQueue));
        stats.setFailedQueueSize(redisClient.zcard(failedQueue));

        return stats;
    }

    public void destroy() {
        for (; ; ) {
            Set<String> js = redisClient.zrange(failedQueue, 0, 100);
            if (js == null || js.isEmpty()) {
                break;
            }
            for (String j : js) {
                redisClient.zrem(failedQueue, j);
                redisClient.del(jobPrefix + j);
            }
        }
        for (; ; ) {
            Set<String> js = redisClient.zrange(retainQueue, 0, 100);
            if (js == null || js.isEmpty()) {
                break;
            }
            for (String j : js) {
                redisClient.zrem(retainQueue, j);
                redisClient.del(jobPrefix + j);
            }
        }
        for (; ; ) {
            Set<String> js = redisClient.zrange(delayQueue, 0, 100);
            if (js == null || js.isEmpty()) {
                break;
            }
            for (String j : js) {
                redisClient.zrem(delayQueue, j);
                redisClient.del(jobPrefix + j);
            }
        }

        redisClient.del(delayQueue);
        redisClient.del(retainQueue);
        redisClient.del(failedQueue);

    }

    /**
     * schedule job to ready queue
     */
    public void schedule() {
        if (scheduleLock.acquire()) {
            try {
                LOG.debug("tube[{}] schedule retain to ready", tubeName);
                retainToReady();
            } finally {
                scheduleLock.release();
            }
        }
    }

    private void retainToReady() {
        int timeout = Math.max(1, LOCK_TIMEOUT - 1) * 1000;
        long deadlineTime = System.currentTimeMillis() + timeout;
        while (true) {
            long currentTime = System.currentTimeMillis();
            if (currentTime >= deadlineTime) {
                break;
            }

            String maxScore = Long.toString(currentTime);
            Set<String> jobSet = redisClient.zrangeByScore(retainQueue, "-inf", maxScore, 0, 100);
            if (jobSet == null || jobSet.isEmpty()) {
                break;
            }

            for (String jobId : jobSet) {
                String ttr = redisClient.hget(jobPrefix + jobId, JOB_TTR);
                if (ttr == null) {
                    LOG.warn("job[{}:{}] is deleted, remove from retain queue.", tubeName, jobId);
                    redisClient.zrem(retainQueue, jobId);
                    continue;
                }
                // for ready queue
                long readyTime = currentTime - Long.parseLong(ttr);

                redisClient.zadd(delayQueue, readyTime, jobId);
                redisClient.zrem(retainQueue, jobId);
            }
        }
    }

    /**
     * add a job to tube's delay queue.
     */
    public boolean produce(Job job) {
        if (redisClient.zscore(delayQueue, job.getId()) == null) {
            Map<String, String> hashJob = job2Map(job);
            redisClient.hmset(jobPrefix + job.getId(), hashJob);

            double score = System.currentTimeMillis() + job.getDtr();
            long uc = redisClient.zadd(delayQueue, score, job.getId());
            return uc > 0;
        }

        return false;
    }

    /**
     * the job is success, delete job from tube's delay queue.
     */
    public boolean delete(String jobId) {
        redisClient.del(jobPrefix + jobId);
        // delete from delay queue
        return redisClient.zrem(delayQueue, jobId) > 0;
    }

    /**
     * consume some jobs.
     *
     * @param max maximum consumption per time
     */
    public List<Job> consume(int max) {
        if (!consumeLock.acquire()) {
            return Collections.emptyList();
        }

        List<Job> jobs = new LinkedList<>();
        try {
            long currentTime = System.currentTimeMillis();
            int timeout = Math.max(1, LOCK_TIMEOUT - 1) * 1000;
            long deadlineTime = currentTime + timeout;
            String maxScore = Long.toString(currentTime);
            Set<String> jobSet = redisClient.zrangeByScore(delayQueue, "-inf", maxScore, 0, max);
            for (String jobId : jobSet) {
                currentTime = System.currentTimeMillis();
                // check deadline limit
                if (currentTime >= deadlineTime) {
                    break;
                }

                Map<String, String> jobMap = redisClient.hgetAll(jobPrefix + jobId);

                if (jobMap == null || jobMap.get(JOB_ID) == null) {
                    LOG.warn("job[{}:{}] is invalid, remove from ready queue.", tubeName, jobId);
                    // delete from delay queue
                    redisClient.zrem(delayQueue, jobId);
                } else {
                    Job job;
                    try {
                        job = map2Job(jobMap);
                    } catch (Exception e) {
                        LOG.warn("job[{}:{}] is invalid, remove to failure queue.", tubeName, jobId);
                        redisClient.zadd(failedQueue, currentTime, jobId);
                        redisClient.zrem(delayQueue, jobId);

                        continue;
                    }

                    // remove job to retain queue from delay queue
                    double score = currentTime + job.getTtr();
                    redisClient.zadd(retainQueue, score, jobId);
                    redisClient.zrem(delayQueue, jobId);
                    // update the job's number of execution
                    redisClient.hincrBy(jobPrefix + jobId, JOB_NOE, 1);

                    jobs.add(job);
                }
            }
        } finally {
            consumeLock.release();
        }
        return jobs;
    }

    /**
     * release a job to delay queue from retain queue.
     */
    public boolean release(String jobId, long dtr) {
        if (redisClient.zscore(retainQueue, jobId) == null) {
            return false;
        }

        if (redisClient.exists(jobPrefix + jobId)) {
            redisClient.hset(jobPrefix + jobId, JOB_DTR, Long.toString(dtr));
            double score = System.currentTimeMillis() + dtr;
            redisClient.zadd(delayQueue, score, jobId);
            redisClient.zrem(retainQueue, jobId);
            return true;
        }

        return false;
    }

    /**
     * the job is success, finish the job.
     */
    public boolean finish(String jobId) {
        redisClient.del(jobPrefix + jobId);
        // delete from retain queue
        return redisClient.zrem(retainQueue, jobId) > 0;
    }

    /**
     * the job is failure, move the job from retain to failed queue.
     */
    public boolean bury(String jobId) {
        // check retain queue has job
        if (redisClient.zscore(retainQueue, jobId) == null) {
            return false;
        }

        // check job exist
        String dtr = redisClient.hget(jobPrefix + jobId, JOB_DTR);
        if (dtr == null) {
            LOG.warn("job[{}:{}] is invalid, remove from retain queue.", tubeName, jobId);
            redisClient.zrem(retainQueue, jobId);
            return false;
        }

        // for submit time
        long submitTime = System.currentTimeMillis() - Long.parseLong(dtr);

        redisClient.zadd(failedQueue, submitTime, jobId);
        redisClient.zrem(retainQueue, jobId);
        return true;
    }

    /**
     * fetch the failed job to peek it.
     */
    public List<Job> peek(long max) {
        List<Job> jobs = new ArrayList<>();
        Set<String> jobSet = redisClient.zrange(failedQueue, 0, max - 1);
        for (String jobId : jobSet) {
            Map<String, String> jobMap = redisClient.hgetAll(jobPrefix + jobId);
            if (jobMap == null || jobMap.get(JOB_ID) == null) {
                LOG.warn("job[{}:{}] is invalid, remove from failed queue.", tubeName, jobId);
                discard(jobId);
            } else {
                Job job;
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
        return jobs;
    }

    /**
     * discard job from tube's failed queue.
     */
    public boolean discard(String jobId) {
        if (redisClient.zrem(failedQueue, jobId) > 0) {
            redisClient.del(jobPrefix + jobId);
            return true;
        }
        return false;
    }

    /**
     * kick the failed job to delay queue.
     */
    public boolean kick(String jobId, long dtr) {
        if (redisClient.zscore(failedQueue, jobId) == null) {
            return false;
        }

        String jobKey = jobPrefix + jobId;
        if (redisClient.exists(jobKey)) {
            redisClient.hset(jobKey, JOB_DTR, Long.toString(dtr));
            double score = System.currentTimeMillis() + dtr;
            redisClient.zadd(delayQueue, score, jobId);
            redisClient.zrem(failedQueue, jobId);
            return true;
        }

        return false;
    }

    @Override
    public String toString() {
        return "Tube [tubeName=" + tubeName + "]";
    }

    private Map<String, String> job2Map(Job job) {
        Map<String, String> res = new HashMap<>();
        res.put(JOB_ID, job.getId());
        res.put(JOB_DTR, Long.toString(job.getDtr()));
        res.put(JOB_TTR, Long.toString(job.getTtr()));
        res.put(JOB_NOE, Long.toString(job.getNoe()));
        res.put(JOB_DATA, encode(job.getData()));
        return res;
    }

    private Job map2Job(Map<String, String> map) {
        Job job = new Job();
        String id = map.get(JOB_ID);
        if (id != null) {
            job.setId(id);
        }
        String dtr = map.get(JOB_DTR);
        if (dtr != null) {
            job.setDtr(Long.parseLong(dtr));
        }
        String ttr = map.get(JOB_TTR);
        if (ttr != null) {
            job.setTtr(Long.parseLong(ttr));
        }
        String noe = map.get(JOB_NOE);
        if (noe != null) {
            job.setNoe(Long.parseLong(noe));
        }
        String data = map.get(JOB_DATA);
        if (data != null) {
            job.setData(decode(data));
        }
        return job;
    }

    private String encode(byte[] data) {
        return new String(data, UTF_8);
    }

    private byte[] decode(String data) {
        return data.getBytes(UTF_8);
    }

}

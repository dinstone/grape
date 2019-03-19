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

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Tuple;

public class Tube {

	private static final Logger LOG = LoggerFactory.getLogger(Tube.class);

	private static final long ONE_YEAR_MS = 31536000000L;

	private static final String TUBE_PREFIX = "tube:";

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
	 * failed queue type : zset
	 */
	private final String failedQueue;

	public Tube(String tubeName, JedisPool jedisPool) {
		this.jedisPool = jedisPool;
		this.tubeName = tubeName;

		this.jobPrefix = TUBE_PREFIX + tubeName + ":job:";
		this.tubeStats = TUBE_PREFIX + tubeName + ":stats";

		this.delayQueue = TUBE_PREFIX + tubeName + ":queue:delay";
		this.readyQueue = TUBE_PREFIX + tubeName + ":queue:ready";
		this.failedQueue = TUBE_PREFIX + tubeName + ":queue:failed";

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
		int maxScheduleCount = 10;
		while (maxScheduleCount > 0) {
			Jedis jedis = jedisPool.getResource();
			try {
				Set<Tuple> jobScoreSet = jedis.zrangeWithScores(delayQueue, 0, 100);
				if (jobScoreSet == null || jobScoreSet.size() == 0) {
					break;
				}

				maxScheduleCount--;

				long currentTime = System.currentTimeMillis();
				for (Tuple jobScore : jobScoreSet) {
					if (jobScore.getScore() < currentTime) {
						String jobId = jobScore.getElement();
						String dtr = jedis.hget(jobPrefix + jobId, "dtr");
						if (dtr == null) {
							LOG.warn("job[{}:{}] is invalid, remove from delay queue.", tubeName, jobId);
							jedis.zrem(delayQueue, jobId);
							continue;
						}

						double score = currentTime + ONE_YEAR_MS + Long.parseLong(dtr);
						jedis.zadd(delayQueue, score, jobId);

						jedis.zadd(readyQueue, currentTime, jobId);
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

	/**
	 * add a job to tube's delay queue.
	 * 
	 * @param job
	 * @return
	 */
	public Tube produce(Job job) {
		Jedis jedis = jedisPool.getResource();
		try {
			if (jedis.zscore(delayQueue, job.getId()) == null) {
				Map<String, String> hashJob = job2Map(job);
				jedis.hmset(jobPrefix + job.getId(), hashJob);

				double score = System.currentTimeMillis() + job.getDtr();
				long uc = jedis.zadd(delayQueue, score, job.getId());
				if (uc > 0) {
					jedis.hincrBy(tubeStats, "tjs", 1);
				}
			}
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}

		return this;
	}

	/**
	 * the job is success, delete job from tube's delay and ready queue.
	 * 
	 * @param jobId
	 * @return
	 */
	public Tube delete(String jobId) {
		Jedis jedis = jedisPool.getResource();
		try {
			long uc = jedis.del(jobPrefix + jobId);
			if (uc > 0) {
				// delete from delay queue
				jedis.zrem(delayQueue, jobId);
				jedis.zrem(readyQueue, jobId);

				jedis.hincrBy(tubeStats, "fjs", 1);
			}
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}

		return this;
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
					LOG.warn("job[{}:{}] is invalid, remove from delay and ready queue.", tubeName, jobId);
					// delete from delay queue
					jedis.zrem(delayQueue, jobId);
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
					jedis.zadd(delayQueue, score, jobId);
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
	 * release a job to delay queue with the new dtr.
	 * 
	 * @param jobId
	 * @param dtr
	 * @return
	 */
	public Tube release(String jobId, long dtr) {
		Jedis jedis = jedisPool.getResource();
		try {
			if (jedis.exists(jobPrefix + jobId)) {
				jedis.hset(jobPrefix + jobId, "dtr", Long.toString(dtr));
				double score = System.currentTimeMillis() + dtr;
				jedis.zadd(delayQueue, score, jobId);
			}
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}

		return this;
	}

	/**
	 * the job is failure, move the job to failed queue.
	 * 
	 * @param jobId
	 * @return
	 */
	public Tube failure(String jobId) {
		Jedis jedis = jedisPool.getResource();
		try {
			if (jedis.exists(jobPrefix + jobId)) {
				double score = System.currentTimeMillis();
				jedis.zadd(failedQueue, score, jobId);

				// delete from delay queue
				jedis.zrem(delayQueue, jobId);
				jedis.zrem(readyQueue, jobId);
			}
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}
		return this;
	}

	/**
	 * peek the failed job to consume it.
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
					jedis.zrem(failedQueue, jobId);
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
	public Tube discard(String jobId) {
		Jedis jedis = jedisPool.getResource();
		try {
			long dc = jedis.zrem(failedQueue, jobId);
			if (dc > 0) {
				jedis.del(jobPrefix + jobId);

				jedis.hincrBy(tubeStats, "fjs", 1);
			}
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}

		return this;
	}

	/**
	 * kick the failed job to delay queue.
	 * 
	 * @param jobId
	 * @param dtr
	 * @return
	 */
	public Tube kick(String jobId, long dtr) {
		Jedis jedis = jedisPool.getResource();
		try {
			if (jedis.exists(jobPrefix + jobId)) {
				jedis.hset(jobPrefix + jobId, "dtr", "" + dtr);
				double score = System.currentTimeMillis() + dtr;
				jedis.zadd(delayQueue, score, jobId);

				jedis.zrem(failedQueue, jobId);
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
		res.put("dtr", Long.toString(job.getDtr()));
		res.put("ttr", Long.toString(job.getTtr()));
		res.put("noe", Long.toString(job.getNoe()));
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

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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dinstone.grape.exception.ApplicationException;
import com.dinstone.grape.exception.BusinessException;
import com.dinstone.grape.exception.TubeErrorCode;
import com.dinstone.grape.redis.RedisClient;

public class Broker {

	private static final Logger LOG = LoggerFactory.getLogger(Broker.class);

	private static final String THREAD_PREFIX = "broker-schedule-thread-";

	private static final String TUBENAME_REGEX = "^([a-z]|[A-Z])(\\w|-)*";

	private static final int TUBENAME_LENGTH = 64;

	private static final int DEFAULT_TTR = 1000;

	private final String group;

	private final String tubeSetKey;

	private final RedisClient redisClient;

	private final Map<String, Tube> tubeMap;

	private final Map<String, Runnable> taskMap;

	private final ScheduledExecutorService executor;

	public Broker(RedisClient redisClient) {
		this(redisClient, null, Runtime.getRuntime().availableProcessors());
	}

	public Broker(RedisClient redisClient, String group, int scheduledSize) {
		if (redisClient == null) {
			throw new ApplicationException("redisClient is null");
		}
		if (scheduledSize <= 0) {
			throw new ApplicationException("scheduledSize must be greater than 0");
		}

		if (group != null && !group.isEmpty()) {
			this.group = group;
			this.tubeSetKey = group + ":tube:set";
		} else {
			this.group = "";
			this.tubeSetKey = "tube:set";
		}

		this.redisClient = redisClient;
		this.tubeMap = new ConcurrentHashMap<>();
		this.taskMap = new ConcurrentHashMap<>();
		this.executor = Executors.newScheduledThreadPool(scheduledSize, new ThreadFactory() {

			private final AtomicInteger index = new AtomicInteger();

			@Override
			public Thread newThread(Runnable r) {
				return new Thread(r, THREAD_PREFIX + index.incrementAndGet());
			}
		});
	}

	public Set<String> tubeSet() {
		return redisClient.smembers(tubeSetKey);
	}

	public Stats stats(String tubeName) {
		return loadTube(tubeName).stats();
	}

	private Tube loadTube(String tubeName) {
		Tube tube = tubeMap.get(tubeName);
		if (tube != null) {
			return tube;
		}

		if (contains(tubeName)) {
			return createTube(tubeName);
		}

		throw new BusinessException(TubeErrorCode.UNKOWN, "unkown tube name '" + tubeName + "'");
	}

	private boolean contains(String tubeName) {
		return redisClient.sismember(tubeSetKey, tubeName);
	}

	public Tube createTube(String tubeName) {
		Pattern p = Pattern.compile(TUBENAME_REGEX);
		if (!p.matcher(tubeName).matches()) {
			throw new BusinessException(TubeErrorCode.INVALID, "tube name not match " + TUBENAME_REGEX);
		}
		if (tubeName.length() > TUBENAME_LENGTH) {
			throw new BusinessException(TubeErrorCode.GREATE, "tube name length greate than " + TUBENAME_LENGTH);
		}

		Tube tube = tubeMap.get(tubeName);
		if (tube == null) {
			addTube(tubeName);
			tube = new Tube(group, tubeName, redisClient);
			tubeMap.putIfAbsent(tubeName, tube);
			tube = tubeMap.get(tubeName);
		}
		return tube;
	}

	public void deleteTube(String tubeName) {
		Tube tube = tubeMap.remove(tubeName);
		if (tube == null) {
			if (contains(tubeName)) {
				tube = new Tube(group, tubeName, redisClient);
			}
		}

		// remove tube
		removeTube(tubeName);

		// destroy tube
		if (tube != null) {
			tube.destroy();
		}
	}

	private void addTube(String tubeName) {
		LOG.info("add tube {}/{}", group, tubeName);
		redisClient.sadd(tubeSetKey, tubeName);
	}

	private void removeTube(String tubeName) {
		redisClient.srem(tubeSetKey, tubeName);
	}

	public boolean produce(String tubeName, Job job) {
		if (tubeName == null || tubeName.length() == 0) {
			throw new BusinessException(TubeErrorCode.EMPTY, "tubeName is empty");
		}
		if (job.getTtr() < DEFAULT_TTR) {
			job.setTtr(DEFAULT_TTR);
		}
		return loadTube(tubeName).produce(job);
	}

	public boolean delete(String tubeName, String jobId) {
		return loadTube(tubeName).delete(jobId);
	}

	public List<Job> consume(String tubeName, int max) {
		if (max < 1) {
			max = 1;
		}
		Tube tube = loadTube(tubeName);
		return tube.consume(max);
	}

	public boolean finish(String tubeName, String jobId) {
		return loadTube(tubeName).finish(jobId);
	}

	public boolean discard(String tubeName, String jobId) {
		return loadTube(tubeName).discard(jobId);
	}

	public List<Job> peek(String tubeName, long max) {
		return loadTube(tubeName).peek(max);
	}

	public boolean kick(String tubeName, String jobId, long dtr) {
		return loadTube(tubeName).kick(jobId, dtr);
	}

	/**
	 * move the job of tube to delay queue from reserved.
	 * 
	 * @param tubeName
	 * @param jobId
	 * @param dtr
	 * 
	 * @return
	 */
	public boolean release(String tubeName, String jobId, long dtr) {
		return loadTube(tubeName).release(jobId, dtr);
	}

	public boolean failure(String tubeName, String jobId) {
		return loadTube(tubeName).failure(jobId);
	}

	public Broker start() {
		executor.scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				try {
					dispatch();
				} catch (Exception e) {
					LOG.warn("dispatch {} error: {}", group, e.getMessage());
				}
			}
		}, 1, 2, TimeUnit.SECONDS);

		LOG.info("Broker[{}] is started", group);
		return this;
	}

	public Broker stop() {
		executor.shutdown();
		try {
			executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.HOURS);
		} catch (InterruptedException e) {
		}
		LOG.info("Broker[{}] is shutdown", group);
		return this;
	}

	private void dispatch() {
		Set<String> tubeSet = tubeSet();
		for (String tubeName : tubeSet) {
			if (!taskMap.containsKey(tubeName)) {
				ScheduledTask task = new ScheduledTask(createTube(tubeName));
				executor.scheduleWithFixedDelay(task, 0, 1, TimeUnit.SECONDS);
				taskMap.put(tubeName, task);
			}
		}
	}

	private final class ScheduledTask implements Runnable {
		private final Tube tube;

		private ScheduledTask(Tube tube) {
			this.tube = tube;
		}

		@Override
		public void run() {
			tube.schedule();
		}
	}

}

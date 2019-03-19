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

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SchedulerTest {

	public static void main(String[] args) throws IOException, InterruptedException {
		ScheduledExecutorService ses = Executors.newScheduledThreadPool(3);

		fixRate(ses);
		fixDelay(ses);

		System.in.read();

		ses.shutdown();

		ses.awaitTermination(1, TimeUnit.HOURS);
	}

	private static void fixDelay(ScheduledExecutorService ses) {
		ses.scheduleWithFixedDelay(new Runnable() {

			@Override
			public void run() {
				try {
					System.out.println(System.currentTimeMillis() + " : B");
					TimeUnit.SECONDS.sleep(3);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}, 1, 2, TimeUnit.SECONDS);
	}

	private static void fixRate(ScheduledExecutorService ses) {
		ses.scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				try {
					System.out.println(System.currentTimeMillis() + " : A");
					TimeUnit.SECONDS.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}
		}, 1, 2, TimeUnit.SECONDS);
	}

}

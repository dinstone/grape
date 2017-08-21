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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.dinstone.grape.core.Scheduler;

import redis.clients.jedis.JedisPool;

public class SchedulerTest {

    public static void main(String[] args) {
        JedisPool jedisPool = new JedisPool("127.0.0.1", 6379);

        List<Scheduler> schedulers = new ArrayList<>(3);
        for (int i = 0; i < 3; i++) {
            Scheduler scheduler = new Scheduler(jedisPool);
            scheduler.start();
            schedulers.add(scheduler);
        }

        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (Scheduler scheduler : schedulers) {
            scheduler.stop();
        }

        jedisPool.destroy();
    }

}

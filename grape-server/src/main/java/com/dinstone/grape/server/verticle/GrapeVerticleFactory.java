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
package com.dinstone.grape.server.verticle;

import java.util.concurrent.Callable;

import com.dinstone.grape.server.ApplicationContext;

import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import io.vertx.core.spi.VerticleFactory;

public class GrapeVerticleFactory implements VerticleFactory {

	private ApplicationContext context;

	public GrapeVerticleFactory(ApplicationContext context) {
		this.context = context;
	}

	@Override
	public String prefix() {
		return "grape";
	}

	public String getVerticleName(Class<?> verticleClass) {
		return prefix() + ":" + verticleClass.getName();
	}

	@Override
	public void createVerticle(String verticleName, ClassLoader classLoader, Promise<Callable<Verticle>> promise) {
		promise.complete(new Callable<Verticle>() {

			@Override
			public Verticle call() throws Exception {
				String clazz = VerticleFactory.removePrefix(verticleName);
				if (WebRestVerticle.class.getName().equals(clazz)) {
					return new WebRestVerticle(context);
				} else if (WebHttpVerticle.class.getName().equals(clazz)) {
					return new WebHttpVerticle(context);
				}
				throw new IllegalArgumentException("unsupported verticle type: " + clazz);
			}
		});
	}

}

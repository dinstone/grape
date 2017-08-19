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

package com.dinstone.grape.server;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

public final class VertxHelper {

	private static final Logger LOG = LoggerFactory.getLogger(VertxHelper.class);

	public static Vertx createVertx(VertxOptions vertxOptions) throws Exception {
		CompletableFuture<Vertx> future = new CompletableFuture<>();
		if (vertxOptions.isClustered()) {
			Vertx.clusteredVertx(vertxOptions, asyncResult -> {
				if (asyncResult.succeeded()) {
					future.complete(asyncResult.result());
				} else {
					future.completeExceptionally(asyncResult.cause());
				}
			});
		} else {
			future.complete(Vertx.vertx(vertxOptions));
		}

		return future.get();
	}

	public static boolean deployVerticle(Vertx vertx, DeploymentOptions deployOptions, String verticleName)
			throws Exception {
		CompletableFuture<Boolean> future = new CompletableFuture<>();
		vertx.deployVerticle(verticleName, deployOptions, deployResponse -> {
			if (deployResponse.succeeded()) {
				LOG.info("verticle deployment is succeeded, {}:{}", verticleName, deployResponse.result());
				future.complete(true);
			} else {
				LOG.error("verticle deployment is failed, {}:{}", verticleName, deployResponse.cause());
				future.completeExceptionally(deployResponse.cause());
			}
		});

		return future.get();
	}

	public static boolean deployVerticle(Vertx vertx, DeploymentOptions deployOptions, Verticle verticle)
			throws Exception {
		CompletableFuture<Boolean> future = new CompletableFuture<>();
		vertx.deployVerticle(verticle, deployOptions, deployResponse -> {
			if (deployResponse.succeeded()) {
				LOG.info("verticle deployment is succeeded, {}:{}", verticle.getClass().getName(),
						deployResponse.result());
				future.complete(true);
			} else {
				LOG.error("verticle deployment is failed, {}:{}", verticle.getClass().getName(),
						deployResponse.cause());
				future.completeExceptionally(deployResponse.cause());
			}
		});

		return future.get();
	}

}

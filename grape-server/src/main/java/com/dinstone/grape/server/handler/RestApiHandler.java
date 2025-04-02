/*
 * Copyright (C) 2016~2024 dinstone<dinstone@163.com>
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
package com.dinstone.grape.server.handler;

import java.util.LinkedHashMap;
import java.util.Map;

import com.dinstone.grape.core.Broker;
import com.dinstone.grape.exception.BusinessException;
import com.dinstone.grape.exception.ErrorCode;
import com.dinstone.grape.server.ApplicationContext;

import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;

public abstract class RestApiHandler {

	private static final int FAILURE_CODE = 9999;

	private static final int CLIENT_ERROR_STATUS_CODE = 400;
	private static final int SERVER_ERROR_STATUS_CODE = 500;

	protected Broker broker;

	public RestApiHandler(ApplicationContext context) {
		broker = context.getBroker();
	}

	protected void success(RoutingContext ctx) {
		success(ctx, null);
	}

	protected void success(RoutingContext ctx, Object result) {
		ctx.response().end(Json.encode(result));
	}

	protected void failed(RoutingContext ctx, ErrorCode code, String message) {
		Map<String, Object> jo = new LinkedHashMap<>();
		jo.put("code", code.getValue());
		jo.put("desc", message);
		ctx.response().setStatusCode(CLIENT_ERROR_STATUS_CODE).end(Json.encode(jo));
	}

	protected void failed(RoutingContext ctx, Throwable throwable) {
		Map<String, Object> jo = new LinkedHashMap<>();
		if (throwable instanceof BusinessException) {
			BusinessException error = (BusinessException) throwable;
			jo.put("code", error.getErrorCode().getValue());
			jo.put("desc", error.getMessage());
			ctx.response().setStatusCode(CLIENT_ERROR_STATUS_CODE);
		} else {
			jo.put("code", FAILURE_CODE);
			jo.put("desc", getMessage(throwable));
			ctx.response().setStatusCode(SERVER_ERROR_STATUS_CODE);
		}
		ctx.response().end(Json.encode(jo));
	}

	private String getMessage(Throwable throwable) {
		if (throwable == null) {
			return "unknown exception";
		}
		String message = throwable.getMessage();
		if (message == null) {
			return getMessage(throwable.getCause());
		}
		return message;
	}

}
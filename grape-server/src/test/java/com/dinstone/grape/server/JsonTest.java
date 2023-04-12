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
package com.dinstone.grape.server;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.dinstone.grape.core.Job;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

public class JsonTest {

    public static void main(String[] args) {
        jsonApi();

        List<Job> result = new ArrayList<>();
        result.add(new Job("j-1", 1000, 1200, new byte[] { 32, 33, 34 }));
        result.add(new Job("j-1", 1000, 1200, new byte[] { 36, 37, 38 }));

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("code", "1");
        res.put("data", result);

        System.out.println(Json.encode(res));

        System.out.println(Json.encode(true));
        System.out.println(Json.encode("i'm string"));

        // Failed to decode: Unrecognized token 'i': was expecting ('true', 'false' or
        // 'null')
        // System.out.println(Json.decodeValue("i'm another string"));
    }

    private static void jsonApi() {
        JsonObject res = new JsonObject().put("code", "1");
        List<Job> result = new ArrayList<>();
        result.add(new Job("j-1", 1000, 1200, new byte[] { 32, 33, 34 }));
        result.add(new Job("j-1", 1000, 1200, new byte[] { 36, 37, 38 }));
        if (result != null) {
            res.put("data", result);
        }
        System.out.println(res.encode());
    }

}

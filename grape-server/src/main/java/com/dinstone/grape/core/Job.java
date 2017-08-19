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

import java.io.Serializable;

public class Job implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * job id
     */
    private String id;

    /**
     * delay to run
     */
    private long dtr;

    /**
     * time to run
     */
    private long ttr;

    /**
     * number of executions
     */
    private long noe;

    /**
     * job content
     */
    private byte[] data;

    public Job() {
        super();
    }

    public Job(String id, long dtr, long ttr, byte[] data) {
        super();
        this.id = id;
        this.dtr = dtr;
        this.ttr = ttr;
        this.data = data;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getDtr() {
        return dtr;
    }

    public void setDtr(long dtr) {
        this.dtr = dtr;
    }

    public long getTtr() {
        return ttr;
    }

    public void setTtr(long ttr) {
        this.ttr = ttr;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public long getNoe() {
        return noe;
    }

    public void setNoe(long noe) {
        this.noe = noe;
    }

}

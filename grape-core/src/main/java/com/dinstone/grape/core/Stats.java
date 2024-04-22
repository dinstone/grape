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

import java.io.Serializable;
import java.util.Date;

public class Stats implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private Date dateTime;

    private String tubeName;

    private long delayQueueSize;

    private long retainQueueSize;

    private long failedQueueSize;

    public Stats() {
        dateTime = new Date();
    }

    public Date getDateTime() {
        return dateTime;
    }

    public String getTubeName() {
        return tubeName;
    }

    public void setTubeName(String tubeName) {
        this.tubeName = tubeName;
    }

    public long getDelayQueueSize() {
        return delayQueueSize;
    }

    public void setDelayQueueSize(long delayQueueSize) {
        this.delayQueueSize = delayQueueSize;
    }

    public long getFailedQueueSize() {
        return failedQueueSize;
    }

    public void setFailedQueueSize(long failedQueueSize) {
        this.failedQueueSize = failedQueueSize;
    }

    public long getRetainQueueSize() {
        return retainQueueSize;
    }

    public void setRetainQueueSize(long retainQueueSize) {
        this.retainQueueSize = retainQueueSize;
    }

    @Override
    public String toString() {
        return "Stats [tubeName=" + tubeName + ", delayQueueSize=" + delayQueueSize + ", retainQueueSize=" + retainQueueSize + ", failedQueueSize=" + failedQueueSize + "]";
    }

}

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

import java.io.Serializable;

public class Stats implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private String tubeName;

    private long totalJobSize;

    private long finishJobSize;

    private long delayQueueSize;

    private long readyQueueSize;

    private long failedQueueSize;

    public String getTubeName() {
        return tubeName;
    }

    public void setTubeName(String tubeName) {
        this.tubeName = tubeName;
    }

    public long getTotalJobSize() {
        return totalJobSize;
    }

    public void setTotalJobSize(long totalJobSize) {
        this.totalJobSize = totalJobSize;
    }

    public long getFinishJobSize() {
        return finishJobSize;
    }

    public void setFinishJobSize(long finishJobSize) {
        this.finishJobSize = finishJobSize;
    }

    public long getDelayQueueSize() {
        return delayQueueSize;
    }

    public void setDelayQueueSize(long delayQueueSize) {
        this.delayQueueSize = delayQueueSize;
    }

    public long getReadyQueueSize() {
        return readyQueueSize;
    }

    public void setReadyQueueSize(long readyQueueSize) {
        this.readyQueueSize = readyQueueSize;
    }

    public long getFailedQueueSize() {
        return failedQueueSize;
    }

    public void setFailedQueueSize(long failedQueueSize) {
        this.failedQueueSize = failedQueueSize;
    }

    @Override
    public String toString() {
        return "Stats [tubeName=" + tubeName + ", totalJobSize=" + totalJobSize + ", finishJobSize=" + finishJobSize
                + ", delayQueueSize=" + delayQueueSize + ", readyQueueSize=" + readyQueueSize + ", failedQueueSize="
                + failedQueueSize + "]";
    }

}

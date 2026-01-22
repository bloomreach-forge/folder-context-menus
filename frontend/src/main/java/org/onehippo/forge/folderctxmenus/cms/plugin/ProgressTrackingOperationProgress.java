/*
 * Copyright 2025 Bloomreach (https://www.bloomreach.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.forge.folderctxmenus.cms.plugin;

import org.onehippo.forge.folderctxmenus.common.OperationProgress;

public class ProgressTrackingOperationProgress implements OperationProgress {

    private volatile long currentCount;
    private volatile long totalCount;
    private volatile String currentPath;
    private volatile boolean cancelled;
    private volatile boolean completed;
    private volatile long startTimeNanos;

    @Override
    public void updateProgress(long current, long total, String currentItemPath) {
        if (startTimeNanos == 0) {
            startTimeNanos = System.nanoTime();
        }
        this.currentCount = current;
        this.totalCount = total;
        this.currentPath = currentItemPath;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    public void cancel() {
        this.cancelled = true;
    }

    public long getCurrentCount() {
        return currentCount;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public String getCurrentPath() {
        return currentPath;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public void markCompleted() {
        this.completed = true;
    }

    public int getProgressPercentage() {
        if (totalCount == 0) {
            return 0;
        }
        return (int) ((currentCount * 100) / totalCount);
    }

    public String getEstimatedTimeRemaining() {
        if (currentCount == 0 || totalCount == 0 || startTimeNanos == 0) {
            return "";
        }

        long elapsedNanos = System.nanoTime() - startTimeNanos;
        double itemsPerNano = (double) currentCount / elapsedNanos;
        long remainingItems = totalCount - currentCount;
        if (remainingItems <= 0) {
            return "";
        }
        long remainingNanos = (long) (remainingItems / itemsPerNano);
        long remainingSeconds = remainingNanos / 1_000_000_000;

        if (remainingSeconds < 60) {
            return remainingSeconds + "s remaining";
        } else {
            return (remainingSeconds / 60) + "m " + (remainingSeconds % 60) + "s remaining";
        }
    }

}

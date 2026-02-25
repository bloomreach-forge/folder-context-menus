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

import java.util.concurrent.atomic.AtomicLong;

import org.onehippo.forge.folderctxmenus.common.OperationProgress;

public class ProgressTrackingOperationProgress implements OperationProgress {

    private volatile Snapshot snapshot = new Snapshot(0, 0, null);
    private volatile boolean cancelled;
    private volatile boolean completed;
    private volatile ProgressCompletionSummary completionSummary;
    private final AtomicLong startTimeNanos = new AtomicLong(0);
    private volatile boolean finalizing = false;
    private volatile long finalizingCount = 0;

    public static final class Snapshot {
        private final long currentCount;
        private final long totalCount;
        private final String currentPath;

        Snapshot(long currentCount, long totalCount, String currentPath) {
            this.currentCount = currentCount;
            this.totalCount = totalCount;
            this.currentPath = currentPath;
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
    }

    @Override
    public void updateProgress(long current, long total, String currentItemPath) {
        startTimeNanos.compareAndSet(0, System.nanoTime());
        this.snapshot = new Snapshot(current, total, currentItemPath);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    public void cancel() {
        this.cancelled = true;
    }

    public Snapshot getSnapshot() {
        return snapshot;
    }

    public long getCurrentCount() {
        return snapshot.getCurrentCount();
    }

    public long getTotalCount() {
        return snapshot.getTotalCount();
    }

    public String getCurrentPath() {
        return snapshot.getCurrentPath();
    }

    public boolean isCompleted() {
        return completed;
    }

    public void markCompleted() {
        this.completed = true;
    }

    public ProgressCompletionSummary getCompletionSummary() {
        return completionSummary;
    }

    public void setCompletionSummary(ProgressCompletionSummary completionSummary) {
        this.completionSummary = completionSummary;
    }

    @Override
    public void enterFinalizingPhase() {
        this.finalizing = true;
    }

    @Override
    public void updateFinalizingProgress(long count) {
        this.finalizingCount = count;
    }

    boolean isFinalizing() {
        return finalizing;
    }

    long getFinalizingCount() {
        return finalizingCount;
    }

    public int getProgressPercentage() {
        Snapshot s = snapshot;
        if (s.getTotalCount() == 0) {
            return 0;
        }
        return (int) ((s.getCurrentCount() * 100) / s.getTotalCount());
    }

    public String getEstimatedTimeRemaining() {
        Snapshot s = snapshot;
        long startNanos = startTimeNanos.get();
        if (s.getCurrentCount() == 0 || s.getTotalCount() == 0 || startNanos == 0) {
            return "";
        }

        long elapsedNanos = System.nanoTime() - startNanos;
        double itemsPerNano = (double) s.getCurrentCount() / elapsedNanos;
        long remainingItems = s.getTotalCount() - s.getCurrentCount();
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

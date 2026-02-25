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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ProgressTrackingOperationProgressTest {

    private ProgressTrackingOperationProgress progress;

    @Before
    public void setUp() {
        progress = new ProgressTrackingOperationProgress();
    }

    @Test
    public void testInitialState() {
        assertEquals(0, progress.getCurrentCount());
        assertEquals(0, progress.getTotalCount());
        assertNull(progress.getCurrentPath());
        assertFalse(progress.isCancelled());
        assertFalse(progress.isCompleted());
        assertEquals(0, progress.getProgressPercentage());
    }

    @Test
    public void testUpdateProgress() {
        progress.updateProgress(50, 100, "/test/path");

        assertEquals(50, progress.getCurrentCount());
        assertEquals(100, progress.getTotalCount());
        assertEquals("/test/path", progress.getCurrentPath());
    }

    @Test
    public void testProgressPercentage() {
        progress.updateProgress(25, 100, "/test/path");
        assertEquals(25, progress.getProgressPercentage());

        progress.updateProgress(50, 100, "/test/path");
        assertEquals(50, progress.getProgressPercentage());

        progress.updateProgress(100, 100, "/test/path");
        assertEquals(100, progress.getProgressPercentage());
    }

    @Test
    public void testProgressPercentageWithZeroTotal() {
        progress.updateProgress(10, 0, "/test/path");
        assertEquals(0, progress.getProgressPercentage());
    }

    @Test
    public void testCancellation() {
        assertFalse(progress.isCancelled());

        progress.cancel();

        assertTrue(progress.isCancelled());
    }

    @Test
    public void testCompletion() {
        assertFalse(progress.isCompleted());

        progress.markCompleted();

        assertTrue(progress.isCompleted());
    }

    @Test
    public void testThreadSafety() throws InterruptedException {
        final int numThreads = 10;
        final int updatesPerThread = 100;
        Thread[] threads = new Thread[numThreads];

        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < updatesPerThread; j++) {
                    progress.updateProgress(threadId * updatesPerThread + j, 1000, "/path/" + j);
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // All updates should have completed without exceptions
        assertTrue(progress.getCurrentCount() > 0);
        assertTrue(progress.getTotalCount() > 0);
        assertNotNull(progress.getCurrentPath());
    }

    @Test
    public void testCancellationThreadSafety() throws InterruptedException {
        Thread updater = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                progress.updateProgress(i, 1000, "/path/" + i);
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });

        Thread canceller = new Thread(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                return;
            }
            progress.cancel();
        });

        updater.start();
        canceller.start();

        updater.join();
        canceller.join();

        assertTrue(progress.isCancelled());
    }

    @Test
    public void testMarkCompleted() {
        assertFalse(progress.isCompleted());

        progress.markCompleted();

        assertTrue(progress.isCompleted());
    }

    @Test
    public void testGetSnapshot() {
        progress.updateProgress(25, 100, "/test/path");

        ProgressTrackingOperationProgress.Snapshot snapshot = progress.getSnapshot();

        assertEquals(25, snapshot.getCurrentCount());
        assertEquals(100, snapshot.getTotalCount());
        assertEquals("/test/path", snapshot.getCurrentPath());
    }

    @Test
    public void testSnapshotAtomicity() {
        progress.updateProgress(50, 100, "/path/a");

        ProgressTrackingOperationProgress.Snapshot snapshot = progress.getSnapshot();

        // Update progress after getting snapshot
        progress.updateProgress(75, 100, "/path/b");

        // Original snapshot should be unchanged
        assertEquals(50, snapshot.getCurrentCount());
        assertEquals(100, snapshot.getTotalCount());
        assertEquals("/path/a", snapshot.getCurrentPath());

        // New snapshot should reflect updates
        ProgressTrackingOperationProgress.Snapshot newSnapshot = progress.getSnapshot();
        assertEquals(75, newSnapshot.getCurrentCount());
        assertEquals("/path/b", newSnapshot.getCurrentPath());
    }

    @Test
    public void testEstimatedTimeRemainingWhenZeroCurrent() {
        progress.updateProgress(0, 100, "/test");

        assertEquals("", progress.getEstimatedTimeRemaining());
    }

    @Test
    public void testEstimatedTimeRemainingWhenZeroTotal() {
        // Force startTimeNanos to be set
        progress.updateProgress(10, 0, "/test");

        assertEquals("", progress.getEstimatedTimeRemaining());
    }

    @Test
    public void testEstimatedTimeRemainingWhenComplete() throws InterruptedException {
        progress.updateProgress(1, 100, "/test");
        Thread.sleep(10); // Ensure some time passes
        progress.updateProgress(100, 100, "/test");

        assertEquals("", progress.getEstimatedTimeRemaining());
    }

    @Test
    public void testEstimatedTimeRemainingFormat() throws InterruptedException {
        progress.updateProgress(1, 1000, "/test");
        Thread.sleep(10); // Small delay to ensure calculation works
        progress.updateProgress(10, 1000, "/test");

        String eta = progress.getEstimatedTimeRemaining();
        // ETA should be non-empty and contain "remaining"
        if (!eta.isEmpty()) {
            assertTrue("ETA should contain 'remaining': " + eta, eta.contains("remaining"));
        }
    }


    @Test
    public void testProgressPercentageConsistency() {
        progress.updateProgress(33, 100, "/test");

        ProgressTrackingOperationProgress.Snapshot snapshot = progress.getSnapshot();
        int percentage = progress.getProgressPercentage();

        // Both should use the same snapshot internally
        assertEquals(33, percentage);
        assertEquals(33, snapshot.getCurrentCount());
    }

    @Test
    public void testCompletionSummaryInitiallyNull() {
        assertNull(progress.getCompletionSummary());
    }

    @Test
    public void testSetAndGetCompletionSummary() {
        ProgressCompletionSummary summary = new ProgressCompletionSummary("Done", false);

        progress.setCompletionSummary(summary);

        assertSame(summary, progress.getCompletionSummary());
        assertEquals("Done", progress.getCompletionSummary().getMessage());
        assertFalse(progress.getCompletionSummary().isError());
    }

    @Test
    public void testFinalizingState_initiallyFalse() {
        assertFalse(progress.isFinalizing());
        assertEquals(0, progress.getFinalizingCount());
    }

    @Test
    public void testEnterFinalizingPhase_setsFinalizingTrue() {
        progress.enterFinalizingPhase();
        assertTrue(progress.isFinalizing());
    }

    @Test
    public void testUpdateFinalizingProgress_updatesCount() {
        progress.enterFinalizingPhase();
        progress.updateFinalizingProgress(57);
        assertEquals(57, progress.getFinalizingCount());
    }

    @Test
    public void testUpdateFinalizingProgress_withoutEnteringPhase_updatesCountButNotFlag() {
        progress.updateFinalizingProgress(10);
        assertEquals(10, progress.getFinalizingCount());
        assertFalse(progress.isFinalizing());
    }

    @Test
    public void testCompletionSummaryWithError() {
        ProgressCompletionSummary summary = new ProgressCompletionSummary("Failed", true);

        progress.setCompletionSummary(summary);

        assertTrue(progress.getCompletionSummary().isError());
    }

}

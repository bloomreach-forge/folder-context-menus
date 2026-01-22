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

        progress.setCompleted(true);

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

}

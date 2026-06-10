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

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Shared thread pool for all folder context menu background operations (copy, move, delete).
 *
 * <p>A single shared pool prevents unbounded thread growth when multiple users run folder
 * operations concurrently. The maximum thread count is controlled by the system property
 * {@code folderctxmenus.maxConcurrentOps} (default: 4).</p>
 */
final class FolderOperationExecutors {

    static final int MAX_CONCURRENT_OPERATIONS =
            Integer.getInteger("folderctxmenus.maxConcurrentOps", 4);

    static final ThreadPoolExecutor SHARED_EXECUTOR = new ThreadPoolExecutor(
            1, MAX_CONCURRENT_OPERATIONS,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(16),
            r -> {
                Thread t = new Thread(r, "FolderContextMenus-Operation");
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    private FolderOperationExecutors() {
    }
}

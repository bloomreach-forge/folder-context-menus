/*
 * Copyright 2025 Bloomreach (https://www.bloomreach.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.forge.folderctxmenus.common;

public interface OperationProgress {

    void updateProgress(long current, long total, String currentItemPath);

    boolean isCancelled();

    /**
     * Hook called after progress is updated. Implementations can override
     * to add behavior such as throttling or debug delays.
     */
    default void onProgressUpdated() {
        // no-op by default
    }

    default void enterFinalizingPhase() {}

    default void updateFinalizingProgress(long count) {}

}

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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OperationProgressTest {

    @Test
    void onProgressUpdated_defaultImplementationShouldBeNoOp() {
        OperationProgress progress = new OperationProgress() {
            @Override
            public void updateProgress(long current, long total, String currentItemPath) {
                // no-op
            }

            @Override
            public boolean isCancelled() {
                return false;
            }
        };

        // Default implementation should not throw
        progress.onProgressUpdated();
    }

    @Test
    void onProgressUpdated_canBeOverridden() {
        final boolean[] hookCalled = {false};

        OperationProgress progress = new OperationProgress() {
            @Override
            public void updateProgress(long current, long total, String currentItemPath) {
                // no-op
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public void onProgressUpdated() {
                hookCalled[0] = true;
            }
        };

        progress.onProgressUpdated();

        assertTrue(hookCalled[0], "Custom onProgressUpdated should have been called");
    }
}

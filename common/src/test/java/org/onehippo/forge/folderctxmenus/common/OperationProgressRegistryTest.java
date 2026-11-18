/*
 * Copyright 2026 Bloomreach (https://www.bloomreach.com)
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

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class OperationProgressRegistryTest {

    private static OperationProgress newProgress() {
        return new OperationProgress() {
            @Override
            public void updateProgress(long current, long total, String currentItemPath) {
                // no-op
            }

            @Override
            public boolean isCancelled() {
                return false;
            }
        };
    }

    @Test
    void registerThenGet_returnsSameInstance() {
        OperationProgress progress = newProgress();
        OperationProgressRegistry.register("op-1", progress);

        try {
            assertSame(progress, OperationProgressRegistry.get("op-1"));
        } finally {
            OperationProgressRegistry.unregister("op-1");
        }
    }

    @Test
    void get_unknownId_returnsNull() {
        assertNull(OperationProgressRegistry.get("unknown-op"));
    }

    @Test
    void get_nullId_returnsNull() {
        assertNull(OperationProgressRegistry.get(null));
    }

    @Test
    void unregister_removesEntry() {
        OperationProgressRegistry.register("op-2", newProgress());
        OperationProgressRegistry.unregister("op-2");

        assertNull(OperationProgressRegistry.get("op-2"));
    }

    @Test
    void register_nullId_isNoOp() {
        OperationProgressRegistry.register(null, newProgress());

        assertNull(OperationProgressRegistry.get(null));
    }

    @Test
    void register_nullProgress_isNoOp() {
        OperationProgressRegistry.register("op-3", null);

        assertNull(OperationProgressRegistry.get("op-3"));
    }

    @Test
    void unregister_unknownId_doesNotThrow() {
        OperationProgressRegistry.unregister("never-registered");
    }

}

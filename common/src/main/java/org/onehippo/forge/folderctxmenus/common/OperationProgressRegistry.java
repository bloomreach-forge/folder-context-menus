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

import java.util.concurrent.ConcurrentHashMap;

/**
 * In-JVM registry bridging a live {@link OperationProgress} instance across a {@link ExtendedFolderWorkflow}
 * invocation. Workflow method arguments are logged/serialized by the repository's workflow layer, so the
 * progress object itself cannot be passed as a parameter; instead the caller registers it under an
 * operation id, passes only that id to the workflow, and the workflow implementation looks it up.
 * <p>
 * Callers must always {@link #unregister(String)} in a {@code finally} block to avoid leaking entries.
 */
public final class OperationProgressRegistry {

    private static final ConcurrentHashMap<String, OperationProgress> OPERATIONS = new ConcurrentHashMap<>();

    private OperationProgressRegistry() {
    }

    public static void register(final String operationId, final OperationProgress progress) {
        if (operationId == null || progress == null) {
            return;
        }
        OPERATIONS.put(operationId, progress);
    }

    public static OperationProgress get(final String operationId) {
        if (operationId == null) {
            return null;
        }
        return OPERATIONS.get(operationId);
    }

    public static void unregister(final String operationId) {
        if (operationId == null) {
            return;
        }
        OPERATIONS.remove(operationId);
    }

}

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

import java.util.concurrent.atomic.AtomicLong;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.util.DefaultCopyHandler;
import org.hippoecm.repository.util.NodeInfo;

public class ProgressTrackingCopyHandler extends DefaultCopyHandler {

    private final OperationProgress progress;
    private final AtomicLong counter;
    private final long total;

    public ProgressTrackingCopyHandler(Node destParentNode, OperationProgress progress,
            AtomicLong counter, long total) throws RepositoryException {
        super(destParentNode);
        this.progress = progress;
        this.counter = counter;
        this.total = total;
    }

    @Override
    public void startNode(NodeInfo nodeInfo) throws RepositoryException {
        checkCancelled();
        super.startNode(nodeInfo);
        reportProgressIfUserVisible();
    }

    private void checkCancelled() throws RepositoryException {
        if (progress != null && progress.isCancelled()) {
            throw new OperationCancelledException("Operation was cancelled");
        }
    }

    private void reportProgressIfUserVisible() throws RepositoryException {
        if (progress == null) {
            return;
        }

        Node current = getCurrent();
        if (current == null) {
            return;
        }

        if (!NodeTraverser.USER_VISIBLE_ITEMS.isAcceptable(current)) {
            return;
        }

        String path = current.getPath();
        progress.updateProgress(counter.incrementAndGet(), total, path);
        progress.onProgressUpdated();
    }

}

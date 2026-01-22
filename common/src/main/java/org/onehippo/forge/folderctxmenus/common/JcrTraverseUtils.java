/*
 * Copyright 2024 Bloomreach (https://www.bloomreach.com)
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
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

public class JcrTraverseUtils {

    private static final long DEBUG_DELAY_MS = Long.getLong("folderctxmenus.debug.delay", 0);

    private JcrTraverseUtils() {
    }

    public static void traverseNodes(final Node node, NodeTraverser nodeTraverser) throws RepositoryException {
        if (nodeTraverser == null) {
            throw new IllegalArgumentException("Provide a non-null nodeTraverser!");
        }

        if (nodeTraverser.isAcceptable(node)) {
            nodeTraverser.accept(node);
        }

        if (nodeTraverser.isTraversable(node) && node.hasNodes()) {
            Node childNode;

            for (NodeIterator nodeIt = node.getNodes(); nodeIt.hasNext(); ) {
                childNode = nodeIt.nextNode();

                if (childNode != null) {
                    traverseNodes(childNode, nodeTraverser);
                }
            }
        }
    }

    public static void traverseNodes(final Node node, final NodeTraverser nodeTraverser,
            final OperationProgress progress, final AtomicLong counter, final long total) throws RepositoryException {
        if (nodeTraverser == null) {
            throw new IllegalArgumentException("Provide a non-null nodeTraverser!");
        }

        if (progress != null && progress.isCancelled()) {
            throw new OperationCancelledException("Operation was cancelled");
        }

        if (nodeTraverser.isAcceptable(node)) {
            if (progress != null) {
                progress.updateProgress(counter.incrementAndGet(), total, node.getPath());

                if (DEBUG_DELAY_MS > 0) {
                    try {
                        Thread.sleep(DEBUG_DELAY_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            nodeTraverser.accept(node);
        }

        if (nodeTraverser.isTraversable(node) && node.hasNodes()) {
            for (NodeIterator nodeIt = node.getNodes(); nodeIt.hasNext(); ) {
                Node childNode = nodeIt.nextNode();

                if (childNode != null) {
                    traverseNodes(childNode, nodeTraverser, progress, counter, total);
                }
            }
        }
    }

}

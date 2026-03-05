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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

public class NodeCounter {

    private NodeCounter() {
    }

    public static long countNodes(final Node node, final NodeTraverser nodeTraverser) throws RepositoryException {
        if (nodeTraverser == null) {
            throw new IllegalArgumentException("Provide a non-null nodeTraverser!");
        }

        long count = 0;

        if (nodeTraverser.isAcceptable(node)) {
            count++;
        }

        if (nodeTraverser.isTraversable(node) && node.hasNodes()) {
            for (NodeIterator nodeIt = node.getNodes(); nodeIt.hasNext(); ) {
                Node childNode = nodeIt.nextNode();

                if (childNode != null) {
                    count += countNodes(childNode, nodeTraverser);
                }
            }
        }

        return count;
    }

}

/*
 * Copyright 2023 Bloomreach (https://www.bloomreach.com)
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

public class JcrTraverseUtils {

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

}

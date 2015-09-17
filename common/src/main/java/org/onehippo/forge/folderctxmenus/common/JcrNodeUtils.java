/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

public class JcrNodeUtils {

    public interface NodeFilter {

        boolean accept(Node node) throws RepositoryException;

        boolean searchDeeper(Node node) throws RepositoryException;

    }

    private JcrNodeUtils() {
    }

    public static Collection<Node> findNodes(final Node baseNode, NodeFilter nodeFilter) throws RepositoryException {
        if (nodeFilter == null) {
            throw new IllegalArgumentException("Provide a non-null nodeFitler!");
        }

        List<Node> nodes = new LinkedList<>();
        fillNodes(nodes, baseNode, nodeFilter);
        return nodes;
    }

    private static void fillNodes(final Collection<Node> nodes, final Node baseNode,
            final NodeFilter nodeFilter) throws RepositoryException {
        if (nodeFilter.accept(baseNode)) {
            nodes.add(baseNode);
        }

        if (nodeFilter.searchDeeper(baseNode)) {
            Node childNode;

            for (NodeIterator nodeIt = baseNode.getNodes(); nodeIt.hasNext(); ) {
                childNode = nodeIt.nextNode();

                if (childNode != null) {
                    fillNodes(nodes, childNode, nodeFilter);
                }
            }
        }
    }
}

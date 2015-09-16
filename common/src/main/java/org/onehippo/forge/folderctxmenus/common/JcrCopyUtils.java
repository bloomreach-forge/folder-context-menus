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

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.util.CopyHandler;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeInfo;

public class JcrCopyUtils {

    private JcrCopyUtils() {
    }

    public static Node copy(final Node srcNode, final String destNodeName, final Node destParentNode)
            throws RepositoryException {
        return JcrUtils.copy(srcNode, destNodeName, destParentNode);
    }

    public static Node copy(final Node srcNode, final String destNodeName, final Node destParentNode,
            final CopyHandler copyHandler) throws RepositoryException {

        if (copyHandler == null) {
            throw new IllegalArgumentException("copyHandler must not be null!");
        }

        if (JcrUtils.isVirtual(srcNode)) {
            return null;
        }

        if (destNodeName.indexOf('/') != -1) {
            throw new IllegalArgumentException(destNodeName + " is a path, not a name");
        }

        if (srcNode.isSame(destParentNode)) {
            throw new IllegalArgumentException("Destination parent node cannot be the same as source node");
        }

        if (isAncestor(srcNode, destParentNode)) {
            throw new IllegalArgumentException("Destination parent node cannot be descendant of source node");
        }

        final NodeInfo nodeInfo = new NodeInfo(srcNode);
        final NodeInfo newInfo = new NodeInfo(destNodeName, 0, nodeInfo.getNodeTypeName(), nodeInfo.getMixinNames());
        copyHandler.startNode(newInfo);
        final Node destNode = copyHandler.getCurrent();
        JcrUtils.copyToChain(srcNode, copyHandler);
        copyHandler.endNode();

        return destNode;
    }

    private static boolean isAncestor(final Node ancestor, final Node descendant) throws RepositoryException {
        try {
            Node node = descendant;

            do {
                node = node.getParent();
            } while (!ancestor.isSame(node));

            return true;
        } catch (ItemNotFoundException e) {
            return false;
        }
    }

}

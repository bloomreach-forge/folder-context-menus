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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import com.google.common.base.Strings;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.hippoecm.repository.util.JcrUtils;

public class FolderCopyTask extends AbstractFolderCopyOrMoveTask {

    public FolderCopyTask(final Session session, final Locale locale, final Node sourceFolderNode,
            final Node destParentFolderNode, final String destFolderNodeName, final String destFolderDisplayName,
            final Boolean resetTranslations) {
        super(session, locale, sourceFolderNode, destParentFolderNode, destFolderNodeName, destFolderDisplayName, resetTranslations);
    }

    @Override
    protected void doExecute() throws RepositoryException {
        if (getSourceFolderNode().getParent().isSame(getDestParentFolderNode())) {
            if (StringUtils.equals(getSourceFolderNode().getName(), getDestFolderNodeName())) {
                throw new RuntimeException("Cannot copy to the same folder: " + getDestParentFolderNode().getPath()
                        + " / " + getDestFolderNodeName());
            }
        }

        if (getSourceFolderNode().isSame(getDestParentFolderNode())) {
            throw new RuntimeException("Cannot copy to the folder itself: " + getDestFolderPath());
        }

        if (getSession().nodeExists(getDestFolderPath())) {
            throw new RuntimeException("Destination folder already exists: " + getDestFolderPath());
        }

        getLogger().info("Copying nodes: from {} to {}.", getSourceFolderNode().getPath(), getDestFolderPath());

        if (getCopyHandler() != null) {
            JcrCopyUtils.copy(getSourceFolderNode(), getDestFolderNodeName(), getDestParentFolderNode(),
                    getCopyHandler());
        } else {
            JcrCopyUtils.copy(getSourceFolderNode(), getDestFolderNodeName(), getDestParentFolderNode());
        }

        setDestFolderNode(JcrUtils.getNodeIfExists(getDestParentFolderNode(), getDestFolderNodeName()));

        updateFolderTranslations(getDestFolderNode(), getDestFolderDisplayName(), getLocale().getLanguage());
    }

    @Override
    protected void doAfterExecute() throws RepositoryException {
        resetHippoDocBaseLinks();
        takeOfflineHippoDocs();
        resetHippoDocumentTranslationIds(getResetTranslations());
    }

    /**
     * Search all the link holder nodes having hippo:docbase property under destFolderNode
     * and reset the hippo:docbase properties to the copied nodes under destFolderNode
     * by comparing the relative paths with the corresponding nodes under the sourceFolderNode.
     */
    protected void resetHippoDocBaseLinks() {
        String destFolderNodePath = null;

        try {
            destFolderNodePath = getDestFolderNode().getPath();

            JcrTraverseUtils.traverseNodes(getDestFolderNode(),
                    new NodeTraverser() {
                        private final String sourceFolderBase = getSourceFolderNode().getPath() + "/";

                        @Override
                        public boolean isAcceptable(Node node) throws RepositoryException {
                            return node.isNodeType("hippo:mirror") && node.hasProperty("hippo:docbase");
                        }

                        @Override
                        public boolean isTraversable(Node node) throws RepositoryException {
                            return !node.isNodeType("hippo:mirror");
                        }

                        @Override
                        public void accept(Node destLinkHolderNode) throws RepositoryException {
                            String destLinkDocBase = JcrUtils.getStringProperty(destLinkHolderNode, "hippo:docbase", null);

                            if (StringUtils.isNotBlank(destLinkDocBase)) {
                                try {
                                    Node sourceLinkedNode = getSession().getNodeByIdentifier(destLinkDocBase);

                                    if (StringUtils.startsWith(sourceLinkedNode.getPath(), sourceFolderBase)) {
                                        String sourceLinkedNodeRelPath = StringUtils.removeStart(sourceLinkedNode.getPath(),
                                                sourceFolderBase);
                                        Node destLinkedNode = JcrUtils.getNodeIfExists(getDestFolderNode(), sourceLinkedNodeRelPath);

                                        if (destLinkedNode != null) {
                                            getLogger().info("Updating the linked node at '{}'.", destLinkHolderNode.getPath());
                                            destLinkHolderNode.setProperty("hippo:docbase", destLinkedNode.getIdentifier());
                                        }
                                    }
                                } catch (ItemNotFoundException ignore) {
                                }
                            }
                        }
                    });
        } catch (RepositoryException e) {
            getLogger().error("Failed to reset link Nodes under destination folder: {}.", destFolderNodePath, e);
        }
    }

    /**
     * Takes offline all the hippo documents under the {@code destFolderNode}.
     */
    protected void takeOfflineHippoDocs() {
        String destFolderNodePath = null;

        try {
            destFolderNodePath = getDestFolderNode().getPath();

            JcrTraverseUtils.traverseNodes(getDestFolderNode(),
                    new NodeTraverser() {
                        @Override
                        public boolean isAcceptable(Node node) throws RepositoryException {
                            if (!node.isNodeType("hippostdpubwf:document")) {
                                return false;
                            }

                            return isLiveVariantNode(node) &&
                                    StringUtils.equals("published", JcrUtils.getStringProperty(node, "hippostd:state", null));
                        }

                        @Override
                        public boolean isTraversable(Node node) throws RepositoryException {
                            return !node.isNodeType("hippostdpubwf:document");
                        }

                        private boolean isLiveVariantNode(final Node variantNode) throws RepositoryException {
                            if (variantNode.hasProperty("hippo:availability")) {
                                for (Value value : variantNode.getProperty("hippo:availability").getValues()) {
                                    if (StringUtils.equals("live", value.getString())) {
                                        return true;
                                    }
                                }
                            }

                            return false;
                        }

                        @Override
                        public void accept(Node liveVariant) throws RepositoryException {
                            liveVariant.setProperty("hippo:availability", ArrayUtils.EMPTY_STRING_ARRAY);
                            liveVariant.setProperty("hippostd:stateSummary", "new");
                        }
                    });
        } catch (RepositoryException e) {
            getLogger().error("Failed to take offline link hippostd:publishableSummary nodes under {}.", destFolderNodePath, e);
        }
    }

}

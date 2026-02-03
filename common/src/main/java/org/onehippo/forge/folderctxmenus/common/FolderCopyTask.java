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

import java.util.Locale;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

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
    protected String getOperationName() {
        return "Copying";
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

        try {
            if (getOperationProgress() != null) {
                long totalNodes = countSourceNodes();
                JcrCopyUtils.copy(getSourceFolderNode(), getDestFolderNodeName(), getDestParentFolderNode(),
                        getOperationProgress(), totalNodes);
            } else if (getCopyHandler() != null) {
                JcrCopyUtils.copy(getSourceFolderNode(), getDestFolderNodeName(), getDestParentFolderNode(),
                        getCopyHandler());
            } else {
                JcrCopyUtils.copy(getSourceFolderNode(), getDestFolderNodeName(), getDestParentFolderNode());
            }
        } finally {
            // Set destFolderNode even on cancellation so doAfterExecute can clean up partial copy
            setDestFolderNode(JcrUtils.getNodeIfExists(getDestParentFolderNode(), getDestFolderNodeName()));
        }

        updateFolderTranslations(getDestFolderNode(), getDestFolderDisplayName(), getLocale().getLanguage());
    }

    @Override
    protected void doAfterExecute() throws RepositoryException {
        // Combined post-processing: all operations in single traversal
        performDestinationPostProcessing(getResetTranslations(), false);
        logOperationCompleted();
    }

    @Override
    protected void processNodeDuringPostProcessing(Node node, PostProcessingContext context) throws RepositoryException {
        // Call parent for translation ID reset
        super.processNodeDuringPostProcessing(node, context);

        // Copy-specific: reset docbase links on mirror nodes
        resetDocBaseLinkIfNeeded(node);

        // Copy-specific: take offline live published documents
        takeOfflineIfNeeded(node);
    }

    private void resetDocBaseLinkIfNeeded(Node node) throws RepositoryException {
        if (!node.isNodeType("hippo:mirror") || !node.hasProperty("hippo:docbase")) {
            return;
        }

        String destLinkDocBase = JcrUtils.getStringProperty(node, "hippo:docbase", null);
        if (StringUtils.isBlank(destLinkDocBase)) {
            return;
        }

        try {
            String sourceFolderBase = getSourceFolderNode().getPath() + "/";
            Node sourceLinkedNode = getSession().getNodeByIdentifier(destLinkDocBase);

            if (StringUtils.startsWith(sourceLinkedNode.getPath(), sourceFolderBase)) {
                String sourceLinkedNodeRelPath = StringUtils.removeStart(sourceLinkedNode.getPath(), sourceFolderBase);
                Node destLinkedNode = JcrUtils.getNodeIfExists(getDestFolderNode(), sourceLinkedNodeRelPath);

                if (destLinkedNode != null) {
                    getLogger().info("Updating the linked node at '{}'.", node.getPath());
                    node.setProperty("hippo:docbase", destLinkedNode.getIdentifier());
                }
            }
        } catch (ItemNotFoundException ignore) {
            // Source linked node no longer exists, skip
        }
    }

    private void takeOfflineIfNeeded(Node node) throws RepositoryException {
        if (!node.isNodeType("hippostdpubwf:document")) {
            return;
        }

        if (!isLivePublishedVariant(node)) {
            return;
        }

        node.setProperty("hippo:availability", ArrayUtils.EMPTY_STRING_ARRAY);
        node.setProperty("hippostd:stateSummary", "new");
    }

    private boolean isLivePublishedVariant(Node node) throws RepositoryException {
        if (!StringUtils.equals("published", JcrUtils.getStringProperty(node, "hippostd:state", null))) {
            return false;
        }

        if (!node.hasProperty("hippo:availability")) {
            return false;
        }

        for (Value value : node.getProperty("hippo:availability").getValues()) {
            if (StringUtils.equals("live", value.getString())) {
                return true;
            }
        }
        return false;
    }

}

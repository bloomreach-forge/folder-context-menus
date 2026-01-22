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
import java.util.concurrent.atomic.AtomicLong;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;

public class FolderMoveTask extends AbstractFolderCopyOrMoveTask {

    public FolderMoveTask(final Session session, final Locale locale, final Node sourceFolderNode,
            final Node destParentFolderNode, final String destFolderNodeName, final String destFolderDisplayName) {
        super(session, locale, sourceFolderNode, destParentFolderNode, destFolderNodeName, destFolderDisplayName);
    }

    @Override
    protected String getOperationName() {
        return "Moving";
    }

    @Override
    protected void doExecute() throws RepositoryException {
        if (getSourceFolderNode().getParent().isSame(getDestParentFolderNode())) {
            if (StringUtils.equals(getSourceFolderNode().getName(), getDestFolderNodeName())) {
                throw new RuntimeException("Cannot move to the same folder: " + getDestFolderPath());
            }
        }

        if (getSourceFolderNode().isSame(getDestParentFolderNode())) {
            throw new RuntimeException("Cannot move to the folder itself: " + getDestFolderPath());
        }

        if (getSession().nodeExists(getDestFolderPath())) {
            throw new RuntimeException("Destination folder already exists: " + getDestFolderPath());
        }

        long totalNodes = 0;
        if (getOperationProgress() != null) {
            totalNodes = countSourceNodes();
        }

        getSession().move(getSourceFolderNode().getPath(), getDestFolderPath());

        setDestFolderNode(JcrUtils.getNodeIfExists(getDestParentFolderNode(), getDestFolderNodeName()));

        try {
            if (getOperationProgress() != null) {
                recomputeHippoPathsWithProgress(totalNodes);
            } else {
                recomputeHippoPaths();
            }
        } finally {
            // Update folder translations even if path recomputation was cancelled
            updateFolderTranslations(getDestFolderNode(), getDestFolderDisplayName(), getLocale().getLanguage());
        }
    }

    private void recomputeHippoPathsWithProgress(long totalNodes) throws RepositoryException {
        AtomicLong counter = new AtomicLong(0);
        JcrTraverseUtils.traverseNodes(getDestFolderNode(),
                new NodeTraverser() {
                    @Override
                    public boolean isAcceptable(Node node) {
                        return true;
                    }

                    @Override
                    public boolean isTraversable(Node node) {
                        return true;
                    }

                    @Override
                    public void accept(Node node) throws RepositoryException {
                        recomputeDerivedDataIfNeeded(node);
                    }
                },
                getOperationProgress(), counter, totalNodes);
    }

    private void recomputeDerivedDataIfNeeded(Node node) throws RepositoryException {
        if (!(node instanceof HippoNode)) {
            return;
        }
        boolean isDerived = node.isNodeType(HippoNodeType.NT_DERIVED);
        boolean hasHippoPaths = node.isNodeType(HippoNodeType.NT_DOCUMENT)
                && node.hasProperty(HippoNodeType.HIPPO_PATHS);

        if (isDerived || hasHippoPaths) {
            ((HippoNode) node).recomputeDerivedData();
        }
    }

    @Override
    protected void doAfterExecute() throws RepositoryException {
        resetHippoDocumentTranslationIds(getResetTranslations());
        logOperationCompleted();
    }

}

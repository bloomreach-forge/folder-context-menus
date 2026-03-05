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

        // Phase 1: Count with progress (read-only traversal of SOURCE before move)
        // This shows progress to user without triggering JCR events
        if (getOperationProgress() != null) {
            countSourceNodesWithProgress();
        }

        // Phase 2: Atomic move (instant, no progress tracking needed)
        getSession().move(getSourceFolderNode().getPath(), getDestFolderPath());
        setDestFolderNode(JcrUtils.getNodeIfExists(getDestParentFolderNode(), getDestFolderNodeName()));

        updateFolderTranslations(getDestFolderNode(), getDestFolderDisplayName(), getLocale().getLanguage());
    }

    /**
     * Count source nodes with progress tracking. This is a read-only traversal
     * that shows progress to the user before the actual move happens.
     * No JCR changes occur during this phase, avoiding the race condition.
     */
    private void countSourceNodesWithProgress() throws RepositoryException {
        long total = countSourceNodes();
        AtomicLong counter = new AtomicLong(0);

        JcrTraverseUtils.traverseNodes(getSourceFolderNode(),
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
                    public void accept(Node node) {
                        // No-op: traversal is only for progress display
                    }
                },
                getOperationProgress(), counter, total);
    }

    @Override
    protected void doAfterExecute() throws RepositoryException {
        // Combined post-processing: recompute paths + reset translation IDs in single traversal
        performDestinationPostProcessing(getResetTranslations(), true);
        logOperationCompleted();
    }

}

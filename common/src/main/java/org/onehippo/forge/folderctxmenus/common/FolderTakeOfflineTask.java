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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.WorkflowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Task that takes all published documents in a folder tree offline by invoking
 * the {@code DocumentWorkflow.depublish()} workflow on each live handle.
 * Each workflow call commits its own changes; no explicit session save is required.
 */
public class FolderTakeOfflineTask extends AbstractFolderTask {

    private static final Logger log = LoggerFactory.getLogger(FolderTakeOfflineTask.class);

    private static final String HIPPO_AVAILABILITY = "hippo:availability";
    private static final String AVAILABILITY_LIVE = "live";
    private static final String WORKFLOW_CATEGORY = "default";

    private OperationProgress operationProgress;

    public FolderTakeOfflineTask(final Session session, final Node sourceFolderNode) {
        super(session, null, sourceFolderNode);
    }

    public void setOperationProgress(final OperationProgress operationProgress) {
        this.operationProgress = operationProgress;
    }

    public OperationProgress getOperationProgress() {
        return operationProgress;
    }

    @Override
    protected void doExecute() throws RepositoryException {
        if (operationProgress != null) {
            operationProgress.updateProgress(0, 1, "Scanning for published documents...");
        }

        final long total = countLiveHandles(getSourceFolderNode());

        if (total == 0) {
            return;
        }

        takeOfflineWithProgress(getSourceFolderNode(), Math.max(total, 1), new AtomicLong(0));
    }

    /**
     * Counts all {@code hippo:handle} nodes in the subtree that currently have a live variant.
     */
    private long countLiveHandles(final Node node) throws RepositoryException {
        long count = 0;
        final NodeIterator children = node.getNodes();
        while (children.hasNext()) {
            final Node child = children.nextNode();
            if (child.isNodeType(HippoNodeType.NT_HANDLE)) {
                if (hasLiveVariant(child)) {
                    count++;
                }
            } else if (isFolderNode(child)) {
                count += countLiveHandles(child);
            }
        }
        return count;
    }

    /**
     * Recursively takes all live handles offline, firing a progress update per document.
     */
    private void takeOfflineWithProgress(final Node node, final long total, final AtomicLong counter)
            throws RepositoryException {
        final List<Node> children = new ArrayList<>();
        final NodeIterator it = node.getNodes();
        while (it.hasNext()) {
            children.add(it.nextNode());
        }

        for (final Node child : children) {
            if (operationProgress != null && operationProgress.isCancelled()) {
                throw new OperationCancelledException("Operation cancelled by user");
            }
            if (child.isNodeType(HippoNodeType.NT_HANDLE)) {
                if (hasLiveVariant(child)) {
                    if (operationProgress != null) {
                        operationProgress.updateProgress(counter.incrementAndGet(), total, child.getPath());
                        operationProgress.onProgressUpdated();
                    }
                    depublishHandle(child);
                }
            } else if (isFolderNode(child)) {
                takeOfflineWithProgress(child, total, counter);
            }
        }
    }

    private void depublishHandle(final Node handle) throws RepositoryException {
        final Optional<DocumentWorkflow> workflow =
                WorkflowUtils.getWorkflow(handle, WORKFLOW_CATEGORY, DocumentWorkflow.class);
        if (workflow.isPresent()) {
            try {
                workflow.get().depublish();
            } catch (RepositoryException e) {
                throw e;
            } catch (Exception e) {
                throw new RepositoryException("Failed to take document offline: " + handle.getPath(), e);
            }
        } else {
            log.warn("No '{}' workflow found for handle: {}", WORKFLOW_CATEGORY, handle.getPath());
        }
    }

    private boolean isFolderNode(final Node node) throws RepositoryException {
        return node.isNodeType(HippoNodeType.NT_DOCUMENT)
                || node.isNodeType("hippostd:folder")
                || node.isNodeType("hippotranslation:translated");
    }

    private boolean hasLiveVariant(final Node handle) throws RepositoryException {
        final NodeIterator variants = handle.getNodes(handle.getName());
        while (variants.hasNext()) {
            final Node variant = variants.nextNode();
            if (variant.hasProperty(HIPPO_AVAILABILITY)) {
                for (Value v : variant.getProperty(HIPPO_AVAILABILITY).getValues()) {
                    if (AVAILABILITY_LIVE.equals(v.getString())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}

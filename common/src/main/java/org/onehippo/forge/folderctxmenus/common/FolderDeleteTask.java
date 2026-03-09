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
import java.util.concurrent.atomic.AtomicLong;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.hippoecm.repository.api.HippoNodeType;

/**
 * Task that deletes a folder and all its contents, blocking if any document in the tree is published.
 */
public class FolderDeleteTask extends AbstractFolderTask {

    private static final String HIPPO_AVAILABILITY = "hippo:availability";
    private static final String AVAILABILITY_LIVE = "live";

    private OperationProgress operationProgress;

    public FolderDeleteTask(final Session session, final Node sourceFolderNode) {
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
        // Phase 1: single pass to count handles and collect any published paths
        if (operationProgress != null) {
            operationProgress.updateProgress(0, 1, "Checking for published content...");
        }
        final List<String> publishedPaths = new ArrayList<>();
        final long total = countAndCheckPublished(getSourceFolderNode(), publishedPaths);

        if (!publishedPaths.isEmpty()) {
            throw new PublishedContentException(publishedPaths);
        }

        // Phase 2: delete each handle individually so progress tracks actual item count,
        // then remove the now-empty folder structure.
        deleteWithProgress(getSourceFolderNode(), Math.max(total, 1), new AtomicLong(0));
    }

    /**
     * Single-pass traversal that counts all {@code hippo:handle} nodes in the subtree
     * and simultaneously collects paths of any that still have a live variant.
     * Combining both phases avoids traversing the tree twice.
     */
    private long countAndCheckPublished(final Node node, final List<String> publishedPaths)
            throws RepositoryException {
        long count = 0;
        final NodeIterator children = node.getNodes();
        while (children.hasNext()) {
            final Node child = children.nextNode();
            if (child.isNodeType(HippoNodeType.NT_HANDLE)) {
                count++;
                if (hasLiveVariant(child)) {
                    publishedPaths.add(child.getPath());
                }
                // Do NOT recurse into the handle's children – they are document variants
                // and workflow nodes, not nested folders or additional handles.
            } else if (isFolderNode(child)) {
                count += countAndCheckPublished(child, publishedPaths);
            }
            // Skip hippo:request, hipposys:*, hippo:translation, etc.
        }
        return count;
    }

    /**
     * Recursively removes each {@code hippo:handle} individually, firing a progress update
     * per handle so the bar advances proportionally to the number of documents deleted.
     * After all handles in a subtree are removed, removes the (now-empty) folder node itself.
     */
    private void deleteWithProgress(final Node node, final long total, final AtomicLong counter)
            throws RepositoryException {
        // Collect children first to avoid iterating over a node while removing its siblings.
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
                if (operationProgress != null) {
                    operationProgress.updateProgress(counter.incrementAndGet(), total, child.getPath());
                    operationProgress.onProgressUpdated();
                }
                child.remove();
            } else if (isFolderNode(child)) {
                deleteWithProgress(child, total, counter);
            }
            // Non-handle, non-folder children are cleaned up when the folder is removed below.
        }

        node.remove();
    }

    private boolean isFolderNode(final Node node) throws RepositoryException {
        return node.isNodeType(HippoNodeType.NT_DOCUMENT)
                || node.isNodeType("hippostd:folder")
                || node.isNodeType("hippotranslation:translated");
    }

    /**
     * Returns true if any variant of this handle is currently live on the site.
     * Checks {@code hippo:availability} for the value {@code "live"} — this is the
     * same property the delivery tier uses and correctly reflects offline documents
     * whose {@code hippostd:state} is still {@code "published"} but whose availability
     * has been cleared by the "Take offline" workflow.
     */
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

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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

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
        if (operationProgress != null) {
            operationProgress.updateProgress(0, 1, "Checking for published content...");
        }

        checkNoPublishedContent(getSourceFolderNode());

        if (operationProgress != null) {
            operationProgress.updateProgress(1, 1, "Deleting folder...");
        }

        getSourceFolderNode().remove();
    }

    /**
     * Collects all document handles in the subtree that still have a published variant,
     * then throws {@link PublishedContentException} listing them so the user knows
     * exactly which documents to take offline.
     */
    private void checkNoPublishedContent(final Node node) throws RepositoryException {
        final List<String> publishedPaths = new ArrayList<>();
        collectPublishedHandles(node, publishedPaths);
        if (!publishedPaths.isEmpty()) {
            throw new PublishedContentException(publishedPaths);
        }
    }

    /**
     * Recursively walks folder nodes only. When a {@code hippo:handle} is found its
     * document variants are inspected; the traversal does NOT descend into the handle's
     * own children, avoiding false positives from workflow or translation child nodes.
     */
    private void collectPublishedHandles(final Node node, final List<String> publishedPaths)
            throws RepositoryException {
        final NodeIterator children = node.getNodes();
        while (children.hasNext()) {
            final Node child = children.nextNode();
            if (child.isNodeType(HippoNodeType.NT_HANDLE)) {
                if (hasLiveVariant(child)) {
                    publishedPaths.add(child.getPath());
                }
                // Do NOT recurse into the handle's children – they are document variants
                // and workflow nodes, not nested folders or additional handles.
            } else if (child.isNodeType(HippoNodeType.NT_DOCUMENT)
                    || child.isNodeType("hippostd:folder")
                    || child.isNodeType("hippotranslation:translated")) {
                // Only recurse into recognised folder/document container node types.
                collectPublishedHandles(child, publishedPaths);
            }
            // Skip hippo:request, hipposys:*, hippo:translation, etc.
        }
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
                for (javax.jcr.Value v : variant.getProperty(HIPPO_AVAILABILITY).getValues()) {
                    if (AVAILABILITY_LIVE.equals(v.getString())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}

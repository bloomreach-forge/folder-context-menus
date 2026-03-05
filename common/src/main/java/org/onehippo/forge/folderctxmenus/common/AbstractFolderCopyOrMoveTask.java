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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.google.common.base.Strings;
import org.hippoecm.repository.HippoStdPubWfNodeType;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.translation.HippoTranslationNodeType;
import org.hippoecm.repository.util.CopyHandler;
import org.hippoecm.repository.util.JcrUtils;

public abstract class AbstractFolderCopyOrMoveTask extends AbstractFolderTask {

    private static final int POST_PROCESSING_SAVE_BATCH_SIZE =
            Integer.getInteger("folderctxmenus.saveBatchSize", 200);

    private final Node destParentFolderNode;
    private final String destFolderNodeName;
    private final String destFolderDisplayName;
    private Node destFolderNode;
    private boolean resetTranslations;

    private CopyHandler copyHandler;
    private OperationProgress operationProgress;

    public AbstractFolderCopyOrMoveTask(final Session session, final Locale locale, final Node sourceFolderNode,
                                        final Node destParentFolderNode, final String destFolderNodeName, final String destFolderDisplayName) {
        //when moving, we want to keep any existing translation links
        this(session, locale, sourceFolderNode, destParentFolderNode, destFolderNodeName, destFolderDisplayName, false);
    }

    public AbstractFolderCopyOrMoveTask(final Session session, final Locale locale, final Node sourceFolderNode,
            final Node destParentFolderNode, final String destFolderNodeName, final String destFolderDisplayName,
            final boolean resetTranslations) {
        super(session, locale, sourceFolderNode);

        this.destParentFolderNode = destParentFolderNode;
        this.destFolderNodeName = destFolderNodeName;
        this.destFolderDisplayName = destFolderDisplayName;
        this.resetTranslations = resetTranslations;
    }

    public Node getDestParentFolderNode() {
        return destParentFolderNode;
    }

    public String getDestFolderNodeName() {
        return destFolderNodeName;
    }

    public String getDestFolderDisplayName() {
        return destFolderDisplayName;
    }

    public Node getDestFolderNode() {
        return destFolderNode;
    }

    public void setDestFolderNode(final Node destFolderNode) {
        this.destFolderNode = destFolderNode;
    }

    public String getDestFolderPath() throws RepositoryException {
        return getDestParentFolderNode().getPath() + "/" + getDestFolderNodeName();
    }

    public boolean getResetTranslations() {
        return resetTranslations;
    }

    public CopyHandler getCopyHandler() {
        return copyHandler;
    }

    public void setCopyHandler(CopyHandler copyHandler) {
        this.copyHandler = copyHandler;
    }

    public OperationProgress getOperationProgress() {
        return operationProgress;
    }

    public void setOperationProgress(OperationProgress operationProgress) {
        this.operationProgress = operationProgress;
    }

    protected abstract String getOperationName();

    @Override
    protected void doBeforeExecute() throws RepositoryException {
        getLogger().info("{} folder '{}' to '{}'",
                getOperationName(),
                getSourceFolderNode().getPath(),
                getDestFolderPath());
    }

    protected void logOperationCompleted() throws RepositoryException {
        if (getDestFolderNode() == null) {
            getLogger().info("{} folder operation completed (no destination created): '{}'",
                    getOperationName(),
                    getSourceFolderNode().getPath());
            return;
        }
        getLogger().info("{} folder operation completed: '{}' -> '{}'",
                getOperationName(),
                getSourceFolderNode().getPath(),
                getDestFolderNode().getPath());
    }

    protected long countSourceNodes() throws RepositoryException {
        return NodeCounter.countNodes(getSourceFolderNode(), NodeTraverser.USER_VISIBLE_ITEMS);
    }

    /**
     * Performs all destination post-processing in a single traversal.
     * Subclasses can override {@link #processNodeDuringPostProcessing} to add additional operations.
     *
     * @param resetTranslationIds whether to reset translation IDs
     * @param recomputePaths whether to recompute hippo:paths
     */
    protected void performDestinationPostProcessing(boolean resetTranslationIds, boolean recomputePaths) {
        if (getDestFolderNode() == null) {
            return;
        }

        if (getOperationProgress() != null) {
            getOperationProgress().enterFinalizingPhase();
        }

        try {
            final Map<String, String> uuidMappings = new HashMap<>();
            final String parentLocale = JcrUtils.getStringProperty(
                    getDestFolderNode().getParent(), HippoTranslationNodeType.LOCALE, null);
            final PostProcessingContext context = new PostProcessingContext(
                    uuidMappings, parentLocale, resetTranslationIds, recomputePaths);

            // Process the root node first for translations
            processTranslationNode(getDestFolderNode(), context);

            int[] postProcessingNodeCount = {0};
            int[] userVisibleCount = {0};
            JcrTraverseUtils.traverseNodes(getDestFolderNode(), new NodeTraverser() {
                @Override
                public boolean isAcceptable(Node node) {
                    return true;
                }

                @Override
                public boolean isTraversable(Node node) throws RepositoryException {
                    // Stop at document nodes and mirror nodes (mirrors don't have meaningful children)
                    return !node.isNodeType(HippoStdPubWfNodeType.HIPPOSTDPUBWF_DOCUMENT)
                            && !node.isNodeType("hippo:mirror");
                }

                @Override
                public void accept(Node node) throws RepositoryException {
                    processNodeDuringPostProcessing(node, context);
                    postProcessingNodeCount[0]++;
                    if (getOperationProgress() != null && NodeTraverser.USER_VISIBLE_ITEMS.isAcceptable(node)) {
                        getOperationProgress().updateFinalizingProgress(++userVisibleCount[0]);
                    }
                    maybeSaveAndRefreshPostProcessing(node, postProcessingNodeCount[0]);
                }
            });
        } catch (RepositoryException e) {
            getLogger().error("Failed to perform post-processing under {}.", getDestFolderNodePath(), e);
        }
    }

    void maybeSaveAndRefreshPostProcessing(Node node, int nodeCount) throws RepositoryException {
        if (POST_PROCESSING_SAVE_BATCH_SIZE <= 0) {
            return;
        }
        if (nodeCount % POST_PROCESSING_SAVE_BATCH_SIZE != 0) {
            return;
        }
        Session session = node.getSession();
        session.save();
        session.refresh(false);
    }

    /**
     * Processes a single node during post-processing traversal.
     * Subclasses can override to add additional operations.
     */
    protected void processNodeDuringPostProcessing(Node node, PostProcessingContext context) throws RepositoryException {
        // Recompute hippo:paths for derived nodes
        if (context.recomputePaths && node instanceof HippoNode) {
            if (node.isNodeType(HippoNodeType.NT_DERIVED) ||
                    (node.isNodeType(HippoNodeType.NT_DOCUMENT) && node.hasProperty(HippoNodeType.HIPPO_PATHS))) {
                ((HippoNode) node).recomputeDerivedData();
            }
        }

        // Reset translation IDs
        processTranslationNode(node, context);
    }

    private void processTranslationNode(Node node, PostProcessingContext context) throws RepositoryException {
        if (!node.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)) {
            return;
        }

        boolean shouldResetIds = context.resetTranslationIds || context.parentLocale == null;
        if (shouldResetIds) {
            String translationUuid = JcrUtils.getStringProperty(node, HippoTranslationNodeType.ID, null);
            if (UUIDUtils.isValidPattern(translationUuid)) {
                String newUuid = context.uuidMappings.computeIfAbsent(translationUuid, k -> UUID.randomUUID().toString());
                node.setProperty(HippoTranslationNodeType.ID, newUuid);
            }
        }

        if (!Strings.isNullOrEmpty(context.parentLocale)) {
            node.setProperty(HippoTranslationNodeType.LOCALE, context.parentLocale);
        }
    }

    private String getDestFolderNodePath() {
        try {
            return getDestFolderNode() != null ? getDestFolderNode().getPath() : null;
        } catch (RepositoryException e) {
            return null;
        }
    }

    /**
     * Context object holding shared state for post-processing operations.
     */
    protected static class PostProcessingContext {
        final Map<String, String> uuidMappings;
        final String parentLocale;
        final boolean resetTranslationIds;
        final boolean recomputePaths;

        PostProcessingContext(Map<String, String> uuidMappings, String parentLocale,
                              boolean resetTranslationIds, boolean recomputePaths) {
            this.uuidMappings = uuidMappings;
            this.parentLocale = parentLocale;
            this.resetTranslationIds = resetTranslationIds;
            this.recomputePaths = recomputePaths;
        }
    }

    /**
     * @deprecated Use {@link #performDestinationPostProcessing(boolean, boolean)} instead for better performance.
     */
    @Deprecated
    protected void recomputeHippoPaths() {
        performDestinationPostProcessing(false, true);
    }

    /**
     * @deprecated Use {@link #performDestinationPostProcessing(boolean, boolean)} instead for better performance.
     */
    @Deprecated
    protected void resetHippoDocumentTranslationIds(boolean resetIds) {
        performDestinationPostProcessing(resetIds, false);
    }

}

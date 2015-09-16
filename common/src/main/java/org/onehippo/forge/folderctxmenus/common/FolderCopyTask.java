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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.RepoUtils;

public class FolderCopyTask extends AbstractFolderCopyOrMoveTask {

    public FolderCopyTask(final Session session, final Locale locale, final Node sourceFolderNode,
            final Node destParentFolderNode, final String destFolderNodeName, final String destFolderDisplayName) {
        super(session, locale, sourceFolderNode, destParentFolderNode, destFolderNodeName, destFolderDisplayName);
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

        getSession().save();
    }

    @Override
    protected void doAfterExecute() throws RepositoryException {
        resetHippoDocBaseLinks();
        takeOfflineHippoDocs();
        resetHippoDocumentTranslationIds();
        getSession().save();
    }

    /**
     * Search all the hippotranslation:translated nodes under {@code destFolderNode} including {@code destFolderNode}
     * and reset the hippotranslation:id property to a newly generate UUID.
     */
    protected void resetHippoDocumentTranslationIds() {
        String destFolderNodePath = null;

        try {
            destFolderNodePath = getDestFolderNode().getPath();

            Map<String, String> uuidMappings = new HashMap<String, String>();

            resetHippoTranslatedNodeWithNewUuid(getDestFolderNode(), uuidMappings);

            String statement = "/jcr:root" + destFolderNodePath + "//element(*,hippotranslation:translated)";
            Query query = getSession().getWorkspace().getQueryManager().createQuery(RepoUtils.encodeXpath(statement),
                    Query.XPATH);
            QueryResult result = query.execute();

            Node translatedNode;

            for (NodeIterator nodeIt = result.getNodes(); nodeIt.hasNext();) {
                translatedNode = nodeIt.nextNode();
                resetHippoTranslatedNodeWithNewUuid(translatedNode, uuidMappings);
            }
        } catch (RepositoryException e) {
            getLogger().error("Failed to reset hippotranslation:id properties under {}.", destFolderNodePath, e);
        }
    }

    private void resetHippoTranslatedNodeWithNewUuid(final Node translatedNode, final Map<String, String> uuidMappings)
            throws RepositoryException {
        if (translatedNode != null && translatedNode.isNodeType("hippotranslation:translated")) {
            String translationUuid = JcrUtils.getStringProperty(translatedNode, "hippotranslation:id", null);

            if (UUIDUtils.isValidPattern(translationUuid)) {
                String newTranslationUuid;

                if (uuidMappings.containsKey(translationUuid)) {
                    newTranslationUuid = uuidMappings.get(translationUuid);
                } else {
                    newTranslationUuid = UUID.randomUUID().toString();
                    uuidMappings.put(translationUuid, newTranslationUuid);
                }

                translatedNode.setProperty("hippotranslation:id", newTranslationUuid);
            }
        }
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
            String statement = "/jcr:root" + destFolderNodePath + "//element(*)[@hippo:docbase]";
            Query query = getSession().getWorkspace().getQueryManager().createQuery(RepoUtils.encodeXpath(statement),
                    Query.XPATH);
            QueryResult result = query.execute();

            String sourceFolderBase = getSourceFolderNode().getPath() + "/";
            Node destLinkHolderNode;
            String destLinkDocBase;
            Node sourceLinkedNode;
            String sourceLinkedNodeRelPath;
            Node destLinkedNode;

            for (NodeIterator nodeIt = result.getNodes(); nodeIt.hasNext();) {
                destLinkHolderNode = nodeIt.nextNode();

                if (destLinkHolderNode.hasProperty("hippo:docbase")) {
                    destLinkDocBase = JcrUtils.getStringProperty(destLinkHolderNode, "hippo:docbase", null);

                    if (StringUtils.isNotBlank(destLinkDocBase)) {
                        try {
                            sourceLinkedNode = getSession().getNodeByIdentifier(destLinkDocBase);

                            if (StringUtils.startsWith(sourceLinkedNode.getPath(), sourceFolderBase)) {
                                sourceLinkedNodeRelPath = StringUtils.removeStart(sourceLinkedNode.getPath(),
                                        sourceFolderBase);
                                destLinkedNode = JcrUtils.getNodeIfExists(getDestFolderNode(), sourceLinkedNodeRelPath);

                                if (destLinkedNode != null) {
                                    getLogger().info("Updating the linked node at '{}'.", destLinkHolderNode.getPath());
                                    destLinkHolderNode.setProperty("hippo:docbase", destLinkedNode.getIdentifier());
                                }
                            }
                        } catch (ItemNotFoundException ignore) {
                        }
                    }
                }
            }
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
            String statement = "/jcr:root" + destFolderNodePath
                    + "//element(*,hippostdpubwf:document)[@hippo:availability='live' and @hippostd:state='published']";
            Query query = getSession().getWorkspace().getQueryManager().createQuery(RepoUtils.encodeXpath(statement),
                    Query.XPATH);
            QueryResult result = query.execute();

            Node liveVariant;

            for (NodeIterator nodeIt = result.getNodes(); nodeIt.hasNext();) {
                liveVariant = nodeIt.nextNode();
                liveVariant.setProperty("hippo:availability", ArrayUtils.EMPTY_STRING_ARRAY);
                liveVariant.setProperty("hippostd:stateSummary", "new");
            }
        } catch (RepositoryException e) {
            getLogger().error("Failed to take offline link hippostd:publishableSummary nodes under {}.", destFolderNodePath, e);
        }
    }

}

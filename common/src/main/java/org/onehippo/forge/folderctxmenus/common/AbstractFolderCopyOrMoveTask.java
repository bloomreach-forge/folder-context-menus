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
import org.hippoecm.repository.util.CopyHandler;
import org.hippoecm.repository.util.JcrUtils;

public abstract class AbstractFolderCopyOrMoveTask extends AbstractFolderTask {

    private final Node destParentFolderNode;
    private final String destFolderNodeName;
    private final String destFolderDisplayName;
    private Node destFolderNode;
    private Boolean linkAsTranslation;

    private CopyHandler copyHandler;

    public AbstractFolderCopyOrMoveTask(final Session session, final Locale locale, final Node sourceFolderNode,
            final Node destParentFolderNode, final String destFolderNodeName, final String destFolderDisplayName,
            final Boolean linkAsTranslation) {
        super(session, locale, sourceFolderNode);

        this.destParentFolderNode = destParentFolderNode;
        this.destFolderNodeName = destFolderNodeName;
        this.destFolderDisplayName = destFolderDisplayName;
        this.linkAsTranslation = linkAsTranslation;
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

    public Boolean getLinkAsTranslation() {
        return linkAsTranslation;
    }

    public CopyHandler getCopyHandler() {
        return copyHandler;
    }

    public void setCopyHandler(CopyHandler copyHandler) {
        this.copyHandler = copyHandler;
    }

    protected void recomputeHippoPaths(final Node folderNode) {
        try {
            JcrTraverseUtils.traverseNodes(getDestFolderNode(),
                    new NodeTraverser() {
                        @Override
                        public boolean isAcceptable(Node node) throws RepositoryException {
                            if (!(node instanceof HippoNode)) {
                                return false;
                            }

                            if (node.isNodeType(HippoNodeType.NT_DERIVED)) {
                                return true;
                            }

                            if (node.isNodeType(HippoNodeType.NT_DOCUMENT) && node.hasProperty(HippoNodeType.HIPPO_PATHS)) {
                                return true;
                            }

                            return false;
                        }

                        @Override
                        public boolean isTraversable(Node node) throws RepositoryException {
                            return !node.isNodeType(HippoStdPubWfNodeType.HIPPOSTDPUBWF_DOCUMENT);
                        }

                        @Override
                        public void accept(Node node) throws RepositoryException {
                            ((HippoNode) node).recomputeDerivedData();
                        }
                    });
        } catch (RepositoryException e) {
            getLogger().error("Failed to touch hippo:paths properties.", e);
        }
    }

    /**
     * Search all the hippotranslation:translated nodes under {@code destFolderNode} including {@code destFolderNode}
     * and reset the hippotranslation:id property to a newly generate UUID.
     */
    protected void resetHippoDocumentTranslationIds(boolean resetIds) {
        String destFolderNodePath = null;

        try {
            destFolderNodePath = getDestFolderNode().getPath();

            final Map<String, String> uuidMappings = new HashMap<String, String>();

            String parentLocale = getDestFolderNode().getParent().getProperty("hippotranslation:locale").getString();

            resetHippoTranslatedNodeWithNewUuid(getDestFolderNode(), uuidMappings, resetIds, parentLocale);

            JcrTraverseUtils.traverseNodes(getDestFolderNode(),
                new NodeTraverser() {
                    @Override
                    public boolean isAcceptable(Node node) throws RepositoryException {
                        return node.isNodeType("hippotranslation:translated");
                    }

                    @Override
                    public boolean isTraversable(Node node) throws RepositoryException {
                        return !node.isNodeType("hippostdpubwf:document");
                    }

                    @Override
                    public void accept(Node translatedNode) throws RepositoryException {
                        resetHippoTranslatedNodeWithNewUuid(translatedNode, uuidMappings, resetIds, parentLocale);
                    }
                });
        } catch (RepositoryException e) {
            getLogger().error("Failed to reset hippotranslation:id properties under {}.", destFolderNodePath, e);
        }
    }

    private void resetHippoTranslatedNodeWithNewUuid(final Node translatedNode, final Map<String, String> uuidMappings,
                                                     final boolean resetIds, final String parentLocale)
        throws RepositoryException {
        if (translatedNode != null && translatedNode.isNodeType("hippotranslation:translated")) {
            if (resetIds) {
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

            if(!Strings.isNullOrEmpty(parentLocale)) {
                translatedNode.setProperty("hippotranslation:locale", parentLocale);
            }
        }
    }

}

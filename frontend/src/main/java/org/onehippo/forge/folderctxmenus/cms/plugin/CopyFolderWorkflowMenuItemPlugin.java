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
package org.onehippo.forge.folderctxmenus.cms.plugin;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.RepoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CopyFolderWorkflowMenuItemPlugin extends AbstractFolderActionWorkflowMenuItemPlugin {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(CopyFolderWorkflowMenuItemPlugin.class);

    public CopyFolderWorkflowMenuItemPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    protected IModel<String> getMenuItemLabelModel() {
        return new StringResourceModel("folder.action.copy.menuitem.label", this, null, "Copy folder...");
    }

    @Override
    protected ResourceReference getMenuItemIconResourceReference() {
        return new PackageResourceReference(getClass(), "copy-folder-16.png");
    }

    @Override
    protected IModel<String> getDialogTitleModel() {
        return new StringResourceModel("folder.action.copy.dialog.label", this, null, "Copy folder...");
    }

    @Override
    protected AbstractDialog<FolderActionDocumentArguments> createDialogInstance(final FolderActionDocumentArguments folderActionDocumentModel) {
        return new CopyOrMoveFolderDialog(getPluginContext(), getPluginConfig(), getDialogTitleModel(), new Model<FolderActionDocumentArguments>(folderActionDocumentModel)) {
            @Override
            protected void onOk() {
                try {
                    copyFolder(getSourceFolderIdentifier(), getSourceFolderPathDisplay(),
                               getDestinationFolderIdentifier(), getDestinationFolderPathDisplay(),
                               getNewFolderName(), getNewFolderUrlName());
                    super.onOk();
                } catch (Exception e) {
                    error(e.getLocalizedMessage());
                }
            }
        };
    }

    protected void copyFolder(final String sourceFolderIdentifier, final String sourceFolderPathDisplay,
                              final String destinationFolderIdentifier, final String destinationFolderPathDisplay,
                              final String newFolderName, final String newFolderUrlName) throws RepositoryException {
        log.debug("Copying folder: from '{}' to '{}/{}'.", sourceFolderPathDisplay, destinationFolderPathDisplay, newFolderUrlName);

        Session jcrSession = UserSession.get().getJcrSession();
        Node sourceFolderNode = jcrSession.getNodeByIdentifier(sourceFolderIdentifier);
        Node destParentFolderNode = jcrSession.getNodeByIdentifier(destinationFolderIdentifier);

        if (sourceFolderNode.getParent().isSame(destParentFolderNode)) {
            if (StringUtils.equals(sourceFolderNode.getName(), newFolderUrlName)) {
                throw new RuntimeException("Cannot copy to the same folder: " + destinationFolderPathDisplay + " / " + newFolderName);
            }
        }

        if (sourceFolderNode.isSame(destParentFolderNode)) {
            throw new RuntimeException("Cannot copy to the folder itself: " + destinationFolderPathDisplay);
        }

        String destFolderPath = destParentFolderNode.getPath() + "/" + newFolderUrlName;

        if (jcrSession.nodeExists(destFolderPath)) {
            throw new RuntimeException("Destination folder already exists: " + destinationFolderPathDisplay + " / " + newFolderName);
        }

        JcrUtils.copy(sourceFolderNode, newFolderUrlName, destParentFolderNode);

        Node destFolderNode = JcrUtils.getNodeIfExists(destParentFolderNode, newFolderUrlName);

        updateFolderTranslations(destFolderNode, newFolderName, UserSession.get().getLocale().getLanguage());

        jcrSession.save();

        afterCopyFolder(sourceFolderNode, destFolderNode);

        jcrSession.save();
    }

    protected void afterCopyFolder(final Node sourceFolderNode, final Node destFolderNode) {
        resetHippoDocBaseLinks(sourceFolderNode, destFolderNode);
    }

    /**
     * Search all the link holder nodes having hippo:docbase property under destFolderNode
     * and reset the hippo:docbase properties to the copied nodes under destFolderNode
     * by comparing the relative paths with the corresponding nodes under the sourceFolderNode.
     * @param sourceFolderNode
     * @param destFolderNode
     */
    protected void resetHippoDocBaseLinks(final Node sourceFolderNode, final Node destFolderNode) {
        try {
            Session jcrSession = UserSession.get().getJcrSession();
            String destFolderNodePath = destFolderNode.getPath();
            String statement = "/jcr:root" + destFolderNodePath + "//element(*)[@hippo:docbase]";
            Query query =
                jcrSession.getWorkspace().getQueryManager().createQuery(RepoUtils.encodeXpath(statement), Query.XPATH);
            QueryResult result = query.execute();

            String sourceFolderBase = sourceFolderNode.getPath() + "/";
            Node destLinkHolderNode;
            String destLinkDocBase;
            Node sourceLinkedNode;
            String sourceLinkedNodeRelPath;
            Node destLinkedNode;

            for (NodeIterator nodeIt = result.getNodes(); nodeIt.hasNext(); ) {
                destLinkHolderNode = nodeIt.nextNode();

                if (destLinkHolderNode.hasProperty("hippo:docbase")) {
                    destLinkDocBase = JcrUtils.getStringProperty(destLinkHolderNode, "hippo:docbase", null);

                    if (StringUtils.isNotBlank(destLinkDocBase)) {
                        try {
                            sourceLinkedNode = jcrSession.getNodeByIdentifier(destLinkDocBase);

                            if (StringUtils.startsWith(sourceLinkedNode.getPath(), sourceFolderBase)) {
                                sourceLinkedNodeRelPath = StringUtils.removeStart(sourceLinkedNode.getPath(), sourceFolderBase);
                                destLinkedNode = JcrUtils.getNodeIfExists(destFolderNode, sourceLinkedNodeRelPath);

                                if (destLinkedNode != null) {
                                    log.debug("Updating the linked node at '{}'.", destLinkHolderNode.getPath());
                                    destLinkHolderNode.setProperty("hippo:docbase", destLinkedNode.getIdentifier());
                                }
                            }
                        } catch (ItemNotFoundException ignore) {
                        }
                    }
                }
            }
        } catch (RepositoryException e) {
            log.error("Failed to reset link Nodes,", e);
        }
    }
}

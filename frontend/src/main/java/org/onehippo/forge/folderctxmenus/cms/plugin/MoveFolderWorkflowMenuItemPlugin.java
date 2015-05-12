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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MoveFolderWorkflowMenuItemPlugin extends AbstractFolderActionWorkflowMenuItemPlugin {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(MoveFolderWorkflowMenuItemPlugin.class);

    public MoveFolderWorkflowMenuItemPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    protected IModel<String> getMenuItemLabelModel() {
        return new StringResourceModel("folder.action.move.menuitem.label", this, null, "Move Folder...");
    }

    @Override
    protected ResourceReference getMenuItemIconResourceReference() {
        return new PackageResourceReference(getClass(), "move-folder-16.png");
    }

    @Override
    protected IModel<String> getDialogTitleModel() {
        return new StringResourceModel("folder.action.move.dialog.label", this, null, "Move Folder...");
    }

    @Override
    protected AbstractDialog<FolderActionDocumentArguments> createDialogInstance(final FolderActionDocumentArguments folderActionDocumentModel) {
        return new CopyOrMoveFolderDialog(getPluginContext(), getPluginConfig(), getDialogTitleModel(), new Model<FolderActionDocumentArguments>(folderActionDocumentModel)) {
            @Override
            protected void onOk() {
                try {
                    moveFolder(getSourceFolderIdentifier(), getSourceFolderPathDisplay(),
                               getDestinationFolderIdentifier(), getDestinationFolderPathDisplay(),
                               getNewFolderName(), getNewFolderUrlName());
                    super.onOk();
                } catch (Exception e) {
                    error(e.getLocalizedMessage());
                }
            }
        };
    }

    protected void moveFolder(final String sourceFolderIdentifier, final String sourceFolderPathDisplay,
                              final String destinationFolderIdentifier, final String destinationFolderPathDisplay,
                              final String newFolderName, final String newFolderUrlName) throws RepositoryException {
        log.debug("Moving folder: from '{}' to '{}/{}'.", sourceFolderPathDisplay, destinationFolderPathDisplay, newFolderUrlName);

        Session jcrSession = UserSession.get().getJcrSession();
        Node sourceFolderNode = jcrSession.getNodeByIdentifier(sourceFolderIdentifier);
        Node destParentFolderNode = jcrSession.getNodeByIdentifier(destinationFolderIdentifier);

        if (sourceFolderNode.getParent().isSame(destParentFolderNode)) {
            if (StringUtils.equals(sourceFolderNode.getName(), newFolderUrlName)) {
                throw new RuntimeException("Cannot move to the same folder: " + destinationFolderPathDisplay + " / " + newFolderName);
            }
        }

        if (sourceFolderNode.isSame(destParentFolderNode)) {
            throw new RuntimeException("Cannot move to the folder itself: " + destinationFolderPathDisplay);
        }

        String destFolderPath = destParentFolderNode.getPath() + "/" + newFolderUrlName;

        if (jcrSession.nodeExists(destFolderPath)) {
            throw new RuntimeException("Destination folder already exists: " + destinationFolderPathDisplay + " / " + newFolderName);
        }

        jcrSession.move(sourceFolderNode.getPath(), destFolderPath);

        Node destFolderNode = JcrUtils.getNodeIfExists(destParentFolderNode, newFolderUrlName);

        updateFolderTranslations(destFolderNode, UserSession.get().getLocale().getLanguage());

        afterMoveFolder(sourceFolderNode, destFolderNode);

        jcrSession.save();
    }

    protected void afterMoveFolder(final Node destFolderNode, final Node sourceFolderNode) {
    }

}

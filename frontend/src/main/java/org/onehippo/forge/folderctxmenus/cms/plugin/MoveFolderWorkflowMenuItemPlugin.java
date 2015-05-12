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

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
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
                moveFolder(getSourceFolderIdentifier(), getSourceFolderPathDisplay(),
                           getDestinationFolderIdentifier(), getDestinationFolderPathDisplay(),
                           getNewFolderName(), getNewFolderUrlName());
                super.onOk();
            }
        };
    }

    protected void moveFolder(final String sourceFolderIdentifier, final String sourceFolderPathDisplay,
                              final String destinationFolderIdentifier, final String destinationFolderPathDisplay,
                              final String newFolderName, final String newFolderUrlName) {
        log.debug("Moving folder: from '{}' to '{}/{}'.", sourceFolderPathDisplay, destinationFolderPathDisplay, newFolderUrlName);
    }
}

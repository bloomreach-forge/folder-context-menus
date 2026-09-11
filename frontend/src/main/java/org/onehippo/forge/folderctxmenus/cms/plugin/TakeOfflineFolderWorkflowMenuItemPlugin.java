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

public class TakeOfflineFolderWorkflowMenuItemPlugin extends AbstractFolderActionWorkflowMenuItemPlugin {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(TakeOfflineFolderWorkflowMenuItemPlugin.class);

    public TakeOfflineFolderWorkflowMenuItemPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    protected boolean hasRequiredPrivilege() {
        return userHasTakeOfflineFolderPrivilege();
    }

    @Override
    protected IModel<String> getMenuItemLabelModel() {
        return new StringResourceModel("folder.action.takeoffline.menuitem.label", this, null)
                .setDefaultValue("Take all documents offline...");
    }

    @Override
    protected ResourceReference getMenuItemIconResourceReference() {
        return new PackageResourceReference(getClass(), "take-offline-folder.svg");
    }

    @Override
    protected IModel<String> getDialogTitleModel() {
        return new StringResourceModel("folder.action.takeoffline.dialog.label", this, null)
                .setDefaultValue("Take All Documents Offline");
    }

    @Override
    protected AbstractDialog<FolderActionDocumentArguments> createDialogInstance(
            final FolderActionDocumentArguments folderActionDocumentModel) {
        return new TakeOfflineFolderDialog(
                getPluginContext(),
                getPluginConfig(),
                getDialogTitleModel(),
                new Model<>(folderActionDocumentModel));
    }

}

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
package org.onehippo.forge.folderctxmenus.cms.plugin;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoSession;
import org.onehippo.forge.folderctxmenus.common.ExtendedFolderWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class CopyFolderWorkflowMenuItemPlugin extends AbstractFolderActionWorkflowMenuItemPlugin {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(CopyFolderWorkflowMenuItemPlugin.class);

    public CopyFolderWorkflowMenuItemPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    protected IModel<String> getMenuItemLabelModel() {
        return new StringResourceModel("folder.action.copy.menuitem.label", this, null).setDefaultValue("Copy folder...");
    }

    @Override
    protected ResourceReference getMenuItemIconResourceReference() {
        return new PackageResourceReference(getClass(), "copy-folder-16.png");
    }

    @Override
    protected IModel<String> getDialogTitleModel() {
        return new StringResourceModel("folder.action.copy.dialog.label", this, null).setDefaultValue("Copy folder...");
    }

    @Override
    protected AbstractDialog<FolderActionDocumentArguments> createDialogInstance(final FolderActionDocumentArguments folderActionDocumentModel) {
        return new CopyOrMoveFolderDialog(getPluginContext(), getPluginConfig(), getDialogTitleModel(),
            new Model<FolderActionDocumentArguments>(folderActionDocumentModel), true) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onOk() {
                if (StringUtils.isBlank(getDestinationFolderIdentifier())) {
                    error("Please select the target folder.");
                    super.onOk();
                    return;
                }

                if (StringUtils.isBlank(getNewFolderUrlName()) || StringUtils.isBlank(getNewFolderName())) {
                    error("Please enter the destination folder name.");
                    super.onOk();
                    return;
                }

                try {
                    final HippoSession hippoSession = UserSession.get().getJcrSession();
                    final Optional<ExtendedFolderWorkflow> advancedFolderWorkflow = getExtendedFolderWorkflow(hippoSession.getNodeByIdentifier(getSourceFolderIdentifier()));
                    if(advancedFolderWorkflow.isPresent()) {
                        advancedFolderWorkflow.get().copyFolder(UserSession.get().getLocale(), getSourceFolderIdentifier(),
                                getDestinationFolderIdentifier(), getNewFolderUrlName(), getNewFolderName(), getLinkAsTranslation());
                    } else {
                        log.error("Extended folder workflow is not available");
                        error("Unable to copy folder.");
                    }
                    super.onOk();
                } catch (final Exception e) {
                    log.error("Failed to copy folder.", e);
                    error(e.getLocalizedMessage());
                }
            }
        };
    }

}

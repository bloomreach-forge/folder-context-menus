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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import java.util.Optional;

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

public class MoveFolderWorkflowMenuItemPlugin extends AbstractFolderActionWorkflowMenuItemPlugin {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(MoveFolderWorkflowMenuItemPlugin.class);

    public MoveFolderWorkflowMenuItemPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    protected boolean hasRequiredPrivilege() {
        return userHasMoveFolderPrivilege();
    }

    @Override
    protected IModel<String> getMenuItemLabelModel() {
        return new StringResourceModel("folder.action.move.menuitem.label", this, null).setDefaultValue("Move Folder...");
    }

    @Override
    protected ResourceReference getMenuItemIconResourceReference() {
        return new PackageResourceReference(getClass(), "move-folder-16.png");
    }

    @Override
    protected IModel<String> getDialogTitleModel() {
        return new StringResourceModel("folder.action.move.dialog.label", this, null).setDefaultValue("Move Folder...");
    }

    @Override
    protected AbstractDialog<FolderActionDocumentArguments> createDialogInstance(
            final FolderActionDocumentArguments folderActionDocumentModel) {
        return new CopyOrMoveFolderDialog(getPluginContext(), getPluginConfig(), getDialogTitleModel(),
                new Model<>(folderActionDocumentModel), false) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onOk() {
                if (!validateInput()) {
                    super.onOk();
                    return;
                }

                executeMove();
            }

            private boolean validateInput() {
                if (StringUtils.isBlank(getDestinationFolderIdentifier())) {
                    error("Please select the target folder.");
                    return false;
                }

                if (StringUtils.isBlank(getNewFolderUrlName()) || StringUtils.isBlank(getNewFolderName())) {
                    error("Please enter the destination folder name.");
                    return false;
                }

                return true;
            }

            private void executeMove() {
                try {
                    HippoSession session = UserSession.get().getJcrSession();
                    Node sourceNode = session.getNodeByIdentifier(getSourceFolderIdentifier());
                    Optional<ExtendedFolderWorkflow> workflow = getExtendedFolderWorkflow(sourceNode);

                    if (workflow.isPresent()) {
                        workflow.get().moveFolder(
                            UserSession.get().getLocale(),
                            getSourceFolderIdentifier(),
                            getDestinationFolderIdentifier(),
                            getNewFolderUrlName(),
                            getNewFolderName()
                        );
                        super.onOk();
                    } else {
                        log.error("Extended folder workflow is not available");
                        error("Unable to move folder.");
                    }
                } catch (Exception e) {
                    log.error("Failed to move folder.", e);
                    error(e.getLocalizedMessage());
                }
            }
        };
    }

}

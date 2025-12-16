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

import javax.jcr.RepositoryException;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.onehippo.forge.folderctxmenus.common.ExtendedFolderWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class MoveFolderWorkflowMenuItemPlugin extends AbstractFolderActionWorkflowMenuItemPlugin {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(MoveFolderWorkflowMenuItemPlugin.class);

    public MoveFolderWorkflowMenuItemPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        // Override parent behavior: only add menu item if user has move privilege
        if (userHasMoveFolderPrivilege()) {
            removeAll(); // Remove any components added by parent constructor
            add(createMoveMenuItemWorkflow());
        } else {
            removeAll(); // Ensure no menu items added
        }
    }

    /**
     * Creates the move workflow menu item component.
     */
    private StdWorkflow<FolderWorkflow> createMoveMenuItemWorkflow() {
        return new StdWorkflow<FolderWorkflow>("menuItem",
                getMenuItemLabelModel(),
                (WorkflowDescriptorModel) getModel()) {

            private FolderActionDocumentArguments folderActionDocumentModel;

            @Override
            protected ResourceReference getIcon() {
                return getMenuItemIconResourceReference();
            }

            @Override
            protected String execute(FolderWorkflow workflow) throws Exception {
                final IDialogService dialogService = getDialogService();

                if (!dialogService.isShowingDialog()) {
                    folderActionDocumentModel = createFolderActionDocumentModel();
                    final IDialogFactory dialogFactory = createDialogFactory(folderActionDocumentModel);
                    dialogService.show(dialogFactory.createDialog());
                }

                return null;
            }

            private FolderActionDocumentArguments createFolderActionDocumentModel() {
                FolderActionDocumentArguments model = new FolderActionDocumentArguments();

                try {
                    HippoNode node = getNode();
                    model.setSourceFolderIdentifier(node.getIdentifier());
                    model.setSourceFolderName(node.getDisplayName());
                    model.setSourceFolderUriName(node.getName());
                    model.setSourceFolderNodeType(node.getPrimaryNodeType().getName());
                } catch (RepositoryException e) {
                    log.error("Could not retrieve folder action workflow document", e);
                    model.setSourceFolderName("");
                    model.setSourceFolderUriName("");
                    model.setSourceFolderNodeType(null);
                }

                return model;
            }

            private HippoNode getNode() throws RepositoryException {
                return (HippoNode) ((WorkflowDescriptorModel) getDefaultModel()).getNode();
            }

            private IDialogService getDialogService() {
                return getPluginContext().getService(IDialogService.class.getName(), IDialogService.class);
            }

            private IDialogFactory createDialogFactory(final FolderActionDocumentArguments model) {
                return () -> createDialogInstance(model);
            }
        };
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
    protected AbstractDialog<FolderActionDocumentArguments> createDialogInstance(final FolderActionDocumentArguments folderActionDocumentModel) {
        return new CopyOrMoveFolderDialog(getPluginContext(), getPluginConfig(), getDialogTitleModel(),
            new Model<FolderActionDocumentArguments>(folderActionDocumentModel), false) {

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
                    if (advancedFolderWorkflow.isPresent()) {
                        advancedFolderWorkflow.get().moveFolder(UserSession.get().getLocale(), getSourceFolderIdentifier(),
                                getDestinationFolderIdentifier(), getNewFolderUrlName(), getNewFolderName());
                    } else {
                        log.error("Extended folder workflow is not available");
                        error("Unable to move folder.");
                    }
                    super.onOk();
                } catch (final Exception e) {
                    log.error("Failed to move folder.", e);
                    error(e.getLocalizedMessage());
                }
            }
        };
    }
}

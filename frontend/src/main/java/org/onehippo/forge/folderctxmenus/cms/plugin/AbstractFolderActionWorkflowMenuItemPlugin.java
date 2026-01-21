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
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.hippoecm.repository.util.WorkflowUtils;
import org.onehippo.forge.folderctxmenus.common.ExtendedFolderWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public abstract class AbstractFolderActionWorkflowMenuItemPlugin extends RenderPlugin<WorkflowDescriptor> {

    private static final String THREEPANE = "threepane";

    private static final String MENU_ITEM_ID = "menuItem";

    /**
     * @deprecated Use {@link #PRIVILEGE_FOLDERCTXMENUS_COPY} and {@link #PRIVILEGE_FOLDERCTXMENUS_MOVE} instead.
     * Kept for backward compatibility.
     */
    @Deprecated
    private static final String PRIVILEGE_FOLDERCTXMENUS_EDITOR = "folderctxmenus:editor";

    private static final String PRIVILEGE_FOLDERCTXMENUS_COPY = "folderctxmenus:copy";

    private static final String PRIVILEGE_FOLDERCTXMENUS_MOVE = "folderctxmenus:move";

    private static final Logger log = LoggerFactory.getLogger(AbstractFolderActionWorkflowMenuItemPlugin.class);

    private String destinationIdentifier;

    public String getDestinationIdentifier() {
        return destinationIdentifier;
    }

    public void setDestinationIdentifier(String destinationIdentifier) {
        this.destinationIdentifier = destinationIdentifier;
    }

    public AbstractFolderActionWorkflowMenuItemPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        if (hasRequiredPrivilege()) {
            add(createMenuItemWorkflow());
        }
    }

    /**
     * Subclasses implement to specify required privilege check.
     *
     * @return true if the user has the required privilege, false otherwise
     */
    protected abstract boolean hasRequiredPrivilege();

    /**
     * Creates the workflow menu item component using Template Method pattern.
     */
    private StdWorkflow<FolderWorkflow> createMenuItemWorkflow() {
        return new StdWorkflow<FolderWorkflow>(MENU_ITEM_ID,
                getMenuItemLabelModel(),
                (WorkflowDescriptorModel) getModel()) {

            @Override
            protected ResourceReference getIcon() {
                return getMenuItemIconResourceReference();
            }

            @Override
            protected String execute(FolderWorkflow workflow) throws Exception {
                final IDialogService dialogService = getDialogService();

                if (!dialogService.isShowingDialog()) {
                    FolderActionDocumentArguments model = createFolderActionDocumentModel();
                    dialogService.show(createDialogFactory(model).createDialog());
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
                    throw new IllegalStateException("Failed to retrieve folder information", e);
                }

                return model;
            }

            private IDialogService getDialogService() {
                return getPluginContext().getService(IDialogService.class.getName(), IDialogService.class);
            }
        };
    }

    protected abstract IModel<String> getMenuItemLabelModel();

    protected ResourceReference getMenuItemIconResourceReference() {
        return null;
    }

    protected IModel<String> getDialogTitleModel() {
        return new StringResourceModel("folder.action.dialog.title", this, null).setDefaultValue("Folder Action");
    }

    protected IDialogFactory createDialogFactory(final FolderActionDocumentArguments folderActionDocumentModel) {
        return new IDialogFactory() {
            public AbstractDialog<FolderActionDocumentArguments> createDialog() {
                return createDialogInstance(folderActionDocumentModel);
            }
        };
    }

    protected abstract AbstractDialog<FolderActionDocumentArguments> createDialogInstance(final FolderActionDocumentArguments folderActionDocumentModel);

    protected Optional<ExtendedFolderWorkflow> getExtendedFolderWorkflow(final Node sourceFolderNode) {
        return WorkflowUtils.getWorkflow(sourceFolderNode, THREEPANE, ExtendedFolderWorkflow.class);
    }

    /**
     * Checks if the current user has copy folder privilege.
     *
     * @return true if user has folderctxmenus:copy privilege, false otherwise
     */
    protected boolean userHasCopyFolderPrivilege() {
        return checkPrivilege(PRIVILEGE_FOLDERCTXMENUS_COPY);
    }

    /**
     * Checks if the current user has move folder privilege.
     *
     * @return true if user has folderctxmenus:move privilege, false otherwise
     */
    protected boolean userHasMoveFolderPrivilege() {
        return checkPrivilege(PRIVILEGE_FOLDERCTXMENUS_MOVE);
    }

    /**
     * Checks if the current user has a specific privilege.
     *
     * @param privilegeName the name of the privilege to check
     * @return true if user has the privilege, false otherwise
     */
    private boolean checkPrivilege(final String privilegeName) {
        try {
            final String path = getNode().getPath();
            final HippoSession hippoSession = UserSession.get().getJcrSession();
            final AccessControlManager accessControlManager = hippoSession.getAccessControlManager();
            return accessControlManager.hasPrivileges(path, new Privilege[]{accessControlManager.privilegeFromName(privilegeName)});
        } catch (final RepositoryException e) {
            log.error("Error checking privilege: " + privilegeName, e);
            return false;
        }
    }

    /**
     * @deprecated Use {@link #userHasCopyFolderPrivilege()} and {@link #userHasMoveFolderPrivilege()} instead.
     * Kept for backward compatibility. Returns true if user has either copy or move privilege.
     *
     * @return true if user has both copy and move privileges
     */
    @Deprecated
    protected boolean userHasAdvancedFolderPrivileges() {
        return userHasCopyFolderPrivilege() && userHasMoveFolderPrivilege();
    }

    /**
     * Retrieves the HippoNode from the workflow descriptor model.
     *
     * @return the HippoNode associated with this workflow
     * @throws RepositoryException if the node cannot be retrieved
     */
    protected HippoNode getNode() throws RepositoryException {
        return (HippoNode) ((WorkflowDescriptorModel) getDefaultModel()).getNode();
    }

}

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
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;

public abstract class AbstractFolderActionWorkflowMenuItemPlugin extends RenderPlugin<WorkflowDescriptor> {

    private String destinationIdentifier;

    public String getDestinationIdentifier() {
        return destinationIdentifier;
    }

    public void setDestinationIdentifier(String destinationIdentifier) {
        this.destinationIdentifier = destinationIdentifier;
    }

    public AbstractFolderActionWorkflowMenuItemPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(new StdWorkflow<FolderWorkflow>("menuItem",
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
                    final IDialogFactory dialogFactory = createDialogFactory();
                    dialogService.show(dialogFactory.createDialog());
                }

                return null;
            }
        });
    }

    protected abstract IModel<String> getMenuItemLabelModel();

    protected ResourceReference getMenuItemIconResourceReference() {
        return null;
    }

    protected IModel<String> getDialogTitleModel() {
        return new StringResourceModel("folder.action.dialog.title", this, null, "Folder Action");
    }

    protected IDialogFactory createDialogFactory() {
        return new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog<JcrNodeModel> createDialog() {
                return createDialogInstance();
            }
        };
    }

    protected abstract AbstractDialog<JcrNodeModel> createDialogInstance();

}

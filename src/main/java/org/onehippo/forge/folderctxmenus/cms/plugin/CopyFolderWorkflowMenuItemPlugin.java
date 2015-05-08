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

import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;

public class CopyFolderWorkflowMenuItemPlugin extends RenderPlugin<WorkflowDescriptor> {

    private static final long serialVersionUID = 1L;

    public CopyFolderWorkflowMenuItemPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
 
        add(new StdWorkflow<FolderWorkflow>("menuItem",
                new StringResourceModel("menuItem.label", this, null),
                (WorkflowDescriptorModel) getModel()) {
 
            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "copy-folder-16.png");
            }
 
            @Override
            protected String execute(FolderWorkflow workflow) throws Exception {
                String category = "new-document";
                String type = "nt:unstructured";
                String relPath = "index";
                workflow.add(category, type, relPath);
                return null;
            }
        });
    }
}

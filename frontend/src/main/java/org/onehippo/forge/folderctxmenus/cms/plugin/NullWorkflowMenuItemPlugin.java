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

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.repository.api.WorkflowDescriptor;

/**
 * No-op render plugin used to suppress platform-provided workflow menu items
 * (e.g. the built-in "Publish all in folder..." and "Take offline all in folder..."
 * items from {@code ExtendedFolderWorkflowPlugin}) by replacing their plugin
 * registration with this class, which renders nothing.
 */
public class NullWorkflowMenuItemPlugin extends RenderPlugin<WorkflowDescriptor> {

    private static final long serialVersionUID = 1L;

    public NullWorkflowMenuItemPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

}

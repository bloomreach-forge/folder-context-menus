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
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.onehippo.forge.folderctxmenus.common.FolderMoveTask;
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

                Session jcrSession = UserSession.get().getJcrSession();

                try {
                    Node sourceFolderNode = jcrSession.getNodeByIdentifier(getSourceFolderIdentifier());
                    Node destParentFolderNode = jcrSession.getNodeByIdentifier(getDestinationFolderIdentifier());

                    FolderMoveTask task =
                            new FolderMoveTask(jcrSession, UserSession.get().getLocale(), sourceFolderNode,
                                    destParentFolderNode, getNewFolderUrlName(), getNewFolderName());
                    task.execute();
                    jcrSession.save();

                    super.onOk();
                } catch (Exception e) {
                    log.error("Failed to move folder.", e);
                    error(e.getLocalizedMessage());
                } finally {
                    try {
                        jcrSession.refresh(false);
                    } catch (RepositoryException e) {
                        log.error("Failed to refresh session.", e);
                    }
                }
            }
        };
    }

}

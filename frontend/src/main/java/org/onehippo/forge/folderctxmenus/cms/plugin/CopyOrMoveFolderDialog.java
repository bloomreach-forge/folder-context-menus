/*
 * Copyright 2024 Bloomreach (https://www.bloomreach.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
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
import javax.swing.tree.TreeNode;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.tree.IJcrTreeNode;
import org.hippoecm.frontend.model.tree.JcrTreeModel;
import org.hippoecm.frontend.model.tree.JcrTreeNode;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.tree.FolderTreeNode;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.StringCodecFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CopyOrMoveFolderDialog extends AbstractFolderDialog {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(CopyOrMoveFolderDialog.class);

    private static final String DEFAULT_START_PATH = "/content/documents";

    private final FolderActionDocumentArguments folderActionDocumentModel;

    private final FolderSelectionCmsJcrTree folderTree;
    private JcrTreeModel treeModel;
    private JcrTreeNode rootTreeNode;
    private JcrNodeModel rootNodeModel;

    private String sourceFolderIdentifier = "";
    private String sourceFolderPathDisplay = "";
    private String destinationFolderIdentifier = "";
    private String destinationFolderPathDisplay = "";
    private String newFolderName = "";
    private String newFolderUrlName = "";

    public CopyOrMoveFolderDialog(final IPluginContext pluginContext, final IPluginConfig pluginConfig,
                                  IModel<String> titleModel, IModel<FolderActionDocumentArguments> model) {

        super(pluginContext, pluginConfig, titleModel, model);

        folderActionDocumentModel = model.getObject();

        if (StringUtils.isNotEmpty(folderActionDocumentModel.getSourceFolderIdentifier())) {
            try {
                Node sourceFolderNode = UserSession.get().getJcrSession().getNodeByIdentifier(folderActionDocumentModel.getSourceFolderIdentifier());
                sourceFolderIdentifier = sourceFolderNode.getIdentifier();
                sourceFolderPathDisplay = getDisplayPathOfNode(sourceFolderNode);
                newFolderName = getDisplayNameOfNode(sourceFolderNode);
                newFolderUrlName = folderActionDocumentModel.getSourceFolderUriName();
            } catch (RepositoryException e) {
                log.error("Failed to retrieve source folder node.", e);
            }
        }

        final Form form = new Form("form");

        final TextField<String> sourceFolderPathDisplayField =
            new TextField<String>("sourceFolderPathDisplay", new PropertyModel<String>(this, "sourceFolderPathDisplay"));
        sourceFolderPathDisplayField.setEnabled(false);
        form.add(sourceFolderPathDisplayField);

        final TextField<String> destinationFolderPathDisplayField =
            new TextField<String>("destinationFolderPathDisplay", new PropertyModel<String>(this, "destinationFolderPathDisplay"));
        destinationFolderPathDisplayField.setEnabled(false);
        destinationFolderPathDisplayField.setOutputMarkupId(true);
        form.add(destinationFolderPathDisplayField);

        rootNodeModel = new JcrNodeModel(DEFAULT_START_PATH);
        FolderOnlyDocumentListFilter folderOnlyTreeConfig = new FolderOnlyDocumentListFilter(pluginConfig);
        rootTreeNode = new FolderTreeNode(rootNodeModel, folderOnlyTreeConfig);
        treeModel = new JcrTreeModel(rootTreeNode);
        folderTree = new FolderSelectionCmsJcrTree("folderTree", treeModel, pluginContext, pluginConfig);
        folderTree.setRootLess(true);
        folderTree.addTreeNodeEventListener(new ITreeNodeEventListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void nodeLinkClicked(AjaxRequestTarget target, TreeNode clickedNode) {
                if (clickedNode instanceof IJcrTreeNode) {
                    try {
                        final IJcrTreeNode treeNodeModel = (IJcrTreeNode) clickedNode;
                        final Node folderNode = treeNodeModel.getNodeModel().getObject();
                        destinationFolderIdentifier = treeNodeModel.getNodeModel().getObject().getIdentifier();
                        destinationFolderPathDisplay = getDisplayPathOfNode(folderNode);
                        target.add(destinationFolderPathDisplayField);
                    } catch (RepositoryException e) {
                        log.error("Failed to retrieve selected folder node.", e);
                    }
                }
            }
        });

        form.add(folderTree);

        final TextField<String> newFolderUrlNameField =
            new TextField<String>("newFolderUrlName", new PropertyModel<String>(this, "newFolderUrlName"));
        newFolderUrlNameField.setOutputMarkupId(true);
        newFolderUrlNameField.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
            }
        });
        form.add(newFolderUrlNameField);

        final TextField<String> newFolderNameField =
            new TextField<String>("newFolderName", new PropertyModel<String>(this, "newFolderName"));
        newFolderNameField.setOutputMarkupId(true);
        newFolderNameField.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                newFolderUrlName = new StringCodecFactory.UriEncoding().encode(getNewFolderName());
                target.add(newFolderUrlNameField);
            }
        });
        form.add(newFolderNameField);

        add(form);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response
            .render(CssHeaderItem.forReference(new PackageResourceReference(CopyOrMoveFolderDialog.class,
                                                                            CopyOrMoveFolderDialog.class
                                                                                .getSimpleName() + ".css")));
    }

    @Override
    protected void onOk() {
        super.onOk();
    }

    public String getSourceFolderIdentifier() {
        return sourceFolderIdentifier;
    }

    public String getSourceFolderPathDisplay() {
        return sourceFolderPathDisplay;
    }

    public void setSourceFolderPathDisplay(String sourceFolderPathDisplay) {
        this.sourceFolderPathDisplay = sourceFolderPathDisplay;
    }

    public String getDestinationFolderIdentifier() {
        return destinationFolderIdentifier;
    }

    public String getDestinationFolderPathDisplay() {
        return destinationFolderPathDisplay;
    }

    public void setDestinationFolderPathDisplay(String destinationFolderPathDisplay) {
        this.destinationFolderPathDisplay = destinationFolderPathDisplay;
    }

    public String getNewFolderName() {
        return newFolderName;
    }

    public void setNewFolderName(String newFolderName) {
        this.newFolderName = newFolderName;
    }

    public String getNewFolderUrlName() {
        return newFolderUrlName;
    }

    public void setNewFolderUrlName(String newFolderUrlName) {
        this.newFolderUrlName = newFolderUrlName;
    }
}

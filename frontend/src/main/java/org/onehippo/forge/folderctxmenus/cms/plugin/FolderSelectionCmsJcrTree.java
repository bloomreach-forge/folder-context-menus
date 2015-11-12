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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.tree.TreeNode;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tree.ITreeState;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.hippoecm.frontend.model.tree.IJcrTreeNode;
import org.hippoecm.frontend.model.tree.JcrTreeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.browse.tree.CmsJcrTree;
import org.hippoecm.frontend.plugins.standards.tree.icon.DefaultTreeNodeIconProvider;
import org.hippoecm.frontend.plugins.standards.tree.icon.ITreeNodeIconProvider;

public class FolderSelectionCmsJcrTree extends CmsJcrTree {

    private static final long serialVersionUID = 1L;

    private List<ITreeNodeEventListener> treeNodeEventListeners = new ArrayList<ITreeNodeEventListener>();

    public FolderSelectionCmsJcrTree(String id, JcrTreeModel treeModel, final IPluginContext context, final IPluginConfig config) {
        super(id, treeModel, new CmsJcrTree.TreeNodeTranslator(), createTreeNodeIconProvider(context, config));
    }

    public void addTreeNodeEventListener(ITreeNodeEventListener treeNodeEventListener) {
        treeNodeEventListeners.add(treeNodeEventListener);
    }

    public void removeTreeNodeEventListener(ITreeNodeEventListener treeNodeEventListener) {
        treeNodeEventListeners.remove(treeNodeEventListener);
    }

    @Override
    protected MarkupContainer newContextContent(MarkupContainer parent, String id, final TreeNode node) {
        return new EmptyPanel(id);
    }

    @Override
    protected void onNodeLinkClicked(AjaxRequestTarget target, TreeNode clickedNode) {
        if (clickedNode instanceof IJcrTreeNode) {
            IJcrTreeNode treeNodeModel = (IJcrTreeNode) clickedNode;

            for (ITreeNodeEventListener treeNodeEventListener : treeNodeEventListeners) {
                treeNodeEventListener.nodeLinkClicked(target, clickedNode);
            }

            //FolderTreePlugin.this.setDefaultModel(treeNodeModel.getNodeModel());
            ITreeState state = getTreeState();
            if (state.isNodeExpanded(clickedNode)) {
                // super has already switched selection.
                if (!state.isNodeSelected(clickedNode)) {
                    state.collapseNode(clickedNode);
                }
            } else {
                state.expandNode(clickedNode);
            }
        }
        updateTree(target);
    }

    @Override
    protected void onJunctionLinkClicked(AjaxRequestTarget target, TreeNode node) {
        updateTree(target);
    }

    private static ITreeNodeIconProvider createTreeNodeIconProvider(final IPluginContext context, final IPluginConfig config) {
        final List<ITreeNodeIconProvider> providers = new LinkedList<ITreeNodeIconProvider>();
        providers.add(new DefaultTreeNodeIconProvider());
        providers.addAll(context.getServices(ITreeNodeIconProvider.class.getName(), ITreeNodeIconProvider.class));
        Collections.reverse(providers);

        return new ITreeNodeIconProvider() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getNodeIcon(String id, TreeNode treeNode, ITreeState state) {
                for (ITreeNodeIconProvider provider : providers) {
                    Component icon = provider.getNodeIcon(id, treeNode, state);

                    if (icon != null) {
                        return icon;
                    }
                }

                throw new RuntimeException("No icon could be found for tree node");
            }
        };
    }

}

/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractFolderDialog extends AbstractDialog<JcrNodeModel> {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(AbstractFolderDialog.class);

    private final IPluginContext pluginContext;
    private final IPluginConfig pluginConfig;

    private IValueMap dialogProperties = new ValueMap("width=640,height=480").makeImmutable();

    private final IModel<String> titleModel;

    public AbstractFolderDialog(IPluginContext pluginContext, IPluginConfig pluginConfig, IModel<String> titleModel, IModel<JcrNodeModel> model) {
        super(model);

        this.pluginContext = pluginContext;
        this.pluginConfig = pluginConfig;

        setOutputMarkupId(true);
        this.titleModel = titleModel;
    }

    public IPluginContext getPluginContext() {
        return pluginContext;
    }

    public IPluginConfig getPluginConfig() {
        return pluginConfig;
    }

    @Override
    public IValueMap getProperties() {
        return dialogProperties;
    }

    @Override
    public IModel<String> getTitle() {
        return titleModel;
    }

    protected String getDisplayPathOfNode(final Node node) {
        List<String> pathItemList = new LinkedList<String>();

        try {
            Node curNode = node;

            while (curNode != null) {
                String curNodePath = curNode.getPath();

                if (StringUtils.equals(curNodePath, "/content/documents") ||
                        StringUtils.equals(curNodePath, "/content") ||
                        StringUtils.equals(curNodePath, "/") ||
                        StringUtils.isBlank(curNodePath)) {
                    break;
                } else if (curNode.isSame(curNode.getSession().getRootNode())) {
                    break;
                }

                String nodeDisplayName = null;

                if (curNode.hasNode("hippo:translation")) {
                    nodeDisplayName = curNode.getNode("hippo:translation").getProperty("hippo:message").getString();
                }

                if (StringUtils.isBlank(nodeDisplayName)) {
                    nodeDisplayName = curNode.getName();
                }

                pathItemList.add(0, nodeDisplayName);

                curNode = curNode.getParent();
            }
        } catch (RepositoryException e) {
            log.warn("Failed to get display node path", e);
        }

        return StringUtils.join(pathItemList, " / ");
    }

}

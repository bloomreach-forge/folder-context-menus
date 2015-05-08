/**
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CopyOrMoveFolderDialog extends AbstractDialog<JcrNodeModel> {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(CopyOrMoveFolderDialog.class);

    private final IModel<String> titleModel;

    public CopyOrMoveFolderDialog(IModel<String> titleModel, IModel<JcrNodeModel> model) {
        super(model);
        setOutputMarkupId(true);

        this.titleModel = titleModel;
    }

    @Override
    protected void onOk() {
    }

    @Override
    public IModel<String> getTitle() {
        return titleModel;
    }

}

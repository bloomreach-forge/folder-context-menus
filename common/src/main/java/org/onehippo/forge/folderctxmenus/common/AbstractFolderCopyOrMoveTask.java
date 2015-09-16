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
package org.onehippo.forge.folderctxmenus.common;

import java.util.Locale;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

public abstract class AbstractFolderCopyOrMoveTask extends AbstractFolderTask {

    private final Node destParentFolderNode;
    private final String destFolderNodeName;
    private final String destFolderDisplayName;
    private Node destFolderNode;

    public AbstractFolderCopyOrMoveTask(final Session session, final Locale locale, final Node sourceFolderNode,
            final Node destParentFolderNode, final String destFolderNodeName, final String destFolderDisplayName) {
        super(session, locale, sourceFolderNode);

        this.destParentFolderNode = destParentFolderNode;
        this.destFolderNodeName = destFolderNodeName;
        this.destFolderDisplayName = destFolderDisplayName;
    }


    public Node getDestParentFolderNode() {
        return destParentFolderNode;
    }

    public String getDestFolderNodeName() {
        return destFolderNodeName;
    }

    public String getDestFolderDisplayName() {
        return destFolderDisplayName;
    }

    public Node getDestFolderNode() {
        return destFolderNode;
    }

    public void setDestFolderNode(final Node destFolderNode) {
        this.destFolderNode = destFolderNode;
    }

    public String getDestFolderPath() throws RepositoryException {
        return getDestParentFolderNode().getPath() + "/" + getDestFolderNodeName();
    }

}

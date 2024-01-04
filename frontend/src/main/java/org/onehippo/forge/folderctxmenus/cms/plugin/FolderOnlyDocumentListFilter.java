/*
 * Copyright 2023 Bloomreach (https://www.bloomreach.com)
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

import java.util.NoSuchElementException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.DocumentListFilter;

public class FolderOnlyDocumentListFilter extends DocumentListFilter {

    private static final long serialVersionUID = 1L;

    public FolderOnlyDocumentListFilter(IPluginConfig pluginConfig) {
        super(pluginConfig);
    }

    @Override
    public NodeIterator filter(Node current, final NodeIterator iter) {
        return new NodeIterator() {
            private int index = 0;
            private Node nextNode;

            private void fillNextNode() {
                nextNode = null;

                try {
                    while (iter.hasNext()) {
                        Node candidate = iter.nextNode();

                        if (candidate.isNodeType("hippostd:folder") || candidate.isNodeType("hippostd:directory")) {
                            nextNode = candidate;
                            break;
                        }
                    }
                } catch (RepositoryException ignored) {
                }
            }

            public Node nextNode() {
                if (nextNode == null) {
                    fillNextNode();
                }
                if (nextNode == null) {
                    throw new NoSuchElementException();
                }
                Node rtValue = nextNode;
                nextNode = null;
                return rtValue;
            }

            public long getPosition() {
                return index;
            }

            public long getSize() {
                return -1;
            }

            public void skip(long count) {
                while (count-- > 0) {
                    nextNode();
                }
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }

            public Object next() {
                return nextNode();
            }

            public boolean hasNext() {
                if (nextNode == null) {
                    fillNextNode();
                }

                if (nextNode == null) {
                    return false;
                }

                return true;
            }
        };
    }

}

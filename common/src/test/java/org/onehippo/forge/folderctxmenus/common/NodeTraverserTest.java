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
package org.onehippo.forge.folderctxmenus.common;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NodeTraverserTest {

    @Test
    void acceptAll_shouldAcceptAllNodes() throws RepositoryException {
        Node node = mock(Node.class);

        assertTrue(NodeTraverser.ACCEPT_ALL.isAcceptable(node));
    }

    @Test
    void acceptAll_shouldTraverseAllNodes() throws RepositoryException {
        Node node = mock(Node.class);

        assertTrue(NodeTraverser.ACCEPT_ALL.isTraversable(node));
    }

    @Test
    void acceptAll_acceptShouldBeNoOp() throws RepositoryException {
        Node node = mock(Node.class);

        // Should not throw
        NodeTraverser.ACCEPT_ALL.accept(node);
    }

    @Test
    void userVisibleItems_shouldAcceptFolders() throws RepositoryException {
        Node folder = mock(Node.class);
        when(folder.isNodeType("nt:folder")).thenReturn(true);
        when(folder.isNodeType("hippo:handle")).thenReturn(false);

        assertTrue(NodeTraverser.USER_VISIBLE_ITEMS.isAcceptable(folder));
    }

    @Test
    void userVisibleItems_shouldAcceptHandles() throws RepositoryException {
        Node handle = mock(Node.class);
        when(handle.isNodeType("nt:folder")).thenReturn(false);
        when(handle.isNodeType("hippo:handle")).thenReturn(true);

        assertTrue(NodeTraverser.USER_VISIBLE_ITEMS.isAcceptable(handle));
    }

    @Test
    void userVisibleItems_shouldRejectOtherNodeTypes() throws RepositoryException {
        Node other = mock(Node.class);
        when(other.isNodeType("nt:folder")).thenReturn(false);
        when(other.isNodeType("hippo:handle")).thenReturn(false);

        assertFalse(NodeTraverser.USER_VISIBLE_ITEMS.isAcceptable(other));
    }

    @Test
    void userVisibleItems_shouldTraverseFolders() throws RepositoryException {
        Node folder = mock(Node.class);
        when(folder.isNodeType("hippo:handle")).thenReturn(false);

        assertTrue(NodeTraverser.USER_VISIBLE_ITEMS.isTraversable(folder));
    }

    @Test
    void userVisibleItems_shouldNotTraverseHandles() throws RepositoryException {
        Node handle = mock(Node.class);
        when(handle.isNodeType("hippo:handle")).thenReturn(true);

        assertFalse(NodeTraverser.USER_VISIBLE_ITEMS.isTraversable(handle));
    }

    @Test
    void userVisibleItems_acceptShouldBeNoOp() throws RepositoryException {
        Node node = mock(Node.class);
        when(node.isNodeType("nt:folder")).thenReturn(true);

        // Should not throw
        NodeTraverser.USER_VISIBLE_ITEMS.accept(node);
    }
}

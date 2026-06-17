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

import java.util.NoSuchElementException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FolderOnlyDocumentListFilterTest {

    private FolderOnlyDocumentListFilter filter;

    @BeforeEach
    void setUp() {
        IPluginConfig config = mock(IPluginConfig.class);
        filter = new FolderOnlyDocumentListFilter(config);
    }

    // ---- helper to build a mock NodeIterator from vararg nodes ----

    private NodeIterator iteratorOf(Node... nodes) {
        NodeIterator iter = mock(NodeIterator.class);
        if (nodes.length == 0) {
            when(iter.hasNext()).thenReturn(false);
        } else {
            // Build hasNext sequence: [true, true, ..., false] with exactly nodes.length trues
            Boolean[] hasNextRest = new Boolean[nodes.length]; // rest array: (nodes.length - 1) trues + 1 false
            for (int i = 0; i < nodes.length - 1; i++) {
                hasNextRest[i] = true;
            }
            hasNextRest[nodes.length - 1] = false;
            when(iter.hasNext()).thenReturn(true, hasNextRest);

            // Build nextNode sequence
            if (nodes.length == 1) {
                when(iter.nextNode()).thenReturn(nodes[0]);
            } else {
                Node[] rest = new Node[nodes.length - 1];
                System.arraycopy(nodes, 1, rest, 0, nodes.length - 1);
                when(iter.nextNode()).thenReturn(nodes[0], rest);
            }
        }
        return iter;
    }

    private Node folderNode(String type) throws RepositoryException {
        Node node = mock(Node.class);
        boolean isFolder = "hippostd:folder".equals(type);
        boolean isDirectory = "hippostd:directory".equals(type);
        when(node.isNodeType("hippostd:folder")).thenReturn(isFolder);
        when(node.isNodeType("hippostd:directory")).thenReturn(isDirectory);
        return node;
    }

    private Node nonFolderNode() throws RepositoryException {
        Node node = mock(Node.class);
        when(node.isNodeType("hippostd:folder")).thenReturn(false);
        when(node.isNodeType("hippostd:directory")).thenReturn(false);
        return node;
    }

    // ---- filter(NodeIterator) ----

    @Test
    void filter_withEmptyIterator_hasNextReturnsFalse() {
        NodeIterator result = filter.filter(iteratorOf());
        assertFalse(result.hasNext());
    }

    @Test
    void filter_withOnlyNonFolderNodes_hasNextReturnsFalse() throws RepositoryException {
        Node doc = nonFolderNode();
        NodeIterator result = filter.filter(iteratorOf(doc));
        assertFalse(result.hasNext());
    }

    @Test
    void filter_withHippostdFolder_hasNextReturnsTrue() throws RepositoryException {
        Node folder = folderNode("hippostd:folder");
        NodeIterator result = filter.filter(iteratorOf(folder));
        assertTrue(result.hasNext());
    }

    @Test
    void filter_withHippostdDirectory_hasNextReturnsTrue() throws RepositoryException {
        Node dir = folderNode("hippostd:directory");
        NodeIterator result = filter.filter(iteratorOf(dir));
        assertTrue(result.hasNext());
    }

    @Test
    void filter_nextNode_returnsFolderNode() throws RepositoryException {
        Node folder = folderNode("hippostd:folder");
        NodeIterator result = filter.filter(iteratorOf(folder));
        assertSame(folder, result.nextNode());
    }

    @Test
    void filter_skipsNonFolderAndReturnsFolderAfterIt() throws RepositoryException {
        Node doc = nonFolderNode();
        Node folder = folderNode("hippostd:folder");
        NodeIterator result = filter.filter(iteratorOf(doc, folder));
        assertTrue(result.hasNext());
        assertSame(folder, result.nextNode());
        assertFalse(result.hasNext());
    }

    @Test
    void filter_nextNode_whenNoMore_throwsNoSuchElementException() {
        NodeIterator result = filter.filter(iteratorOf());
        assertThrows(NoSuchElementException.class, result::nextNode);
    }

    @Test
    void filter_next_returnsFolderNode() throws RepositoryException {
        Node folder = folderNode("hippostd:folder");
        NodeIterator result = filter.filter(iteratorOf(folder));
        assertSame(folder, result.next());
    }

    @Test
    void filter_getSize_returnsMinusOne() {
        NodeIterator result = filter.filter(iteratorOf());
        assertEquals(-1, result.getSize());
    }

    @Test
    void filter_getPosition_returnsZeroInitially() {
        NodeIterator result = filter.filter(iteratorOf());
        assertEquals(0, result.getPosition());
    }

    @Test
    void filter_remove_throwsUnsupportedOperationException() throws RepositoryException {
        Node folder = folderNode("hippostd:folder");
        NodeIterator result = filter.filter(iteratorOf(folder));
        assertThrows(UnsupportedOperationException.class, result::remove);
    }

    @Test
    void filter_skip_advancesPastNodes() throws RepositoryException {
        Node folder1 = folderNode("hippostd:folder");
        Node folder2 = folderNode("hippostd:folder");
        NodeIterator result = filter.filter(iteratorOf(folder1, folder2));
        // skip 1 consumes folder1 via nextNode(); folder2 should then be next
        result.skip(1);
        assertSame(folder2, result.nextNode());
    }

    @Test
    void filter_repositoryExceptionDuringIsNodeType_treatedAsNonFolder() throws RepositoryException {
        Node badNode = mock(Node.class);
        when(badNode.isNodeType(anyString())).thenThrow(new RepositoryException("repo error"));
        NodeIterator result = filter.filter(iteratorOf(badNode));
        // RepositoryException is swallowed (ignored block); the node is treated as non-folder
        assertFalse(result.hasNext());
    }

    // ---- filter(Node, NodeIterator) delegates to filter(NodeIterator) ----

    @Test
    void filter_withCurrentNode_delegatesToIteratorFilter() throws RepositoryException {
        Node current = mock(Node.class);
        Node folder = folderNode("hippostd:folder");
        NodeIterator iter = iteratorOf(folder);
        NodeIterator result = filter.filter(current, iter);
        assertTrue(result.hasNext());
        assertSame(folder, result.nextNode());
    }

    @Test
    void filter_multipleHasNextCallsAreSafe() throws RepositoryException {
        Node folder = folderNode("hippostd:folder");
        NodeIterator result = filter.filter(iteratorOf(folder));
        // Calling hasNext multiple times before nextNode should not consume the node
        assertTrue(result.hasNext());
        assertTrue(result.hasNext());
        assertSame(folder, result.nextNode());
        assertFalse(result.hasNext());
    }
}

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

import java.util.concurrent.atomic.AtomicLong;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class JcrTraverseUtilsTest {

    private Node rootNode;
    private Node childNode1;
    private Node childNode2;
    private OperationProgress progress;
    private NodeTraverser traverser;

    @BeforeEach
    public void setUp() throws RepositoryException {
        rootNode = mock(Node.class);
        childNode1 = mock(Node.class);
        childNode2 = mock(Node.class);
        progress = mock(OperationProgress.class);
        traverser = mock(NodeTraverser.class);

        when(rootNode.getPath()).thenReturn("/root");
        when(childNode1.getPath()).thenReturn("/root/child1");
        when(childNode2.getPath()).thenReturn("/root/child2");

        // Configure nodes as folders for user-visible item detection
        when(rootNode.isNodeType("nt:folder")).thenReturn(true);
        when(rootNode.isNodeType("hippo:handle")).thenReturn(false);
        when(childNode1.isNodeType("nt:folder")).thenReturn(true);
        when(childNode1.isNodeType("hippo:handle")).thenReturn(false);
        when(childNode2.isNodeType("nt:folder")).thenReturn(true);
        when(childNode2.isNodeType("hippo:handle")).thenReturn(false);

        NodeIterator rootIterator = mock(NodeIterator.class);
        when(rootIterator.hasNext()).thenReturn(true, true, false);
        when(rootIterator.nextNode()).thenReturn(childNode1, childNode2);

        NodeIterator child1Iterator = mock(NodeIterator.class);
        when(child1Iterator.hasNext()).thenReturn(false);

        NodeIterator child2Iterator = mock(NodeIterator.class);
        when(child2Iterator.hasNext()).thenReturn(false);

        when(rootNode.hasNodes()).thenReturn(true);
        when(rootNode.getNodes()).thenReturn(rootIterator);

        when(childNode1.hasNodes()).thenReturn(false);
        when(childNode2.hasNodes()).thenReturn(false);

        when(traverser.isAcceptable(any(Node.class))).thenReturn(true);
        when(traverser.isTraversable(any(Node.class))).thenReturn(true);
        when(progress.isCancelled()).thenReturn(false);
    }

    @Test
    public void traverseNodes_withProgress_shouldUpdateProgress() throws RepositoryException {
        AtomicLong counter = new AtomicLong(0);
        long total = 3;

        JcrTraverseUtils.traverseNodes(rootNode, traverser, progress, counter, total);

        verify(progress, times(3)).updateProgress(anyLong(), eq(total), anyString());
        verify(progress).updateProgress(1, total, "/root");
        verify(progress).updateProgress(2, total, "/root/child1");
        verify(progress).updateProgress(3, total, "/root/child2");
    }

    @Test
    public void traverseNodes_withProgress_shouldCallOnProgressUpdated() throws RepositoryException {
        AtomicLong counter = new AtomicLong(0);
        long total = 3;

        JcrTraverseUtils.traverseNodes(rootNode, traverser, progress, counter, total);

        verify(progress, times(3)).onProgressUpdated();
    }

    @Test
    public void traverseNodes_withProgress_shouldInvokeTraverserForAcceptableNodes() throws RepositoryException {
        AtomicLong counter = new AtomicLong(0);
        long total = 3;

        JcrTraverseUtils.traverseNodes(rootNode, traverser, progress, counter, total);

        verify(traverser, times(3)).accept(any(Node.class));
    }

    @Test
    public void traverseNodes_withProgress_shouldCheckCancellation() throws RepositoryException {
        AtomicLong counter = new AtomicLong(0);
        long total = 3;

        JcrTraverseUtils.traverseNodes(rootNode, traverser, progress, counter, total);

        verify(progress, atLeastOnce()).isCancelled();
    }

    @Test
    public void traverseNodes_withProgress_shouldThrowExceptionWhenCancelled() throws RepositoryException {
        AtomicLong counter = new AtomicLong(0);
        long total = 3;

        when(progress.isCancelled()).thenReturn(true);

        assertThrows(OperationCancelledException.class,
                () -> JcrTraverseUtils.traverseNodes(rootNode, traverser, progress, counter, total));
    }

    @Test
    public void traverseNodes_withoutProgress_shouldTraverseNormally() throws RepositoryException {
        JcrTraverseUtils.traverseNodes(rootNode, traverser);

        verify(traverser, times(3)).accept(any(Node.class));
    }

    @Test
    public void traverseNodes_withNullProgress_shouldNotFail() throws RepositoryException {
        AtomicLong counter = new AtomicLong(0);
        long total = 3;

        JcrTraverseUtils.traverseNodes(rootNode, traverser, null, counter, total);

        verify(traverser, times(3)).accept(any(Node.class));
    }

    @Test
    public void traverseNodes_withNullTraverser_shouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> JcrTraverseUtils.traverseNodes(rootNode, null));
    }

    @Test
    public void traverseNodes_withProgress_withNullTraverser_shouldThrowException() {
        AtomicLong counter = new AtomicLong(0);

        assertThrows(IllegalArgumentException.class,
                () -> JcrTraverseUtils.traverseNodes(rootNode, null, progress, counter, 10));
    }

    @Test
    public void traverseNodes_withoutProgress_whenNodeNotAcceptable_doesNotCallAccept() throws RepositoryException {
        // Root is not acceptable; children are still acceptable via the any() stub from setUp
        when(traverser.isAcceptable(rootNode)).thenReturn(false);

        JcrTraverseUtils.traverseNodes(rootNode, traverser);

        verify(traverser, never()).accept(rootNode);
        // children are still accepted
        verify(traverser, times(2)).accept(any(Node.class));
    }

    @Test
    public void traverseNodes_withoutProgress_whenNotTraversable_doesNotVisitChildren() throws RepositoryException {
        when(traverser.isTraversable(rootNode)).thenReturn(false);

        JcrTraverseUtils.traverseNodes(rootNode, traverser);

        // Root is accepted, but children are never visited
        verify(traverser, times(1)).accept(rootNode);
        verify(traverser, never()).accept(childNode1);
        verify(traverser, never()).accept(childNode2);
    }

    @Test
    public void traverseNodes_withoutProgress_whenHasNoChildren_doesNotRecurse() throws RepositoryException {
        when(rootNode.hasNodes()).thenReturn(false);

        JcrTraverseUtils.traverseNodes(rootNode, traverser);

        verify(traverser, times(1)).accept(rootNode);
        verify(traverser, never()).accept(childNode1);
        verify(traverser, never()).accept(childNode2);
    }

    @Test
    public void traverseNodes_withoutProgress_whenIteratorYieldsNullChild_skipsNull() throws RepositoryException {
        NodeIterator nullChildIterator = mock(NodeIterator.class);
        when(nullChildIterator.hasNext()).thenReturn(true, false);
        when(nullChildIterator.nextNode()).thenReturn(null);
        when(rootNode.getNodes()).thenReturn(nullChildIterator);

        JcrTraverseUtils.traverseNodes(rootNode, traverser);

        // Only root is accepted; null child is skipped
        verify(traverser, times(1)).accept(rootNode);
    }

    @Test
    public void traverseNodes_withProgress_whenNodeNotUserVisible_doesNotUpdateProgressForThatNode()
            throws RepositoryException {
        // Make childNode1 neither a folder nor a handle → not user-visible
        when(childNode1.isNodeType("nt:folder")).thenReturn(false);
        when(childNode1.isNodeType("hippo:handle")).thenReturn(false);

        AtomicLong counter = new AtomicLong(0);

        JcrTraverseUtils.traverseNodes(rootNode, traverser, progress, counter, 3);

        // root and childNode2 are nt:folder → 2 progress updates; childNode1 is skipped
        verify(progress, times(2)).updateProgress(anyLong(), eq(3L), anyString());
    }

    @Test
    public void traverseNodes_withProgress_whenIteratorYieldsNullChild_skipsNull() throws RepositoryException {
        NodeIterator nullChildIterator = mock(NodeIterator.class);
        when(nullChildIterator.hasNext()).thenReturn(true, false);
        when(nullChildIterator.nextNode()).thenReturn(null);
        when(rootNode.getNodes()).thenReturn(nullChildIterator);

        AtomicLong counter = new AtomicLong(0);

        JcrTraverseUtils.traverseNodes(rootNode, traverser, progress, counter, 1);

        // Only root triggers progress; null child is skipped without error
        verify(traverser, times(1)).accept(rootNode);
    }

}

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
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class NodeCounterTest {

    private Node rootNode;
    private Node childNode1;
    private Node childNode2;
    private Node grandChildNode;

    @BeforeEach
    public void setUp() throws RepositoryException {
        rootNode = mock(Node.class);
        childNode1 = mock(Node.class);
        childNode2 = mock(Node.class);
        grandChildNode = mock(Node.class);

        NodeIterator rootIterator = mock(NodeIterator.class);
        when(rootIterator.hasNext()).thenReturn(true, true, false);
        when(rootIterator.nextNode()).thenReturn(childNode1, childNode2);

        NodeIterator child1Iterator = mock(NodeIterator.class);
        when(child1Iterator.hasNext()).thenReturn(true, false);
        when(child1Iterator.nextNode()).thenReturn(grandChildNode);

        NodeIterator child2Iterator = mock(NodeIterator.class);
        when(child2Iterator.hasNext()).thenReturn(false);

        NodeIterator grandChildIterator = mock(NodeIterator.class);
        when(grandChildIterator.hasNext()).thenReturn(false);

        when(rootNode.hasNodes()).thenReturn(true);
        when(rootNode.getNodes()).thenReturn(rootIterator);

        when(childNode1.hasNodes()).thenReturn(true);
        when(childNode1.getNodes()).thenReturn(child1Iterator);

        when(childNode2.hasNodes()).thenReturn(false);

        when(grandChildNode.hasNodes()).thenReturn(false);
    }

    @Test
    public void countNodes_shouldCountAllAcceptableNodes() throws RepositoryException {
        NodeTraverser traverser = new NodeTraverser() {
            @Override
            public boolean isAcceptable(Node node) throws RepositoryException {
                return true;
            }

            @Override
            public boolean isTraversable(Node node) throws RepositoryException {
                return true;
            }

            @Override
            public void accept(Node node) throws RepositoryException {
            }
        };

        long count = NodeCounter.countNodes(rootNode, traverser);

        assertEquals(4, count);
    }

    @Test
    public void countNodes_shouldCountOnlyAcceptableNodes() throws RepositoryException {
        NodeTraverser traverser = new NodeTraverser() {
            @Override
            public boolean isAcceptable(Node node) throws RepositoryException {
                return node == rootNode || node == grandChildNode;
            }

            @Override
            public boolean isTraversable(Node node) throws RepositoryException {
                return true;
            }

            @Override
            public void accept(Node node) throws RepositoryException {
            }
        };

        long count = NodeCounter.countNodes(rootNode, traverser);

        assertEquals(2, count);
    }

    @Test
    public void countNodes_shouldStopTraversingWhenNotTraversable() throws RepositoryException {
        NodeTraverser traverser = new NodeTraverser() {
            @Override
            public boolean isAcceptable(Node node) throws RepositoryException {
                return true;
            }

            @Override
            public boolean isTraversable(Node node) throws RepositoryException {
                return node == rootNode;
            }

            @Override
            public void accept(Node node) throws RepositoryException {
            }
        };

        long count = NodeCounter.countNodes(rootNode, traverser);

        assertEquals(3, count);
    }

    @Test
    public void countNodes_shouldReturnZeroWhenNoNodesAreAcceptable() throws RepositoryException {
        NodeTraverser traverser = new NodeTraverser() {
            @Override
            public boolean isAcceptable(Node node) throws RepositoryException {
                return false;
            }

            @Override
            public boolean isTraversable(Node node) throws RepositoryException {
                return true;
            }

            @Override
            public void accept(Node node) throws RepositoryException {
            }
        };

        long count = NodeCounter.countNodes(rootNode, traverser);

        assertEquals(0, count);
    }

    @Test
    public void countNodes_shouldThrowExceptionWhenTraverserIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> NodeCounter.countNodes(rootNode, null));
    }

}

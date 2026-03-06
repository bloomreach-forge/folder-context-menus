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
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.hippoecm.repository.api.HippoNodeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FolderDeleteTaskTest {

    private Session session;
    private Node folderNode;

    @BeforeEach
    void setUp() throws RepositoryException {
        session = mock(Session.class);
        folderNode = mock(Node.class);
        when(folderNode.getPath()).thenReturn("/content/documents/site/myfolder");
    }

    // --- Helpers: fully stubbed before being passed to thenReturn() ---

    private NodeIterator emptyIterator() {
        NodeIterator it = mock(NodeIterator.class);
        when(it.hasNext()).thenReturn(false);
        return it;
    }

    private NodeIterator iteratorOf(Node first, Node... rest) {
        NodeIterator it = mock(NodeIterator.class);
        if (rest.length == 0) {
            when(it.hasNext()).thenReturn(true, false);
            when(it.nextNode()).thenReturn(first);
        } else {
            Node[] all = new Node[1 + rest.length];
            all[0] = first;
            System.arraycopy(rest, 0, all, 1, rest.length);

            Boolean[] hasNextSeq = new Boolean[all.length + 1];
            for (int i = 0; i < all.length; i++) {
                hasNextSeq[i] = true;
            }
            hasNextSeq[all.length] = false;

            when(it.hasNext()).thenReturn(hasNextSeq[0],
                    java.util.Arrays.copyOfRange(hasNextSeq, 1, hasNextSeq.length));
            when(it.nextNode()).thenReturn(all[0],
                    java.util.Arrays.copyOfRange(all, 1, all.length));
        }
        return it;
    }

    private Node mockHandleWithLiveVariant(String handlePath) throws RepositoryException {
        Node handle = mock(Node.class);
        when(handle.getPath()).thenReturn(handlePath);
        when(handle.getName()).thenReturn("article");
        when(handle.isNodeType(HippoNodeType.NT_HANDLE)).thenReturn(true);
        when(handle.isNodeType(HippoNodeType.NT_DOCUMENT)).thenReturn(false);
        when(handle.isNodeType("hippostd:folder")).thenReturn(false);
        when(handle.isNodeType("hippotranslation:translated")).thenReturn(false);

        Value liveValue = mock(Value.class);
        when(liveValue.getString()).thenReturn("live");

        Property availProp = mock(Property.class);
        when(availProp.getValues()).thenReturn(new Value[]{liveValue});

        Node variant = mock(Node.class);
        when(variant.hasProperty("hippo:availability")).thenReturn(true);
        when(variant.getProperty("hippo:availability")).thenReturn(availProp);

        // iteratorOf fully set up before being used
        NodeIterator variantIt = iteratorOf(variant);
        when(handle.getNodes("article")).thenReturn(variantIt);

        return handle;
    }

    private Node mockHandleWithOfflineVariant() throws RepositoryException {
        Node handle = mock(Node.class);
        when(handle.getPath()).thenReturn("/content/documents/site/myfolder/offline");
        when(handle.getName()).thenReturn("offline");
        when(handle.isNodeType(HippoNodeType.NT_HANDLE)).thenReturn(true);
        when(handle.isNodeType(HippoNodeType.NT_DOCUMENT)).thenReturn(false);
        when(handle.isNodeType("hippostd:folder")).thenReturn(false);
        when(handle.isNodeType("hippotranslation:translated")).thenReturn(false);

        Property availProp = mock(Property.class);
        when(availProp.getValues()).thenReturn(new Value[0]);

        Node variant = mock(Node.class);
        when(variant.hasProperty("hippo:availability")).thenReturn(true);
        when(variant.getProperty("hippo:availability")).thenReturn(availProp);

        NodeIterator variantIt = iteratorOf(variant);
        when(handle.getNodes("offline")).thenReturn(variantIt);

        return handle;
    }

    // --- Tests ---

    @Test
    void execute_emptyFolder_removesSourceNode() throws RepositoryException {
        NodeIterator children = emptyIterator();
        when(folderNode.getNodes()).thenReturn(children);

        FolderDeleteTask task = new FolderDeleteTask(session, folderNode);
        task.execute();

        verify(folderNode).remove();
    }

    @Test
    void execute_folderWithOfflineDocument_removesSourceNode() throws RepositoryException {
        Node handle = mockHandleWithOfflineVariant();
        NodeIterator children = iteratorOf(handle);
        when(folderNode.getNodes()).thenReturn(children);

        FolderDeleteTask task = new FolderDeleteTask(session, folderNode);
        task.execute();

        verify(folderNode).remove();
    }

    @Test
    void execute_folderWithLiveDocument_throwsPublishedContentException() throws RepositoryException {
        Node handle = mockHandleWithLiveVariant("/content/documents/site/myfolder/article");
        NodeIterator children = iteratorOf(handle);
        when(folderNode.getNodes()).thenReturn(children);

        FolderDeleteTask task = new FolderDeleteTask(session, folderNode);
        PublishedContentException ex = assertThrows(PublishedContentException.class, task::execute);

        assertTrue(ex.getDocumentPaths().contains("/content/documents/site/myfolder/article"));
        verify(folderNode, never()).remove();
    }

    @Test
    void execute_folderWithMultipleLiveDocs_collectsAllPaths() throws RepositoryException {
        Node handle1 = mockHandleWithLiveVariant("/content/documents/site/folder/doc1");
        Node handle2 = mockHandleWithLiveVariant("/content/documents/site/folder/doc2");
        NodeIterator children = iteratorOf(handle1, handle2);
        when(folderNode.getNodes()).thenReturn(children);

        FolderDeleteTask task = new FolderDeleteTask(session, folderNode);
        PublishedContentException ex = assertThrows(PublishedContentException.class, task::execute);

        assertEquals(2, ex.getDocumentPaths().size());
        verify(folderNode, never()).remove();
    }

    @Test
    void execute_withOperationProgress_updatesProgress() throws RepositoryException {
        NodeIterator children = emptyIterator();
        when(folderNode.getNodes()).thenReturn(children);
        OperationProgress progress = mock(OperationProgress.class);

        FolderDeleteTask task = new FolderDeleteTask(session, folderNode);
        task.setOperationProgress(progress);
        task.execute();

        verify(progress).updateProgress(0, 1, "Checking for published content...");
        verify(progress).updateProgress(1, 1, "Deleting folder...");
    }

    @Test
    void execute_withNoOperationProgress_doesNotThrow() throws RepositoryException {
        NodeIterator children = emptyIterator();
        when(folderNode.getNodes()).thenReturn(children);

        FolderDeleteTask task = new FolderDeleteTask(session, folderNode);
        assertDoesNotThrow(task::execute);
    }

    @Test
    void execute_nestedFolderWithLiveDoc_isDetected() throws RepositoryException {
        Node handle = mockHandleWithLiveVariant("/content/documents/site/folder/sub/article");
        NodeIterator subChildren = iteratorOf(handle);

        Node subFolder = mock(Node.class);
        when(subFolder.isNodeType(HippoNodeType.NT_HANDLE)).thenReturn(false);
        when(subFolder.isNodeType(HippoNodeType.NT_DOCUMENT)).thenReturn(false);
        when(subFolder.isNodeType("hippostd:folder")).thenReturn(true);
        when(subFolder.getNodes()).thenReturn(subChildren);

        NodeIterator topChildren = iteratorOf(subFolder);
        when(folderNode.getNodes()).thenReturn(topChildren);

        FolderDeleteTask task = new FolderDeleteTask(session, folderNode);
        PublishedContentException ex = assertThrows(PublishedContentException.class, task::execute);

        assertFalse(ex.getDocumentPaths().isEmpty());
        verify(folderNode, never()).remove();
    }

    @Test
    void getSetOperationProgress_roundtrips() {
        OperationProgress progress = mock(OperationProgress.class);
        FolderDeleteTask task = new FolderDeleteTask(session, folderNode);
        task.setOperationProgress(progress);
        assertSame(progress, task.getOperationProgress());
    }
}

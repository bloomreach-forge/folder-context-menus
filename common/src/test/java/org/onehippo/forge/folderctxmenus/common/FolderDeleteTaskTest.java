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

    // --- Helper factories ---

    private NodeIterator emptyIterator() {
        NodeIterator it = mock(NodeIterator.class);
        when(it.hasNext()).thenReturn(false);
        return it;
    }

    private NodeIterator iteratorOf(Node... nodes) {
        NodeIterator it = mock(NodeIterator.class);
        boolean[] consumed = {false};
        if (nodes.length == 0) {
            when(it.hasNext()).thenReturn(false);
        } else if (nodes.length == 1) {
            when(it.hasNext()).thenReturn(true, false);
            when(it.nextNode()).thenReturn(nodes[0]);
        } else {
            // Build return value chains for hasNext() and nextNode()
            Boolean[] hasNextValues = new Boolean[nodes.length + 1];
            for (int i = 0; i < nodes.length; i++) {
                hasNextValues[i] = true;
            }
            hasNextValues[nodes.length] = false;
            when(it.hasNext()).thenReturn(hasNextValues[0],
                    java.util.Arrays.copyOfRange(hasNextValues, 1, hasNextValues.length));
            when(it.nextNode()).thenReturn(nodes[0],
                    java.util.Arrays.copyOfRange(nodes, 1, nodes.length));
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

        Node variant = mock(Node.class);
        Property availProp = mock(Property.class);
        Value liveValue = mock(Value.class);
        when(liveValue.getString()).thenReturn("live");
        when(availProp.getValues()).thenReturn(new Value[]{liveValue});
        when(variant.hasProperty("hippo:availability")).thenReturn(true);
        when(variant.getProperty("hippo:availability")).thenReturn(availProp);
        when(handle.getNodes("article")).thenReturn(iteratorOf(variant));

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

        Node variant = mock(Node.class);
        Property availProp = mock(Property.class);
        // Empty availability = document is offline
        when(availProp.getValues()).thenReturn(new Value[0]);
        when(variant.hasProperty("hippo:availability")).thenReturn(true);
        when(variant.getProperty("hippo:availability")).thenReturn(availProp);
        when(handle.getNodes("offline")).thenReturn(iteratorOf(variant));

        return handle;
    }

    // --- Tests ---

    @Test
    void execute_emptyFolder_removesSourceNode() throws RepositoryException {
        when(folderNode.getNodes()).thenReturn(emptyIterator());

        FolderDeleteTask task = new FolderDeleteTask(session, folderNode);
        task.execute();

        verify(folderNode).remove();
    }

    @Test
    void execute_folderWithOfflineDocument_removesSourceNode() throws RepositoryException {
        Node handle = mockHandleWithOfflineVariant();
        when(folderNode.getNodes()).thenReturn(iteratorOf(handle));

        FolderDeleteTask task = new FolderDeleteTask(session, folderNode);
        task.execute();

        verify(folderNode).remove();
    }

    @Test
    void execute_folderWithLiveDocument_throwsPublishedContentException() throws RepositoryException {
        Node handle = mockHandleWithLiveVariant("/content/documents/site/myfolder/article");
        when(folderNode.getNodes()).thenReturn(iteratorOf(handle));

        FolderDeleteTask task = new FolderDeleteTask(session, folderNode);
        PublishedContentException ex = assertThrows(PublishedContentException.class, task::execute);

        assertTrue(ex.getDocumentPaths().contains("/content/documents/site/myfolder/article"));
        verify(folderNode, never()).remove();
    }

    @Test
    void execute_folderWithMultipleLiveDocs_collectsAllPaths() throws RepositoryException {
        Node handle1 = mockHandleWithLiveVariant("/content/documents/site/folder/doc1");
        Node handle2 = mockHandleWithLiveVariant("/content/documents/site/folder/doc2");
        when(folderNode.getNodes()).thenReturn(iteratorOf(handle1, handle2));

        FolderDeleteTask task = new FolderDeleteTask(session, folderNode);
        PublishedContentException ex = assertThrows(PublishedContentException.class, task::execute);

        assertEquals(2, ex.getDocumentPaths().size());
        verify(folderNode, never()).remove();
    }

    @Test
    void execute_withOperationProgress_updatesProgress() throws RepositoryException {
        when(folderNode.getNodes()).thenReturn(emptyIterator());
        OperationProgress progress = mock(OperationProgress.class);

        FolderDeleteTask task = new FolderDeleteTask(session, folderNode);
        task.setOperationProgress(progress);
        task.execute();

        verify(progress).updateProgress(0, 1, "Checking for published content...");
        verify(progress).updateProgress(1, 1, "Deleting folder...");
    }

    @Test
    void execute_withNoOperationProgress_doesNotThrow() throws RepositoryException {
        when(folderNode.getNodes()).thenReturn(emptyIterator());

        FolderDeleteTask task = new FolderDeleteTask(session, folderNode);
        assertDoesNotThrow(task::execute);
    }

    @Test
    void execute_nestedFolderWithLiveDoc_isDetected() throws RepositoryException {
        // Sub-folder containing a live document
        Node subFolder = mock(Node.class);
        when(subFolder.isNodeType(HippoNodeType.NT_HANDLE)).thenReturn(false);
        when(subFolder.isNodeType(HippoNodeType.NT_DOCUMENT)).thenReturn(false);
        when(subFolder.isNodeType("hippostd:folder")).thenReturn(true);

        Node handle = mockHandleWithLiveVariant("/content/documents/site/folder/sub/article");
        when(subFolder.getNodes()).thenReturn(iteratorOf(handle));
        when(folderNode.getNodes()).thenReturn(iteratorOf(subFolder));

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

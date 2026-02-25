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

import java.util.Locale;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoNodeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AbstractFolderTaskTest {

    private Session session;
    private Node sourceNode;

    @BeforeEach
    void setUp() throws RepositoryException {
        session = mock(Session.class);
        sourceNode = mock(Node.class);
        when(sourceNode.getPath()).thenReturn("/content/source");
    }

    private AbstractFolderTask taskWithNoOp(Locale locale) {
        return new AbstractFolderTask(session, locale, sourceNode) {
            @Override
            protected void doExecute() {
            }
        };
    }

    // --- Constructor / accessor tests ---

    @Test
    void constructor_withNullLocale_defaultsToSystemLocale() {
        AbstractFolderTask task = taskWithNoOp(null);
        assertEquals(Locale.getDefault(), task.getLocale());
    }

    @Test
    void constructor_withLocale_usesProvidedLocale() {
        AbstractFolderTask task = taskWithNoOp(Locale.FRENCH);
        assertEquals(Locale.FRENCH, task.getLocale());
    }

    @Test
    void getSession_returnsConstructorSession() {
        AbstractFolderTask task = taskWithNoOp(Locale.ENGLISH);
        assertSame(session, task.getSession());
    }

    @Test
    void getSourceFolderNode_returnsConstructorNode() {
        AbstractFolderTask task = taskWithNoOp(Locale.ENGLISH);
        assertSame(sourceNode, task.getSourceFolderNode());
    }

    // --- Logger tests ---

    @Test
    void getLogger_withNoCustomLogger_returnsDefaultLogger() {
        AbstractFolderTask task = taskWithNoOp(Locale.ENGLISH);
        assertSame(task.getDefaultLogger(), task.getLogger());
    }

    @Test
    void getLogger_afterSetLogger_returnsCustomLogger() {
        Logger customLogger = mock(Logger.class);
        AbstractFolderTask task = taskWithNoOp(Locale.ENGLISH);
        task.setLogger(customLogger);
        assertSame(customLogger, task.getLogger());
    }

    // --- execute() lifecycle tests ---

    @Test
    void execute_invokesLifecycleMethodsInOrder() throws RepositoryException {
        int[] order = {0};
        int[] before = {-1}, exec = {-1}, after = {-1};

        AbstractFolderTask task = new AbstractFolderTask(session, Locale.ENGLISH, sourceNode) {
            @Override
            protected void doBeforeExecute() {
                before[0] = order[0]++;
            }

            @Override
            protected void doExecute() {
                exec[0] = order[0]++;
            }

            @Override
            protected void doAfterExecute() {
                after[0] = order[0]++;
            }
        };

        task.execute();

        assertEquals(0, before[0]);
        assertEquals(1, exec[0]);
        assertEquals(2, after[0]);
    }

    @Test
    void execute_whenCancelled_runsDoAfterExecuteThenRethrows() throws RepositoryException {
        boolean[] afterCalled = {false};

        AbstractFolderTask task = new AbstractFolderTask(session, Locale.ENGLISH, sourceNode) {
            @Override
            protected void doExecute() throws RepositoryException {
                throw new OperationCancelledException("cancelled");
            }

            @Override
            protected void doAfterExecute() {
                afterCalled[0] = true;
            }
        };

        assertThrows(OperationCancelledException.class, task::execute);
        assertTrue(afterCalled[0], "doAfterExecute must be called on cancellation");
    }

    @Test
    void execute_whenNonCancelledRepositoryException_propagatesWithoutCallingDoAfterExecute() {
        boolean[] afterCalled = {false};

        AbstractFolderTask task = new AbstractFolderTask(session, Locale.ENGLISH, sourceNode) {
            @Override
            protected void doExecute() throws RepositoryException {
                throw new RepositoryException("some error");
            }

            @Override
            protected void doAfterExecute() {
                afterCalled[0] = true;
            }
        };

        assertThrows(RepositoryException.class, task::execute);
        assertFalse(afterCalled[0], "doAfterExecute must NOT be called for non-cancellation exceptions");
    }

    // --- updateFolderTranslations tests ---

    @Test
    void updateFolderTranslations_withBlankDisplayName_doesNotSetProperty() throws RepositoryException {
        Node folderNode = mock(Node.class);
        when(folderNode.getPath()).thenReturn("/content/folder");

        AbstractFolderTask task = taskWithNoOp(Locale.ENGLISH);
        task.updateFolderTranslations(folderNode, "");

        verify(folderNode, never()).setProperty(anyString(), anyString());
        verify(folderNode, never()).addMixin(anyString());
    }

    @Test
    void updateFolderTranslations_withNullDisplayName_doesNotSetProperty() throws RepositoryException {
        Node folderNode = mock(Node.class);
        when(folderNode.getPath()).thenReturn("/content/folder");

        AbstractFolderTask task = taskWithNoOp(Locale.ENGLISH);
        task.updateFolderTranslations(folderNode, null);

        verify(folderNode, never()).setProperty(anyString(), anyString());
    }

    @Test
    void updateFolderTranslations_withDisplayName_whenAlreadyNamed_setsPropertyWithoutAddingMixin()
            throws RepositoryException {
        Node folderNode = mock(Node.class);
        when(folderNode.getPath()).thenReturn("/content/folder");
        when(folderNode.isNodeType(HippoNodeType.NT_NAMED)).thenReturn(true);

        AbstractFolderTask task = taskWithNoOp(Locale.ENGLISH);
        task.updateFolderTranslations(folderNode, "My Folder");

        verify(folderNode, never()).addMixin(anyString());
        verify(folderNode).setProperty(HippoNodeType.HIPPO_NAME, "My Folder");
    }

    @Test
    void updateFolderTranslations_withDisplayName_whenNotNamed_addsMixinThenSetsProperty()
            throws RepositoryException {
        Node folderNode = mock(Node.class);
        when(folderNode.getPath()).thenReturn("/content/folder");
        when(folderNode.isNodeType(HippoNodeType.NT_NAMED)).thenReturn(false);

        AbstractFolderTask task = taskWithNoOp(Locale.ENGLISH);
        task.updateFolderTranslations(folderNode, "My Folder");

        verify(folderNode).addMixin(HippoNodeType.NT_NAMED);
        verify(folderNode).setProperty(HippoNodeType.HIPPO_NAME, "My Folder");
    }

    @Test
    void updateFolderTranslations_whenRepositoryExceptionThrown_swallowsException() throws RepositoryException {
        Node folderNode = mock(Node.class);
        when(folderNode.getPath()).thenThrow(new RepositoryException("repo error"));

        Logger logger = mock(Logger.class);
        AbstractFolderTask task = taskWithNoOp(Locale.ENGLISH);
        task.setLogger(logger);

        assertDoesNotThrow(() -> task.updateFolderTranslations(folderNode, "My Folder"));
    }
}

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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link AbstractFolderCopyOrMoveTask}:
 * <ul>
 *   <li>Getter contracts</li>
 *   <li>{@code maybeSaveAndRefreshPostProcessing} batch-save logic</li>
 *   <li>{@code PostProcessingContext} value-object construction</li>
 * </ul>
 */
class AbstractFolderCopyOrMoveTaskTest {

    private Session session;
    private Node sourceNode;
    private Node destParentNode;

    @BeforeEach
    void setUp() throws RepositoryException {
        session = mock(Session.class);
        sourceNode = mock(Node.class);
        destParentNode = mock(Node.class);
        when(sourceNode.getPath()).thenReturn("/content/source");
        when(destParentNode.getPath()).thenReturn("/content/archive");
    }

    // ---- minimal concrete subclass ----

    private AbstractFolderCopyOrMoveTask task(boolean resetTranslations) {
        return new AbstractFolderCopyOrMoveTask(
                session, Locale.ENGLISH, sourceNode, destParentNode, "dest-folder", "Dest Folder", resetTranslations) {
            @Override
            protected String getOperationName() {
                return "TestOp";
            }

            @Override
            protected void doExecute() {
                // no-op
            }
        };
    }

    // ---- getter contracts ----

    @Test
    void getDestParentFolderNode_returnsConstructorValue() {
        assertSame(destParentNode, task(false).getDestParentFolderNode());
    }

    @Test
    void getDestFolderNodeName_returnsConstructorValue() {
        assertEquals("dest-folder", task(false).getDestFolderNodeName());
    }

    @Test
    void getDestFolderDisplayName_returnsConstructorValue() {
        assertEquals("Dest Folder", task(false).getDestFolderDisplayName());
    }

    @Test
    void getResetTranslations_trueWhenConstructedWithTrue() {
        assertTrue(task(true).getResetTranslations());
    }

    @Test
    void getResetTranslations_falseWhenConstructedWithFalse() {
        assertFalse(task(false).getResetTranslations());
    }

    @Test
    void getDestFolderPath_combinesParentPathAndNodeName() throws RepositoryException {
        assertEquals("/content/archive/dest-folder", task(false).getDestFolderPath());
    }

    @Test
    void getDestFolderNode_initiallyNull() {
        assertNull(task(false).getDestFolderNode());
    }

    @Test
    void setDestFolderNode_thenGetDestFolderNode_returnsSameNode() throws RepositoryException {
        AbstractFolderCopyOrMoveTask t = task(false);
        Node dest = mock(Node.class);
        t.setDestFolderNode(dest);
        assertSame(dest, t.getDestFolderNode());
    }

    @Test
    void setOperationProgress_thenGetOperationProgress_returnsSame() {
        AbstractFolderCopyOrMoveTask t = task(false);
        OperationProgress progress = mock(OperationProgress.class);
        t.setOperationProgress(progress);
        assertSame(progress, t.getOperationProgress());
    }

    // ---- twoArg constructor defaults resetTranslations=false ----

    @Test
    void twoArgConstructor_setsResetTranslationsFalse() {
        AbstractFolderCopyOrMoveTask t = new AbstractFolderCopyOrMoveTask(
                session, Locale.ENGLISH, sourceNode, destParentNode, "dest", "Dest") {
            @Override
            protected String getOperationName() {
                return "Noop";
            }

            @Override
            protected void doExecute() {
            }
        };
        assertFalse(t.getResetTranslations());
    }

    // ---- maybeSaveAndRefreshPostProcessing ----

    @Test
    void maybeSaveAndRefreshPostProcessing_atBatchBoundary_savesAndRefreshes() throws RepositoryException {
        AbstractFolderCopyOrMoveTask t = task(false);
        Node node = mock(Node.class);
        when(node.getSession()).thenReturn(session);

        t.maybeSaveAndRefreshPostProcessing(node, 200); // default batch size = 200

        verify(session).save();
        verify(session).refresh(false);
    }

    @Test
    void maybeSaveAndRefreshPostProcessing_notAtBoundary_doesNotSave() throws RepositoryException {
        AbstractFolderCopyOrMoveTask t = task(false);
        Node node = mock(Node.class);
        when(node.getSession()).thenReturn(session);

        t.maybeSaveAndRefreshPostProcessing(node, 199);

        verify(session, never()).save();
        verify(session, never()).refresh(anyBoolean());
    }

    @Test
    void maybeSaveAndRefreshPostProcessing_atDoubledBoundary_savesAgain() throws RepositoryException {
        AbstractFolderCopyOrMoveTask t = task(false);
        Node node = mock(Node.class);
        when(node.getSession()).thenReturn(session);

        t.maybeSaveAndRefreshPostProcessing(node, 400);

        verify(session).save();
        verify(session).refresh(false);
    }

    @Test
    void maybeSaveAndRefreshPostProcessing_atCountOne_doesNotSave() throws RepositoryException {
        AbstractFolderCopyOrMoveTask t = task(false);
        Node node = mock(Node.class);
        when(node.getSession()).thenReturn(session);

        t.maybeSaveAndRefreshPostProcessing(node, 1);

        verify(session, never()).save();
    }

    // ---- PostProcessingContext ----

    @Test
    void postProcessingContext_storesAllFields() {
        Map<String, String> uuidMappings = new HashMap<>();
        AbstractFolderCopyOrMoveTask.PostProcessingContext ctx =
                new AbstractFolderCopyOrMoveTask.PostProcessingContext(uuidMappings, "en", true, false);

        assertSame(uuidMappings, ctx.uuidMappings);
        assertEquals("en", ctx.parentLocale);
        assertTrue(ctx.resetTranslationIds);
        assertFalse(ctx.recomputePaths);
    }

    @Test
    void postProcessingContext_nullParentLocaleAllowed() {
        AbstractFolderCopyOrMoveTask.PostProcessingContext ctx =
                new AbstractFolderCopyOrMoveTask.PostProcessingContext(new HashMap<>(), null, false, true);

        assertNull(ctx.parentLocale);
        assertFalse(ctx.resetTranslationIds);
        assertTrue(ctx.recomputePaths);
    }
}

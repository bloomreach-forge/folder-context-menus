/*
 * Copyright 2026 Bloomreach (https://www.bloomreach.com)
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
import java.util.UUID;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.FolderWorkflowImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.onehippo.repository.security.StandardPermissionNames.HIPPO_AUTHOR;

/**
 * Tests for {@link ExtendedFolderWorkflowImpl}, covering the FORGE-676 regression: copy/move must run
 * through this workflow so that (a) the {@code HIPPO_AUTHOR} permission gate is checked on the
 * <em>user</em> session, and (b) the actual write happens on the elevated {@code rootSession} rather
 * than the caller's own session. Also covers the {@link OperationProgressRegistry} bridge used to
 * carry a live {@link OperationProgress} across the workflow invocation.
 * <p>
 * No live repository is used: {@code brut}'s {@code BrxmTestingRepository} has no Hippo namespaces or
 * node types registered (confirmed separately) and its access manager always grants permission, so it
 * cannot exercise either behaviour above. Session/Node/WorkflowContext are mocked instead, following
 * the pattern already used by {@link AbstractFolderTaskTest} and {@link JcrCopyUtilsTest}.
 */
class ExtendedFolderWorkflowImplTest {

    private WorkflowContext context;
    private Session userSession;
    private Session rootSession;
    private Node subject;
    private ExtendedFolderWorkflowImpl workflow;

    private Node sourceNode;
    private Node destParentNode;

    @BeforeEach
    void setUp() throws Exception {
        context = mock(WorkflowContext.class);
        userSession = mock(Session.class);
        rootSession = mock(Session.class);
        subject = mock(Node.class);
        when(subject.getIdentifier()).thenReturn("subject-id");
        when(rootSession.getNodeByIdentifier("subject-id")).thenReturn(subject);

        workflow = new ExtendedFolderWorkflowImpl(context, userSession, rootSession, subject);
    }

    private void stubSource(String sourceId) throws RepositoryException {
        sourceNode = mock(Node.class);
        when(rootSession.getNodeByIdentifier(sourceId)).thenReturn(sourceNode);
    }

    private void stubDestination(String destId, boolean isFolderType, boolean hasAuthorPermission) throws RepositoryException {
        destParentNode = mock(Node.class);
        when(rootSession.getNodeByIdentifier(destId)).thenReturn(destParentNode);
        when(destParentNode.isNodeType(HippoStdNodeType.NT_FOLDER)).thenReturn(isFolderType);
        when(destParentNode.getPath()).thenReturn("/content/dest");
        when(userSession.hasPermission("/content/dest", HIPPO_AUTHOR)).thenReturn(hasAuthorPermission);
    }

    // ---- destination-not-a-folder guard: must reject before checking permission or writing ----

    @Test
    void moveFolder_whenDestinationNotFolderType_rejectsWithoutCheckingPermissionOrWriting() throws Exception {
        stubSource("src-1");
        stubDestination("dest-1", false, true);

        WorkflowException ex = assertThrows(WorkflowException.class,
                () -> workflow.moveFolder(Locale.ENGLISH, "src-1", "dest-1", "moved", "Moved"));

        assertEquals("Cannot move folder; Destination is not folder", ex.getMessage());
        verify(userSession, never()).hasPermission(anyString(), anyString());
        verify(rootSession, never()).move(anyString(), anyString());
        verify(rootSession, never()).save();
    }

    @Test
    void copyFolder_whenDestinationNotFolderType_rejectsWithoutCheckingPermissionOrWriting() throws Exception {
        stubSource("src-1");
        stubDestination("dest-1", false, true);

        WorkflowException ex = assertThrows(WorkflowException.class,
                () -> workflow.copyFolder(Locale.ENGLISH, "src-1", "dest-1", "copied", "Copied", false));

        assertEquals("Cannot copy folder; Destination is not folder", ex.getMessage());
        verify(userSession, never()).hasPermission(anyString(), anyString());
        verify(rootSession, never()).save();
    }

    // ---- HIPPO_AUTHOR permission guard: this is the exact FORGE-676 regression check ----
    // The permission must be checked on userSession (the caller's own, non-elevated session), and a
    // denial must prevent any write from happening on rootSession.

    @Test
    void moveFolder_whenUserLacksAuthorPermission_rejectsWithoutWriting() throws Exception {
        stubSource("src-2");
        stubDestination("dest-2", true, false);

        WorkflowException ex = assertThrows(WorkflowException.class,
                () -> workflow.moveFolder(Locale.ENGLISH, "src-2", "dest-2", "moved", "Moved"));

        assertEquals(FolderWorkflowImpl.USER_LACKS_AUTHOR_PERMISSION_IN_DESTINATION_FOLDER, ex.getMessage());
        verify(userSession).hasPermission("/content/dest", HIPPO_AUTHOR);
        verify(rootSession, never()).move(anyString(), anyString());
        verify(rootSession, never()).save();
    }

    @Test
    void copyFolder_whenUserLacksAuthorPermission_rejectsWithoutWriting() throws Exception {
        stubSource("src-2");
        stubDestination("dest-2", true, false);

        WorkflowException ex = assertThrows(WorkflowException.class,
                () -> workflow.copyFolder(Locale.ENGLISH, "src-2", "dest-2", "copied", "Copied", false));

        assertEquals(FolderWorkflowImpl.USER_LACKS_AUTHOR_PERMISSION_IN_DESTINATION_FOLDER, ex.getMessage());
        verify(userSession).hasPermission("/content/dest", HIPPO_AUTHOR);
        verify(rootSession, never()).save();
    }

    // ---- Successful move: proves the write goes through rootSession, not userSession ----
    // This is the direct proof of the fix: before FORGE-676 was fixed, the write ran on a
    // same-privilege session and failed for non-admins. Here the write must happen on rootSession
    // regardless of what userSession is capable of.

    @Test
    void moveFolder_whenAuthorized_movesViaRootSessionAndNeverTouchesUserSession() throws Exception {
        stubSource("src-3");
        stubDestination("dest-3", true, true);

        Node sourceParent = mock(Node.class);
        when(sourceNode.getParent()).thenReturn(sourceParent);
        when(sourceNode.getPath()).thenReturn("/content/source");

        Node movedNode = mock(Node.class);
        when(destParentNode.hasNode("moved")).thenReturn(true);
        when(destParentNode.getNode("moved")).thenReturn(movedNode);
        when(movedNode.getParent()).thenReturn(destParentNode);

        // blank display name skips the translation-naming branch entirely (not under test here)
        workflow.moveFolder(Locale.ENGLISH, "src-3", "dest-3", "moved", "");

        verify(rootSession).move("/content/source", "/content/dest/moved");
        verify(rootSession).save();
        verify(rootSession).refresh(false);
        verify(userSession, never()).move(anyString(), anyString());
        verify(userSession, never()).save();
    }

    // ---- OperationProgressRegistry wiring: proves operationId correctly bridges the live progress
    // object into the task, and that cancellation stops the operation before any write. ----

    @Test
    void moveFolder_whenRegisteredProgressIsCancelled_stopsBeforeWritingAndWrapsException() throws Exception {
        stubSource("src-4");
        stubDestination("dest-4", true, true);

        // FolderMoveTask.doExecute() checks source-vs-destination relationships before anything else.
        Node sourceParent = mock(Node.class);
        when(sourceNode.getParent()).thenReturn(sourceParent);
        when(sourceNode.getPath()).thenReturn("/content/source");

        OperationProgress progress = mock(OperationProgress.class);
        when(progress.isCancelled()).thenReturn(true);

        String operationId = "op-" + UUID.randomUUID();
        OperationProgressRegistry.register(operationId, progress);
        try {
            WorkflowException ex = assertThrows(WorkflowException.class,
                    () -> workflow.moveFolder(Locale.ENGLISH, "src-4", "dest-4", "moved", "Moved", operationId));

            assertEquals("Folder move operation was cancelled", ex.getMessage());
            assertInstanceOf(OperationCancelledException.class, ex.getCause());
            verify(rootSession, never()).move(anyString(), anyString());
            verify(rootSession, never()).save();
            verify(rootSession).refresh(false);
        } finally {
            OperationProgressRegistry.unregister(operationId);
        }
    }

    @Test
    void copyFolder_whenRegisteredProgressIsCancelled_stopsBeforeWritingAndWrapsException() throws Exception {
        stubSource("src-4");
        stubDestination("dest-4", true, true);

        Node sourceParent = mock(Node.class);
        when(sourceNode.getParent()).thenReturn(sourceParent);
        when(sourceNode.getPath()).thenReturn("/content/source");
        // Matches JcrCopyUtilsTest's pattern for a destination with no ancestor relationship to source.
        when(destParentNode.getParent()).thenThrow(new ItemNotFoundException("root has no parent"));
        when(destParentNode.isCheckedOut()).thenReturn(true);
        // ProgressTrackingCopyHandler's constructor (DefaultCopyHandler) reads the destination
        // parent's own node types and workspace before any cancellation check runs; without these
        // stubs Mockito returns null for the array/object getters, causing an unrelated NPE.
        when(destParentNode.getMixinNodeTypes()).thenReturn(new NodeType[0]);
        when(destParentNode.getPrimaryNodeType()).thenReturn(mock(NodeType.class));
        Workspace destWorkspace = mock(Workspace.class);
        when(destParentNode.getSession()).thenReturn(rootSession);
        when(rootSession.getWorkspace()).thenReturn(destWorkspace);
        when(destWorkspace.getNodeTypeManager()).thenReturn(mock(NodeTypeManager.class));

        OperationProgress progress = mock(OperationProgress.class);
        when(progress.isCancelled()).thenReturn(true);

        String operationId = "op-" + UUID.randomUUID();
        OperationProgressRegistry.register(operationId, progress);
        try {
            WorkflowException ex = assertThrows(WorkflowException.class,
                    () -> workflow.copyFolder(Locale.ENGLISH, "src-4", "dest-4", "copied", "Copied", false, operationId));

            assertEquals("Folder copy operation was cancelled", ex.getMessage());
            assertInstanceOf(OperationCancelledException.class, ex.getCause());
            verify(rootSession, never()).save();
            verify(rootSession).refresh(false);
        } finally {
            OperationProgressRegistry.unregister(operationId);
        }
    }

    // ---- Backward compatibility: the pre-existing overloads (without operationId) must behave
    // exactly as calling the new overload with a null operationId. ----

    @Test
    void moveFolder_legacyOverloadWithoutOperationId_behavesSameAsExplicitNull() throws Exception {
        stubSource("src-5");
        stubDestination("dest-5", true, false);

        WorkflowException ex = assertThrows(WorkflowException.class,
                () -> workflow.moveFolder(Locale.ENGLISH, "src-5", "dest-5", "moved", "Moved"));

        assertEquals(FolderWorkflowImpl.USER_LACKS_AUTHOR_PERMISSION_IN_DESTINATION_FOLDER, ex.getMessage());
    }

    @Test
    void copyFolder_legacyOverloadWithoutOperationId_behavesSameAsExplicitNull() throws Exception {
        stubSource("src-5");
        stubDestination("dest-5", true, false);

        WorkflowException ex = assertThrows(WorkflowException.class,
                () -> workflow.copyFolder(Locale.ENGLISH, "src-5", "dest-5", "copied", "Copied", false));

        assertEquals(FolderWorkflowImpl.USER_LACKS_AUTHOR_PERMISSION_IN_DESTINATION_FOLDER, ex.getMessage());
    }

    // ---- refreshRootSession() runs even when the whole call succeeds ----

    @Test
    void moveFolder_alwaysRefreshesRootSessionEvenWhenRejected() throws Exception {
        stubSource("src-6");
        stubDestination("dest-6", true, false);

        assertThrows(WorkflowException.class,
                () -> workflow.moveFolder(Locale.ENGLISH, "src-6", "dest-6", "moved", "Moved"));

        verify(rootSession).refresh(false);
    }
}

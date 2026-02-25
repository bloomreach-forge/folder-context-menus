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
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.InOrder;

class ProgressTrackingCopyHandlerTest {

    private static final int SAVE_BATCH_SIZE = 200;

    private Node destParentNode;
    private Session session;

    @BeforeEach
    void setUp() throws RepositoryException {
        session = mock(Session.class);
        Workspace workspace = mock(Workspace.class);
        NodeTypeManager nodeTypeManager = mock(NodeTypeManager.class);
        NodeType primaryNodeType = mock(NodeType.class);

        destParentNode = mock(Node.class);
        // DefaultCopyHandler constructor: JcrUtils.ensureIsCheckedOut(node) stops recursing when true
        when(destParentNode.isCheckedOut()).thenReturn(true);
        // DefaultCopyHandler constructor: nodeTypeManager = node.getSession().getWorkspace().getNodeTypeManager()
        when(destParentNode.getSession()).thenReturn(session);
        when(session.getWorkspace()).thenReturn(workspace);
        when(workspace.getNodeTypeManager()).thenReturn(nodeTypeManager);
        // DefaultCopyHandler.setCurrent(node): JcrUtils.getMixinNodeTypes / getPrimaryNodeType
        when(destParentNode.getMixinNodeTypes()).thenReturn(new NodeType[0]);
        when(destParentNode.getPrimaryNodeType()).thenReturn(primaryNodeType);
    }

    private ProgressTrackingCopyHandler handlerWithCount(long count) throws RepositoryException {
        AtomicLong counter = new AtomicLong(count);
        return new ProgressTrackingCopyHandler(destParentNode, null, counter, 1000);
    }

    @Test
    void maybeSaveIncrementally_whenCounterIsExactBatchMultiple_savesAndRefreshesSession() throws RepositoryException {
        ProgressTrackingCopyHandler handler = handlerWithCount(SAVE_BATCH_SIZE);

        handler.maybeSaveIncrementally();

        InOrder ordered = inOrder(session);
        ordered.verify(session).save();
        ordered.verify(session).refresh(false);
    }

    @Test
    void maybeSaveIncrementally_whenCounterIsSecondBatchMultiple_savesAndRefreshesSession() throws RepositoryException {
        ProgressTrackingCopyHandler handler = handlerWithCount(SAVE_BATCH_SIZE * 2L);

        handler.maybeSaveIncrementally();

        InOrder ordered = inOrder(session);
        ordered.verify(session).save();
        ordered.verify(session).refresh(false);
    }

    @Test
    void maybeSaveIncrementally_whenCounterIsNotBatchMultiple_doesNotSave() throws RepositoryException {
        ProgressTrackingCopyHandler handler = handlerWithCount(SAVE_BATCH_SIZE - 1);

        handler.maybeSaveIncrementally();

        verify(session, never()).save();
    }

    @Test
    void maybeSaveIncrementally_whenCounterIsZero_doesNotSave() throws RepositoryException {
        ProgressTrackingCopyHandler handler = handlerWithCount(0);

        handler.maybeSaveIncrementally();

        verify(session, never()).save();
    }

    @Test
    void maybeSaveIncrementally_whenCounterIsOne_doesNotSave() throws RepositoryException {
        ProgressTrackingCopyHandler handler = handlerWithCount(1);

        handler.maybeSaveIncrementally();

        verify(session, never()).save();
    }

    /**
     * Regression: previously maybeSaveIncrementally() was called inside startNode(), which fired
     * session.save() before mandatory properties of the new node had been copied, causing
     * ConstraintViolationException. The fix moves the save to endNode(); this test verifies that
     * endNode() triggers a save (via maybeSaveIncrementally) when the counter is at a boundary,
     * using the same getCurrent() path as production code.
     */
    @Test
    void endNode_atBatchBoundary_savesBeforePoppingNode() throws RepositoryException {
        // handlerWithCount(SAVE_BATCH_SIZE) pre-loads the counter at the boundary.
        // getCurrent() returns destParentNode (set in DefaultCopyHandler constructor).
        ProgressTrackingCopyHandler handler = handlerWithCount(SAVE_BATCH_SIZE);

        handler.endNode();

        InOrder ordered = inOrder(session);
        ordered.verify(session).save();
        ordered.verify(session).refresh(false);
    }

    @Test
    void endNode_notAtBatchBoundary_doesNotSave() throws RepositoryException {
        ProgressTrackingCopyHandler handler = handlerWithCount(SAVE_BATCH_SIZE - 1);

        handler.endNode();

        verify(session, never()).save();
    }
}

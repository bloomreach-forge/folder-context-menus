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

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.util.CopyHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for the validation branches of JcrCopyUtils.
 * <p>
 * Note: JcrUtils.isVirtual() internally casts to HippoNode, so srcNode is mocked as HippoNode
 * (which extends Node) so that the cast succeeds and the stub on isVirtual() is honoured.
 * By default Mockito returns false for boolean methods, so isVirtual() = false without
 * explicit stubbing — making the node "not virtual" and allowing the subsequent validations
 * to be reached.
 */
class JcrCopyUtilsTest {

    // ---- copy(Node, String, Node, CopyHandler) ----

    @Test
    void copy_withHandler_whenNullHandler_throwsIllegalArgument() throws RepositoryException {
        HippoNode src = mock(HippoNode.class);
        Node dest = mock(Node.class);

        assertThrows(IllegalArgumentException.class,
                () -> JcrCopyUtils.copy(src, "dest", dest, (CopyHandler) null));
    }

    @Test
    void copy_withHandler_whenVirtualNode_returnsNull() throws RepositoryException {
        HippoNode src = mock(HippoNode.class);
        Node dest = mock(Node.class);
        CopyHandler handler = mock(CopyHandler.class);
        when(src.isVirtual()).thenReturn(true);

        Node result = JcrCopyUtils.copy(src, "dest", dest, handler);

        assertNull(result);
    }

    @Test
    void copy_withHandler_whenDestNameContainsSlash_throwsIllegalArgument() throws RepositoryException {
        HippoNode src = mock(HippoNode.class);
        Node dest = mock(Node.class);
        CopyHandler handler = mock(CopyHandler.class);

        assertThrows(IllegalArgumentException.class,
                () -> JcrCopyUtils.copy(src, "parent/child", dest, handler));
    }

    @Test
    void copy_withHandler_whenSrcSameAsDest_throwsIllegalArgument() throws RepositoryException {
        HippoNode src = mock(HippoNode.class);
        CopyHandler handler = mock(CopyHandler.class);
        when(src.isSame(src)).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> JcrCopyUtils.copy(src, "dest", src, handler));
    }

    @Test
    void copy_withHandler_whenDestIsDescendantOfSrc_throwsIllegalArgument() throws RepositoryException {
        HippoNode src = mock(HippoNode.class);
        HippoNode destParent = mock(HippoNode.class);
        Node intermediate = mock(Node.class);
        CopyHandler handler = mock(CopyHandler.class);

        // destParent is NOT the same as src
        when(src.isSame(destParent)).thenReturn(false);
        // but destParent's parent IS src → destParent is a descendant of src
        when(destParent.getParent()).thenReturn(intermediate);
        when(src.isSame(intermediate)).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> JcrCopyUtils.copy(src, "dest", destParent, handler));
    }

    // ---- copy(Node, String, Node, OperationProgress, long) ----

    @Test
    void copy_withProgress_whenVirtualNode_returnsNull() throws RepositoryException {
        HippoNode src = mock(HippoNode.class);
        Node dest = mock(Node.class);
        OperationProgress progress = mock(OperationProgress.class);
        when(src.isVirtual()).thenReturn(true);

        Node result = JcrCopyUtils.copy(src, "dest", dest, progress, 10);

        assertNull(result);
    }

    @Test
    void copy_withProgress_whenDestNameContainsSlash_throwsIllegalArgument() throws RepositoryException {
        HippoNode src = mock(HippoNode.class);
        Node dest = mock(Node.class);
        OperationProgress progress = mock(OperationProgress.class);

        assertThrows(IllegalArgumentException.class,
                () -> JcrCopyUtils.copy(src, "parent/child", dest, progress, 10));
    }

    @Test
    void copy_withProgress_whenSrcSameAsDest_throwsIllegalArgument() throws RepositoryException {
        HippoNode src = mock(HippoNode.class);
        OperationProgress progress = mock(OperationProgress.class);
        when(src.isSame(src)).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> JcrCopyUtils.copy(src, "dest", src, progress, 10));
    }

    @Test
    void copy_withProgress_whenDestIsDescendantOfSrc_throwsIllegalArgument() throws RepositoryException {
        HippoNode src = mock(HippoNode.class);
        HippoNode destParent = mock(HippoNode.class);
        Node intermediate = mock(Node.class);
        OperationProgress progress = mock(OperationProgress.class);

        when(src.isSame(destParent)).thenReturn(false);
        when(destParent.getParent()).thenReturn(intermediate);
        when(src.isSame(intermediate)).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> JcrCopyUtils.copy(src, "dest", destParent, progress, 10));
    }

    // ---- isAncestor edge: ItemNotFoundException means not an ancestor ----

    @Test
    void copy_withHandler_whenDestParentHasNoParent_noAncestorExceptionThrown() throws RepositoryException {
        HippoNode src = mock(HippoNode.class);
        HippoNode destParent = mock(HippoNode.class);
        CopyHandler handler = mock(CopyHandler.class);

        when(src.isSame(destParent)).thenReturn(false);
        // getParent() throws ItemNotFoundException → dest is not a descendant of src
        when(destParent.getParent()).thenThrow(new ItemNotFoundException("root has no parent"));

        // The "ancestor" check passes (returns false), so no IAE from that guard.
        // The actual copy operation will likely fail due to incomplete mock setup,
        // but the important thing is we do NOT get an IAE from the ancestor check.
        assertThrows(Exception.class,
                () -> JcrCopyUtils.copy(src, "dest", destParent, handler),
                "An exception is expected (from the copy itself) but NOT an IAE about ancestor");

        // Explicitly: ensure it is not the "ancestor" IAE
        try {
            JcrCopyUtils.copy(src, "dest", destParent, handler);
        } catch (IllegalArgumentException e) {
            assertFalse(e.getMessage().contains("descendant"),
                    "Should not throw the ancestor IAE when dest is not a descendant");
        } catch (Exception ignored) {
            // Any other exception is acceptable — it means we passed the ancestor check
        }
    }
}

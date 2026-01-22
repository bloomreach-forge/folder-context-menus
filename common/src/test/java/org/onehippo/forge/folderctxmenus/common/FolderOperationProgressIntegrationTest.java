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
import javax.jcr.SimpleCredentials;
import javax.jcr.nodetype.NodeType;

import org.bloomreach.forge.brut.common.repository.BrxmTestingRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FolderOperationProgressIntegrationTest {

    private BrxmTestingRepository repository;
    private Session session;
    private Node sourceFolderNode;
    private Node destParentFolderNode;
    private TestOperationProgress progress;

    @BeforeEach
    public void setUp() throws Exception {
        repository = new BrxmTestingRepository();
        session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));

        Node root = session.getRootNode();

        if (!root.hasNode("content")) {
            root.addNode("content", NodeType.NT_FOLDER);
        }
        Node contentNode = root.getNode("content");

        if (!contentNode.hasNode("documents")) {
            contentNode.addNode("documents", NodeType.NT_FOLDER);
        }
        Node documentsNode = contentNode.getNode("documents");

        if (documentsNode.hasNode("testfolder")) {
            documentsNode.getNode("testfolder").remove();
        }
        sourceFolderNode = documentsNode.addNode("testfolder", NodeType.NT_FOLDER);

        sourceFolderNode.addNode("child1", NodeType.NT_FOLDER);
        sourceFolderNode.addNode("child2", NodeType.NT_FOLDER);
        sourceFolderNode.getNode("child1").addNode("grandchild", NodeType.NT_FOLDER);

        if (documentsNode.hasNode("archive")) {
            documentsNode.getNode("archive").remove();
        }
        destParentFolderNode = documentsNode.addNode("archive", NodeType.NT_FOLDER);

        session.save();

        progress = new TestOperationProgress();
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (session != null) {
            session.logout();
        }
        if (repository != null) {
            repository.close();
        }
    }

    @Test
    public void testCopyFolderWithProgress() throws RepositoryException {
        FolderCopyTask task = new FolderCopyTask(
                session,
                Locale.ENGLISH,
                sourceFolderNode,
                destParentFolderNode,
                "testfolder-copy",
                "Test Folder Copy",
                false
        );
        task.setOperationProgress(progress);

        task.execute();

        assertTrue(progress.updateCount > 0, "Progress should have been updated");
        assertTrue(progress.lastTotal > 0, "Total should be greater than zero");
        assertFalse(progress.cancelled, "Should not be cancelled");

        assertTrue(destParentFolderNode.hasNode("testfolder-copy"), "Copied folder should exist");
        Node copiedFolder = destParentFolderNode.getNode("testfolder-copy");
        assertTrue(copiedFolder.hasNode("child1"), "Copied folder should have children");
    }

    @Test
    public void testMoveFolderWithProgress() throws RepositoryException {
        FolderMoveTask task = new FolderMoveTask(
                session,
                Locale.ENGLISH,
                sourceFolderNode,
                destParentFolderNode,
                "testfolder-moved",
                "Test Folder Moved"
        );
        task.setOperationProgress(progress);

        String originalPath = sourceFolderNode.getPath();
        task.execute();

        assertTrue(progress.updateCount > 0, "Progress should have been updated");
        assertTrue(progress.lastTotal > 0, "Total should be greater than zero");
        assertFalse(progress.cancelled, "Should not be cancelled");

        assertFalse(session.nodeExists(originalPath), "Original folder should not exist");
        assertTrue(destParentFolderNode.hasNode("testfolder-moved"), "Moved folder should exist");
        Node movedFolder = destParentFolderNode.getNode("testfolder-moved");
        assertTrue(movedFolder.hasNode("child1"), "Moved folder should have children");
    }

    @Test
    public void testCopyFolderWithCancellation() throws RepositoryException {
        CancellableOperationProgress cancellableProgress = new CancellableOperationProgress(2);

        FolderCopyTask task = new FolderCopyTask(
                session,
                Locale.ENGLISH,
                sourceFolderNode,
                destParentFolderNode,
                "testfolder-cancelled",
                "Test Folder Cancelled",
                false
        );
        task.setOperationProgress(cancellableProgress);

        assertThrows(OperationCancelledException.class, task::execute, "Should have thrown OperationCancelledException");
        assertTrue(cancellableProgress.cancelled, "Should have been cancelled");
        assertTrue(cancellableProgress.updateCount > 0, "Should have recorded some progress");
    }

    @Test
    public void testProgressReportsAccurateCount() throws RepositoryException {
        FolderCopyTask task = new FolderCopyTask(
                session,
                Locale.ENGLISH,
                sourceFolderNode,
                destParentFolderNode,
                "testfolder-accurate",
                "Test Folder Accurate",
                false
        );
        task.setOperationProgress(progress);

        task.execute();

        assertTrue(progress.lastCurrent > 0, "Current count should progress");
        assertTrue(progress.lastCurrent <= progress.lastTotal, "Current should not exceed total");
        assertNotNull(progress.lastPath, "Should have recorded last path");
    }

    private static class TestOperationProgress implements OperationProgress {
        int updateCount = 0;
        long lastCurrent = 0;
        long lastTotal = 0;
        String lastPath = null;
        boolean cancelled = false;

        @Override
        public void updateProgress(long current, long total, String currentItemPath) {
            updateCount++;
            lastCurrent = current;
            lastTotal = total;
            lastPath = currentItemPath;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }
    }

    private static class CancellableOperationProgress extends TestOperationProgress {
        private final int cancelAfterUpdates;

        CancellableOperationProgress(int cancelAfterUpdates) {
            this.cancelAfterUpdates = cancelAfterUpdates;
        }

        @Override
        public void updateProgress(long current, long total, String currentItemPath) {
            super.updateProgress(current, total, currentItemPath);
            if (updateCount >= cancelAfterUpdates) {
                cancelled = true;
            }
        }
    }

}

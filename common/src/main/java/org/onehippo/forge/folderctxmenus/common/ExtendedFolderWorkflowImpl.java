/*
 * Copyright 2020 Bloomreach (http://www.bloomreach.com)
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

import static org.onehippo.repository.security.StandardPermissionNames.HIPPO_AUTHOR;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.FolderWorkflowImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.rmi.RemoteException;
import java.util.Locale;

public class ExtendedFolderWorkflowImpl extends FolderWorkflowImpl implements ExtendedFolderWorkflow {

    private static final Logger log = LoggerFactory.getLogger(ExtendedFolderWorkflowImpl.class);

    private final Session rootSession;
    private final Session userSession;

    public ExtendedFolderWorkflowImpl(WorkflowContext context, Session userSession, Session rootSession, Node subject) throws RemoteException, RepositoryException {
        super(context, userSession, rootSession, subject);
        this.rootSession = rootSession;
        this.userSession = userSession;
    }

    public void copyFolder(final Locale locale,
                           final String sourceFolderId,
                           final String destParentFolderId,
                           final String destFolderNodeName,
                           final String destFolderDisplayName) throws WorkflowException {
        try {
            final Node sourceFolderNode = rootSession.getNodeByIdentifier(sourceFolderId);
            final Node destParentFolderNode = rootSession.getNodeByIdentifier(destParentFolderId);

            if (!destParentFolderNode.isNodeType(HippoStdNodeType.NT_FOLDER)) {
                throw new WorkflowException("Cannot copy folder; Destination is not folder");
            }

            if (!userSession.hasPermission(destParentFolderNode.getPath(), HIPPO_AUTHOR)) {
                throw new WorkflowException(FolderWorkflowImpl.USER_LACKS_AUTHOR_PERMISSION_IN_DESTINATION_FOLDER);
            }

            final FolderCopyTask task = new FolderCopyTask(
                    rootSession,
                    locale,
                    sourceFolderNode,
                    destParentFolderNode,
                    destFolderNodeName,
                    destFolderDisplayName);
            task.execute();
            rootSession.save();
        } catch (final RepositoryException e) {
            log.error("Error while copying folder", e);
            throw new WorkflowException("Unable to copy folder");
        } finally {
            refreshRootSession();
        }
    }

    public void moveFolder(final Locale locale,
                           final String sourceFolderId,
                           final String destParentFolderId,
                           final String destFolderNodeName,
                           final String destFolderDisplayName) throws WorkflowException {
        try {
            final Node sourceFolderNode = rootSession.getNodeByIdentifier(sourceFolderId);
            final Node destParentFolderNode = rootSession.getNodeByIdentifier(destParentFolderId);

            if (!destParentFolderNode.isNodeType(HippoStdNodeType.NT_FOLDER)) {
                throw new WorkflowException("Cannot move folder; Destination is not folder");
            }

            if (!userSession.hasPermission(destParentFolderNode.getPath(), HIPPO_AUTHOR)) {
                throw new WorkflowException(FolderWorkflowImpl.USER_LACKS_AUTHOR_PERMISSION_IN_DESTINATION_FOLDER);
            }

            final FolderMoveTask task = new FolderMoveTask(
                    rootSession,
                    locale,
                    sourceFolderNode,
                    destParentFolderNode,
                    destFolderNodeName,
                    destFolderDisplayName);
            task.execute();
            rootSession.save();
        } catch (final RepositoryException e) {
            log.error("Error while moving folder", e);
            throw new WorkflowException("Unable to move folder");
        } finally {
            refreshRootSession();
        }
    }

    private void refreshRootSession() {
        try {
            rootSession.refresh(false);
        } catch (RepositoryException e) {
            log.error("Unable to refresh workflow root session", e);
        }
    }
}

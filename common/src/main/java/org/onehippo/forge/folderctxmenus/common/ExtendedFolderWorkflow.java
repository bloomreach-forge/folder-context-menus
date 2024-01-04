/*
 * Copyright 2023 Bloomreach (https://www.bloomreach.com)
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

import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;

import java.util.Locale;

/**
 * Advanced folder workflow which provides the option to copy or move folders.
 */
public interface ExtendedFolderWorkflow extends Workflow {

    /**
     * Copy a folder to into another folder.
     * @param locale the locale of the folder
     * @param sourceFolderId the UUID of the source folder
     * @param destParentFolderId the UUID of the destination folder into which the folder is copied
     * @param destFolderNodeName the node name of the new folder
     * @param destFolderDisplayName the display name of the new folder
     * @throws WorkflowException when an exception occurs while copying
     */
    void copyFolder(final Locale locale,
                    final String sourceFolderId,
                    final String destParentFolderId,
                    final String destFolderNodeName,
                    final String destFolderDisplayName)
            throws WorkflowException;


    /**
     * Move a folder to another folder.
     * @param locale the locale of the folder
     * @param sourceFolderId the UUID of the source folder
     * @param destParentFolderId the UUID of the destination folder into which the folder is copied
     * @param destFolderNodeName the node name of the new folder
     * @param destFolderDisplayName the display name of the new folder
     * @throws WorkflowException when an exception occurs while copying
     */
    void moveFolder(final Locale locale,
                    final String sourceFolderId,
                    final String destParentFolderId,
                    final String destFolderNodeName,
                    final String destFolderDisplayName)
            throws WorkflowException;

}

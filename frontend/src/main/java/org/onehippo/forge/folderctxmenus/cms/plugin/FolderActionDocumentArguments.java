/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.forge.folderctxmenus.cms.plugin;

import java.util.Collections;
import java.util.Map;

import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.repository.api.Localized;

public class FolderActionDocumentArguments implements IClusterable {

    private String sourceFolderIdentifier;
    private String sourceFolderName;
    private String sourceFolderUriName;
    private String sourceFolderNodeType;
    private Map<Localized, String> sourceFolderLocalizedNames;

    public FolderActionDocumentArguments() {
    }

    public String getSourceFolderIdentifier() {
        return sourceFolderIdentifier;
    }

    public void setSourceFolderIdentifier(String sourceFolderIdentifier) {
        this.sourceFolderIdentifier = sourceFolderIdentifier;
    }

    public String getSourceFolderName() {
        return sourceFolderName;
    }

    public void setSourceFolderName(String sourceFolderName) {
        this.sourceFolderName = sourceFolderName;
    }

    public String getSourceFolderUriName() {
        return sourceFolderUriName;
    }

    public void setSourceFolderUriName(String sourceFolderUriName) {
        this.sourceFolderUriName = sourceFolderUriName;
    }

    public String getSourceFolderNodeType() {
        return sourceFolderNodeType;
    }

    public void setSourceFolderNodeType(String sourceFolderNodeType) {
        this.sourceFolderNodeType = sourceFolderNodeType;
    }

    public Map<Localized, String> getSourceFolderLocalizedNames() {
        if (sourceFolderLocalizedNames == null) {
            return Collections.emptyMap();
        }

        return sourceFolderLocalizedNames;
    }

    public void setSourceFolderLocalizedNames(Map<Localized, String> sourceFolderLocalizedNames) {
        this.sourceFolderLocalizedNames = sourceFolderLocalizedNames;
    }

}

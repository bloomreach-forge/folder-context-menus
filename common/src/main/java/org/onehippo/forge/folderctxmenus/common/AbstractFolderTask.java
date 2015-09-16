/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.commons.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractFolderTask {

    private static Logger defaultLogger = LoggerFactory.getLogger(AbstractFolderTask.class);

    private Logger logger;

    private final Session session;
    private final Node sourceFolderNode;
    private final Locale locale;

    public AbstractFolderTask(final Session session, final Locale locale, final Node sourceFolderNode) {
        this.session = session;

        if (locale != null) {
            this.locale = locale;
        } else {
            this.locale = Locale.getDefault();
        }

        this.sourceFolderNode = sourceFolderNode;
    }

    public Logger getDefaultLogger() {
        return defaultLogger;
    }

    public Logger getLogger() {
        return logger != null ? logger : getDefaultLogger();
    }

    public void setLogger(final Logger logger) {
        this.logger = logger;
    }

    public Session getSession() {
        return session;
    }

    public Locale getLocale() {
        return locale;
    }

    public Node getSourceFolderNode() {
        return sourceFolderNode;
    }

    public final void execute() throws RepositoryException {
        boolean succeeded = false;

        try {
            doBeforeExecute();
            doExecute();
            doAfterExecute();
            succeeded = true;
        } finally {
            if (succeeded) {
                getSession().save();
            } else {
                getSession().refresh(false);
            }
        }
    }

    protected void doBeforeExecute() throws RepositoryException {
    }

    abstract protected void doExecute() throws RepositoryException;

    protected void doAfterExecute() throws RepositoryException {
    }

    protected void updateFolderTranslations(final Node folderNode, final String translatedName, String ... langsToFind) {
        String folderPath = null;

        try {
            folderPath = folderNode.getPath();

            if (StringUtils.isNotBlank(translatedName) && folderNode.isNodeType("hippo:translated")) {
                Node translationNode;
                String language;

                for (NodeIterator nodeIt = folderNode.getNodes("hippo:translation"); nodeIt.hasNext(); ) {
                    translationNode = nodeIt.nextNode();
                    language = JcrUtils.getStringProperty(translationNode, "hippo:language", null);

                    if (StringUtils.isBlank(language) || ArrayUtils.contains(langsToFind, language)) {
                        translationNode.setProperty("hippo:message", translatedName);
                    }
                }
            }
        } catch (RepositoryException e) {
            getLogger().error("Failed to set hippo:translation node of folder at {} with value='{}'.",
                    folderPath, translatedName, e);
        }
    }

}

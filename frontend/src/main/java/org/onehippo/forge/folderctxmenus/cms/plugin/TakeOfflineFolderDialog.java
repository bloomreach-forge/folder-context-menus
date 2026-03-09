/*
 * Copyright 2025 Bloomreach (https://www.bloomreach.com)
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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.onehippo.forge.folderctxmenus.common.FolderTakeOfflineTask;
import org.onehippo.forge.folderctxmenus.common.OperationCancelledException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TakeOfflineFolderDialog extends AbstractFolderDialog {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(TakeOfflineFolderDialog.class);

    private static final int PROGRESS_DIALOG_WIDTH = 450;

    private static final CssResourceReference CSS_REFERENCE =
            new CssResourceReference(TakeOfflineFolderDialog.class, "TakeOfflineFolderDialog.css");

    // Reuses the shared dialog JS (FolderContextMenus.resizeDialog) from CopyOrMoveFolderDialog.
    private static final JavaScriptResourceReference JS_REFERENCE =
            new JavaScriptResourceReference(CopyOrMoveFolderDialog.class, "CopyOrMoveFolderDialog.js");

    private final WebMarkupContainer formContainer;
    private final WebMarkupContainer progressContainer;
    private final ProgressPanel progressPanel;
    private ProgressTrackingOperationProgress operationProgress;
    private final AtomicReference<Exception> operationError = new AtomicReference<>(null);
    private boolean inProgressMode = false;

    private final String sourceFolderIdentifier;
    private final String sourceFolderName;
    private String sourceFolderPath = "";
    private String operationUserId = "unknown";

    public TakeOfflineFolderDialog(final IPluginContext pluginContext, final IPluginConfig pluginConfig,
                                   final IModel<String> titleModel,
                                   final IModel<FolderActionDocumentArguments> model) {
        super(pluginContext, pluginConfig, titleModel, model);

        final FolderActionDocumentArguments args = model.getObject();
        this.sourceFolderIdentifier = args.getSourceFolderIdentifier();
        this.sourceFolderName = args.getSourceFolderName();

        try {
            final Node sourceFolderNode = UserSession.get().getJcrSession().getNodeByIdentifier(sourceFolderIdentifier);
            sourceFolderPath = sourceFolderNode.getPath();
        } catch (Exception e) {
            log.warn("Failed to resolve source folder path", e);
        }

        formContainer = new WebMarkupContainer("formContainer");
        formContainer.setOutputMarkupId(true);
        formContainer.setOutputMarkupPlaceholderTag(true);
        add(formContainer);

        formContainer.add(new Label("folderName", sourceFolderName));

        progressContainer = new WebMarkupContainer("progressContainer");
        progressContainer.setOutputMarkupId(true);
        progressContainer.setOutputMarkupPlaceholderTag(true);
        progressContainer.setVisible(false);

        progressPanel = new ProgressPanel("progressPanel", new ProgressTrackingOperationProgress(), null) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onCloseClicked(AjaxRequestTarget target) {
                closeDialog();
                refreshJcrSession();
            }
        };
        progressPanel.setVisible(false);
        progressContainer.add(progressPanel);
        add(progressContainer);
    }

    /**
     * Suppresses model-change reactions while the operation is in progress.
     * Each {@code DocumentWorkflow.depublish()} call commits independently and fires JCR
     * observation events that could otherwise cause the CMS to navigate away and
     * auto-close this dialog before the user sees the result.
     */
    @Override
    public void onModelChanged() {
        if (inProgressMode) {
            return;
        }
        super.onModelChanged();
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(CSS_REFERENCE));
        response.render(JavaScriptHeaderItem.forReference(JS_REFERENCE));
    }

    @Override
    protected void handleSubmit() {
        onOk();
        if (!hasError() && !inProgressMode) {
            closeDialog();
        }
    }

    protected void onOk() {
        AjaxRequestTarget target = getRequestCycle().find(AjaxRequestTarget.class).orElse(null);
        if (target != null) {
            executeTakeOffline(target);
        }
    }

    private void executeTakeOffline(final AjaxRequestTarget target) {
        final Session session = UserSession.get().getJcrSession();
        operationUserId = resolveUserId();

        inProgressMode = true;
        operationProgress = new ProgressTrackingOperationProgress();
        operationError.set(null);

        formContainer.setVisible(false);
        setOkVisible(false);
        setCancelVisible(false);

        progressContainer.setVisible(true);
        progressPanel.reinitialize(operationProgress, () ->
                CompletableFuture.runAsync(() -> {
                    try {
                        final Session bgSession = session.impersonate(
                                new SimpleCredentials(session.getUserID(), new char[0]));
                        try {
                            final Node sourceNode = bgSession.getNodeByIdentifier(sourceFolderIdentifier);
                            final FolderTakeOfflineTask task = new FolderTakeOfflineTask(bgSession, sourceNode);
                            task.setOperationProgress(operationProgress);
                            task.execute();
                            // Refresh to sync any changes made by the workflow sessions
                            bgSession.refresh(false);
                        } finally {
                            bgSession.logout();
                        }
                    } catch (Exception e) {
                        operationError.set(e);
                    } finally {
                        operationProgress.setCompletionSummary(buildCompletionSummary(operationError.get()));
                        operationProgress.markCompleted();
                    }
                }, FolderOperationExecutors.SHARED_EXECUTOR)
        );
        progressPanel.setVisible(true);

        target.add(formContainer, progressContainer);
        target.appendJavaScript("FolderContextMenus.resizeDialog(" + PROGRESS_DIALOG_WIDTH + ")");
    }

    private ProgressCompletionSummary buildCompletionSummary(final Exception error) {
        if (error instanceof OperationCancelledException || operationProgress.isCancelled()) {
            log.info("Take offline cancelled: user='{}', path='{}'", operationUserId, sourceFolderPath);
            return new ProgressCompletionSummary(
                    "Cancelled after taking " + operationProgress.getCurrentCount() + " items offline.", false);
        }
        if (error != null) {
            log.error("Take offline failed: user='{}', path='{}'", operationUserId, sourceFolderPath, error);
            return new ProgressCompletionSummary(
                    "Take offline failed: " + error.getMessage(), true);
        }
        log.info("Take offline completed: user='{}', path='{}'", operationUserId, sourceFolderPath);
        return new ProgressCompletionSummary(
                "All published documents in '" + sourceFolderName + "' have been taken offline.", false);
    }

    private void refreshJcrSession() {
        try {
            UserSession.get().getJcrSession().refresh(false);
        } catch (RepositoryException e) {
            log.warn("Failed to refresh JCR session after take offline", e);
        }
    }

    private String resolveUserId() {
        try {
            return UserSession.get().getJcrSession().getUserID();
        } catch (Exception e) {
            return "unknown";
        }
    }

}

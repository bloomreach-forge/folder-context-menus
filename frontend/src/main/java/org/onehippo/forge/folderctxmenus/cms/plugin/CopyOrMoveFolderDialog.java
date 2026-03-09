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
import java.util.concurrent.ThreadPoolExecutor;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.swing.tree.TreeNode;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.tree.IJcrTreeNode;
import org.hippoecm.frontend.model.tree.JcrTreeModel;
import org.hippoecm.frontend.model.tree.JcrTreeNode;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.tree.FolderTreeNode;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.StringCodecFactory;
import org.onehippo.forge.folderctxmenus.common.OperationCancelledException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CopyOrMoveFolderDialog extends AbstractFolderDialog {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(CopyOrMoveFolderDialog.class);
    public static final String DEFAULT_LINK_TRANSLATION = "default.linkTranslation";
    private static final int PROGRESS_DIALOG_WIDTH = 450;
    private static final CssResourceReference CSS_REFERENCE =
            new CssResourceReference(CopyOrMoveFolderDialog.class, "CopyOrMoveFolderDialog.css");
    private static final JavaScriptResourceReference JS_REFERENCE =
            new JavaScriptResourceReference(CopyOrMoveFolderDialog.class, "CopyOrMoveFolderDialog.js");
    private static final ThreadPoolExecutor FOLDER_OP_EXECUTOR = FolderOperationExecutors.SHARED_EXECUTOR;

    private final FolderSelectionCmsJcrTree folderTree;
    private JcrTreeModel treeModel;
    private JcrTreeNode rootTreeNode;
    private JcrNodeModel rootNodeModel;

    private WebMarkupContainer formContainer;
    private WebMarkupContainer progressContainer;
    private ProgressPanel progressPanel;
    private ProgressTrackingOperationProgress operationProgress;
    private final AtomicReference<Exception> operationError = new AtomicReference<>(null);
    private final AtomicBoolean outcomeLogged = new AtomicBoolean(false);
    private boolean inProgressMode = false;
    private String operationUserId;
    private String operationId;

    private String sourceFolderIdentifier = "";
    private String sourceFolderPath = "";
    private String sourceFolderPathDisplay = "";
    private String destinationFolderIdentifier = "";
    private String destinationFolderPath = "";
    private String destinationFolderPathDisplay = "";
    private String newFolderName = "";
    private String newFolderUrlName = "";
    private Boolean linkAsTranslation = Boolean.FALSE;
    private final boolean isCopyOperation;

    public CopyOrMoveFolderDialog(final IPluginContext pluginContext, final IPluginConfig pluginConfig,
                                  IModel<String> titleModel, IModel<FolderActionDocumentArguments> model,
                                  boolean isCopyDialog) {

        super(pluginContext, pluginConfig, titleModel, model);

        this.isCopyOperation = isCopyDialog;
        final FolderActionDocumentArguments folderActionDocumentModel = model.getObject();

        boolean isSourceFolderTranslated = false;
        if (StringUtils.isNotEmpty(folderActionDocumentModel.getSourceFolderIdentifier())) {
            try {
                Node sourceFolderNode = UserSession.get().getJcrSession().getNodeByIdentifier(folderActionDocumentModel.getSourceFolderIdentifier());
                sourceFolderIdentifier = sourceFolderNode.getIdentifier();
                sourceFolderPath = sourceFolderNode.getPath();
                sourceFolderPathDisplay = getDisplayPathOfNode(sourceFolderNode);
                newFolderName = getDisplayNameOfNode(sourceFolderNode);
                newFolderUrlName = folderActionDocumentModel.getSourceFolderUriName();
                isSourceFolderTranslated = sourceFolderNode.isNodeType("hippotranslation:translated");
            } catch (RepositoryException e) {
                log.error("Failed to retrieve source folder node.", e);
            }
        }

        formContainer = new WebMarkupContainer("formContainer");
        formContainer.setOutputMarkupId(true);
        formContainer.setOutputMarkupPlaceholderTag(true);
        add(formContainer);

        progressContainer = new WebMarkupContainer("progressContainer");
        progressContainer.setOutputMarkupId(true);
        progressContainer.setOutputMarkupPlaceholderTag(true);
        progressContainer.setVisible(false);
        progressPanel = new ProgressPanel("progressPanel", new ProgressTrackingOperationProgress(), null) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onCloseClicked(AjaxRequestTarget target) {
                refreshJcrSession();
                closeDialog();
                browseToDestinationIfSuccessful();
            }
        };
        progressPanel.setVisible(false);
        progressContainer.add(progressPanel);
        add(progressContainer);

        final Form form = new Form("form");
        formContainer.add(form);

        final TextField<String> sourceFolderPathDisplayField =
            new TextField<>("sourceFolderPathDisplay", LambdaModel.of(this::getSourceFolderPathDisplay, this::setSourceFolderPathDisplay));
        sourceFolderPathDisplayField.setEnabled(false);
        form.add(sourceFolderPathDisplayField);

        final TextField<String> destinationFolderPathDisplayField =
            new TextField<>("destinationFolderPathDisplay", LambdaModel.of(this::getDestinationFolderPathDisplay, this::setDestinationFolderPathDisplay));
        destinationFolderPathDisplayField.setEnabled(false);
        destinationFolderPathDisplayField.setOutputMarkupId(true);
        form.add(destinationFolderPathDisplayField);

        rootNodeModel = new JcrNodeModel(sourceFolderPath.replaceAll("^(/[^/]+/[^/]+).*", "$1"));
        FolderOnlyDocumentListFilter folderOnlyTreeConfig = new FolderOnlyDocumentListFilter(pluginConfig);
        rootTreeNode = new FolderTreeNode(rootNodeModel, folderOnlyTreeConfig);
        treeModel = new JcrTreeModel(rootTreeNode);
        folderTree = new FolderSelectionCmsJcrTree("folderTree", treeModel, pluginContext, pluginConfig);
        folderTree.setRootLess(true);
        folderTree.addTreeNodeEventListener(new ITreeNodeEventListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void nodeLinkClicked(AjaxRequestTarget target, TreeNode clickedNode) {
                if (clickedNode instanceof IJcrTreeNode) {
                    try {
                        final IJcrTreeNode treeNodeModel = (IJcrTreeNode) clickedNode;
                        final Node folderNode = treeNodeModel.getNodeModel().getObject();
                        destinationFolderIdentifier = folderNode.getIdentifier();
                        destinationFolderPath = folderNode.getPath();
                        destinationFolderPathDisplay = getDisplayPathOfNode(folderNode);
                        target.add(destinationFolderPathDisplayField);
                    } catch (RepositoryException e) {
                        log.error("Failed to retrieve selected folder node.", e);
                    }
                }
            }
        });

        form.add(folderTree);

        final TextField<String> newFolderUrlNameField =
            new TextField<>("newFolderUrlName", LambdaModel.of(this::getNewFolderUrlName, this::setNewFolderUrlName));
        newFolderUrlNameField.setOutputMarkupId(true);
        form.add(newFolderUrlNameField);

        final TextField<String> newFolderNameField =
            new TextField<>("newFolderName", LambdaModel.of(this::getNewFolderName, this::setNewFolderName));
        newFolderNameField.setOutputMarkupId(true);
        newFolderNameField.add(new AjaxFormComponentUpdatingBehavior("change") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                newFolderUrlName = new StringCodecFactory.UriEncoding().encode(getNewFolderName());
                target.add(newFolderUrlNameField);
            }
        });
        form.add(newFolderNameField);

        WebMarkupContainer row = new WebMarkupContainer("linkAsTranslationRow");
        row.setVisible(isCopyDialog && isSourceFolderTranslated); // this will remove the <tr> from the HTML output
        form.add(row);
        linkAsTranslation = pluginConfig.getAsBoolean(DEFAULT_LINK_TRANSLATION);
        final CheckBox linkAsTranslationsField =
            new CheckBox("linkAsTranslation", LambdaModel.of(this::getLinkAsTranslation, this::setLinkAsTranslation));
        linkAsTranslationsField.setOutputMarkupId(true);
        linkAsTranslationsField.setVisible(isCopyDialog && isSourceFolderTranslated);
        row.add(linkAsTranslationsField);
    }

    @Override
    public IValueMap getProperties() {
        return new ValueMap("width=640,height=480").makeImmutable();
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(CSS_REFERENCE));
        response.render(JavaScriptHeaderItem.forReference(JS_REFERENCE));
    }

    public String getSourceFolderIdentifier() {
        return sourceFolderIdentifier;
    }

    public String getSourceFolderPathDisplay() {
        return sourceFolderPathDisplay;
    }

    public void setSourceFolderPathDisplay(String sourceFolderPathDisplay) {
        this.sourceFolderPathDisplay = sourceFolderPathDisplay;
    }

    public String getDestinationFolderIdentifier() {
        return destinationFolderIdentifier;
    }

    public String getDestinationFolderPathDisplay() {
        return destinationFolderPathDisplay;
    }

    public void setDestinationFolderPathDisplay(String destinationFolderPathDisplay) {
        this.destinationFolderPathDisplay = destinationFolderPathDisplay;
    }

    public String getNewFolderName() {
        return newFolderName;
    }

    public void setNewFolderName(String newFolderName) {
        this.newFolderName = newFolderName;
    }

    public String getNewFolderUrlName() {
        return newFolderUrlName;
    }

    public void setNewFolderUrlName(String newFolderUrlName) {
        this.newFolderUrlName = newFolderUrlName;
    }

    public Boolean getLinkAsTranslation() {
        return linkAsTranslation;
    }

    public void setLinkAsTranslation(Boolean linkAsTranslation) {
        this.linkAsTranslation = linkAsTranslation;
    }

    @Override
    protected void handleSubmit() {
        onOk();
        if (!hasError() && !inProgressMode) {
            closeDialog();
        }
    }

    protected void startOperationWithProgress(AjaxRequestTarget target, FolderOperationExecutor executor) {
        inProgressMode = true;
        operationProgress = new ProgressTrackingOperationProgress();
        operationError.set(null);
        outcomeLogged.set(false);
        operationUserId = resolveUserId();
        operationId = UUID.randomUUID().toString();

        formContainer.setVisible(false);
        setOkVisible(false);
        setCancelVisible(false);

        progressContainer.setVisible(true);
        progressPanel.reinitialize(operationProgress, () -> {
            if (operationProgress.isCancelled()) {
                operationProgress.setCompletionSummary(buildCompletionSummary(operationError.get()));
                operationProgress.markCompleted();
                return;
            }
            CompletableFuture.runAsync(() -> {
                try {
                    executor.execute(operationProgress);
                } catch (Exception e) {
                    operationError.set(e);
                } finally {
                    Exception error = operationError.get();
                    logOutcome(error);
                    operationProgress.setCompletionSummary(buildCompletionSummary(error));
                    operationProgress.markCompleted();
                }
            }, FOLDER_OP_EXECUTOR);
        });
        progressPanel.setVisible(true);

        target.add(formContainer, progressContainer);
        target.appendJavaScript("FolderContextMenus.resizeDialog(" + PROGRESS_DIALOG_WIDTH + ")");

    }

    protected boolean validateInput() {
        if (StringUtils.isBlank(destinationFolderIdentifier)) {
            error("Please select the target folder.");
            return false;
        }
        if (StringUtils.isBlank(newFolderUrlName) || StringUtils.isBlank(newFolderName)) {
            error("Please enter the destination folder name.");
            return false;
        }
        return true;
    }

    protected boolean destinationExists(Session session) {
        try {
            Node destParentNode = session.getNodeByIdentifier(destinationFolderIdentifier);
            String destPath = destParentNode.getPath() + "/" + newFolderUrlName;
            if (session.nodeExists(destPath)) {
                error("A folder named '" + newFolderName + "' already exists at the destination. " +
                        "Please choose a different name or destination.");
                return true;
            }
        } catch (RepositoryException e) {
            log.error("Failed to check destination folder", e);
            error("Failed to verify destination: " + e.getMessage());
            return true;
        }
        return false;
    }

    private static String getUserFriendlyErrorMessage(Exception e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof AccessDeniedException) {
                return "Access denied. You may not have permission to perform this operation.";
            }
            if (cause instanceof LockException) {
                return "The content is currently locked by another user or process.";
            }
            if (cause instanceof ConstraintViolationException) {
                return "The operation violates repository constraints.";
            }
            cause = cause.getCause();
        }

        if (e instanceof RepositoryException) {
            return "A repository error occurred. Please try again or contact support.";
        }
        return "An unexpected error occurred.";
    }

    private ProgressCompletionSummary buildCompletionSummary(Exception error) {
        long count = operationProgress.getCurrentCount();
        boolean cancelled = error instanceof OperationCancelledException || operationProgress.isCancelled();
        String verb = isCopyOperation ? "copied" : "moved";

        if (cancelled) {
            return new ProgressCompletionSummary("Cancelled after " + verb + " " + count + " items", false);
        }
        if (error != null) {
            return new ProgressCompletionSummary(
                    "Error after " + count + " items: " + getUserFriendlyErrorMessage(error),
                    true);
        }
        return new ProgressCompletionSummary("Successfully " + verb + " " + count + " items", false);
    }

    private void logOutcome(Exception error) {
        if (!outcomeLogged.compareAndSet(false, true)) {
            return;
        }

        long count = operationProgress.getCurrentCount();
        boolean cancelled = error instanceof OperationCancelledException || operationProgress.isCancelled();

        if (cancelled) {
            log.info("Folder {} operation was cancelled by user", isCopyOperation ? "copy" : "move");
        } else if (error != null) {
            log.error("Folder {} operation failed", isCopyOperation ? "copy" : "move", error);
        } else {
            logOperationEvent(count);
        }
    }

    private void logOperationEvent(long itemCount) {
        try {
            String destJcrPath = destinationFolderPath + "/" + newFolderUrlName;
            String operation = isCopyOperation ? "copy" : "move";

            log.info("Folder {} completed: opId='{}', user='{}', source='{}', destination='{}', items={}",
                    operation,
                    operationId,
                    operationUserId,
                    sourceFolderPath,
                    destJcrPath,
                    itemCount);
        } catch (Exception e) {
            log.warn("Failed to log folder operation event: {}", e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private void browseToDestinationIfSuccessful() {
        if (operationProgress == null || operationProgress.isCancelled() || operationError.get() != null) {
            return;
        }
        if (StringUtils.isBlank(destinationFolderPath) || StringUtils.isBlank(newFolderUrlName)) {
            return;
        }
        IBrowseService<JcrNodeModel> browseService = getPluginContext()
                .getService(IBrowseService.class.getName(), IBrowseService.class);
        if (browseService != null) {
            browseService.browse(new JcrNodeModel(destinationFolderPath + "/" + newFolderUrlName));
        }
    }

    private void refreshJcrSession() {
        try {
            UserSession.get().getJcrSession().refresh(false);
        } catch (RepositoryException e) {
            log.warn("Failed to refresh JCR session after folder operation", e);
        }
    }

    private String resolveUserId() {
        try {
            return UserSession.get().getJcrSession().getUserID();
        } catch (Exception e) {
            log.warn("Failed to resolve user id for folder operation logging", e);
            return "unknown";
        }
    }

    public interface FolderOperationExecutor {
        void execute(ProgressTrackingOperationProgress progress) throws Exception;
    }
}

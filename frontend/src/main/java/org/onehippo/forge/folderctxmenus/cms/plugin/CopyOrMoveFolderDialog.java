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
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
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
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.tree.IJcrTreeNode;
import org.hippoecm.frontend.model.tree.JcrTreeModel;
import org.hippoecm.frontend.model.tree.JcrTreeNode;
import org.hippoecm.frontend.plugin.IPluginContext;
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
    private static final JavaScriptResourceReference JS_REFERENCE =
            new JavaScriptResourceReference(CopyOrMoveFolderDialog.class, "CopyOrMoveFolderDialog.js");
    private static final Executor FOLDER_OP_EXECUTOR = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "FolderContextMenus-Operation");
        thread.setDaemon(true);
        return thread;
    });

    private final FolderSelectionCmsJcrTree folderTree;
    private JcrTreeModel treeModel;
    private JcrTreeNode rootTreeNode;
    private JcrNodeModel rootNodeModel;

    private WebMarkupContainer formContainer;
    private WebMarkupContainer progressContainer;
    private ProgressTrackingOperationProgress operationProgress;
    private volatile Exception operationError;
    private boolean inProgressMode = false;

    private String sourceFolderIdentifier = "";
    private String sourceFolderPath = "";
    private String sourceFolderPathDisplay = "";
    private String destinationFolderIdentifier = "";
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
        progressContainer.add(new EmptyPanel("progressPanel"));
        add(progressContainer);

        final Form form = new Form("form");
        formContainer.add(form);

        final TextField<String> sourceFolderPathDisplayField =
            new TextField<String>("sourceFolderPathDisplay", new PropertyModel<String>(this, "sourceFolderPathDisplay"));
        sourceFolderPathDisplayField.setEnabled(false);
        form.add(sourceFolderPathDisplayField);

        final TextField<String> destinationFolderPathDisplayField =
            new TextField<String>("destinationFolderPathDisplay", new PropertyModel<String>(this, "destinationFolderPathDisplay"));
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
            new TextField<String>("newFolderUrlName", new PropertyModel<String>(this, "newFolderUrlName"));
        newFolderUrlNameField.setOutputMarkupId(true);
        newFolderUrlNameField.add(new AjaxFormComponentUpdatingBehavior("change") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
            }
        });
        form.add(newFolderUrlNameField);

        final TextField<String> newFolderNameField =
            new TextField<String>("newFolderName", new PropertyModel<String>(this, "newFolderName"));
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
            new CheckBox("linkAsTranslation", new PropertyModel<Boolean>(this, "linkAsTranslation"));
        linkAsTranslationsField.setOutputMarkupId(true);
        linkAsTranslationsField.setVisible(isCopyDialog && isSourceFolderTranslated);
        row.add(linkAsTranslationsField);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(
                new PackageResourceReference(CopyOrMoveFolderDialog.class, "CopyOrMoveFolderDialog.css")));
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

    @Override
    public void onCancel() {
        cancelOperationIfRunning();
        super.onCancel();
    }

    @Override
    public void onClose() {
        cancelOperationIfRunning();
        super.onClose();
    }

    private void cancelOperationIfRunning() {
        if (operationProgress != null && !operationProgress.isCompleted()) {
            operationProgress.cancel();
        }
    }

    protected void startOperationWithProgress(AjaxRequestTarget target, FolderOperationExecutor executor) {
        inProgressMode = true;
        operationProgress = new ProgressTrackingOperationProgress();
        operationError = null;

        formContainer.setVisible(false);
        setOkVisible(false);
        setCancelVisible(false);

        progressContainer.setVisible(true);
        ProgressPanel progressPanel = new ProgressPanel("progressPanel", operationProgress) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onOperationComplete(AjaxRequestTarget target) {
                String message;
                boolean isError;
                long count = operationProgress.getCurrentCount();

                if (operationError instanceof OperationCancelledException || operationProgress.isCancelled()) {
                    String gerund = isCopyOperation ? "copying" : "moving";
                    message = "Cancelled after " + gerund + " " + count + " items";
                    isError = false;
                } else if (operationError != null) {
                    log.error("Folder operation failed after {} items", count, operationError);
                    message = "Error after " + count + " items: " + operationError.getMessage();
                    isError = true;
                } else {
                    String pastTense = isCopyOperation ? "copied" : "moved";
                    message = "Successfully " + pastTense + " " + count + " items";
                    isError = false;
                }

                showCompletionSummary(target, message, isError);
            }

            @Override
            protected void onCloseClicked(AjaxRequestTarget target) {
                closeDialog();
            }
        };
        progressContainer.replace(progressPanel);

        target.add(formContainer, progressContainer);
        target.appendJavaScript("FolderContextMenus.resizeDialog(" + PROGRESS_DIALOG_WIDTH + ")");

        CompletableFuture.runAsync(() -> {
            try {
                executor.execute(operationProgress);
            } catch (Exception e) {
                operationError = e;
            } finally {
                operationProgress.markCompleted();
            }
        }, FOLDER_OP_EXECUTOR);
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

    public interface FolderOperationExecutor {
        void execute(ProgressTrackingOperationProgress progress) throws Exception;
    }
}

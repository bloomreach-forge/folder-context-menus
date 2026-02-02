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

import java.time.Duration;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxChannel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.CssResourceReference;

public abstract class ProgressPanel extends Panel {

    private static final long serialVersionUID = 1L;
    private static final int POLL_INTERVAL_MS = 200;
    private static final int MAX_PATH_DISPLAY_LENGTH = 60;

    private final ProgressTrackingOperationProgress progress;
    private final Label statusLabel;
    private final WebMarkupContainer progressBar;
    private final Label progressLabel;
    private final Label pathLabel;
    private final AjaxLink<Void> cancelButton;
    private final AjaxLink<Void> closeButton;

    public ProgressPanel(String id, ProgressTrackingOperationProgress progress) {
        super(id);
        this.progress = progress;

        statusLabel = new Label("statusLabel", Model.of("Starting..."));
        statusLabel.setOutputMarkupId(true);
        add(statusLabel);

        progressBar = new WebMarkupContainer("progressBar");
        progressBar.setOutputMarkupId(true);
        progressBar.add(AttributeModifier.replace("style", "width: 0%"));
        add(progressBar);

        progressLabel = new Label("progressLabel", Model.of("0%"));
        progressLabel.setOutputMarkupId(true);
        progressBar.add(progressLabel);

        pathLabel = new Label("pathLabel", Model.of(""));
        pathLabel.setOutputMarkupId(true);
        pathLabel.setOutputMarkupPlaceholderTag(true);
        add(pathLabel);

        cancelButton = new AjaxLink<Void>("cancelButton") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.setChannel(new AjaxChannel("cancel-op", AjaxChannel.Type.ACTIVE));
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                progress.cancel();
                cancelButton.setEnabled(false);
                statusLabel.setDefaultModel(Model.of("Cancelling..."));
                target.add(cancelButton, statusLabel);
            }
        };
        cancelButton.setOutputMarkupId(true);
        add(cancelButton);

        closeButton = new AjaxLink<Void>("closeButton") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                onCloseClicked(target);
            }
        };
        closeButton.setOutputMarkupId(true);
        add(closeButton);

        AbstractAjaxTimerBehavior timerBehavior = new AbstractAjaxTimerBehavior(Duration.ofMillis(POLL_INTERVAL_MS)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onTimer(AjaxRequestTarget target) {
                if (progress.isCompleted()) {
                    stop(target);
                    onOperationComplete(target);
                    return;
                }

                updateProgressDisplay(target);
            }
        };
        add(timerBehavior);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(
                new CssResourceReference(ProgressPanel.class, "ProgressPanel.css")));
    }

    private void updateProgressDisplay(AjaxRequestTarget target) {
        long total = progress.getTotalCount();
        int percentage = total > 0 ? progress.getProgressPercentage() : 0;

        updateStatus(total);
        updateProgressBar(percentage);
        updatePath(progress.getCurrentPath());

        target.add(statusLabel, progressBar, pathLabel);

        if (progress.isCancelled() && cancelButton.isEnabled()) {
            cancelButton.setEnabled(false);
            target.add(cancelButton);
        }
    }

    private void updateStatus(long total) {
        String status;
        if (total > 0) {
            String eta = progress.getEstimatedTimeRemaining();
            status = String.format("Processing %d/%d items%s",
                    progress.getCurrentCount(), total,
                    eta.isEmpty() ? "" : " - " + eta);
        } else {
            status = "Initializing...";
        }
        statusLabel.setDefaultModel(Model.of(status));
    }

    private void updateProgressBar(int percentage) {
        progressLabel.setDefaultModel(Model.of(percentage + "%"));
        progressBar.add(AttributeModifier.replace("style", "width: " + percentage + "%"));
    }

    private void updatePath(String path) {
        String displayPath = "";
        if (path != null && !path.isEmpty()) {
            displayPath = truncatePath(path);
        }
        pathLabel.setDefaultModel(Model.of(displayPath));
    }

    private String truncatePath(String path) {
        if (path.length() <= MAX_PATH_DISPLAY_LENGTH) {
            return path;
        }
        return "..." + path.substring(path.length() - MAX_PATH_DISPLAY_LENGTH + 3);
    }

    protected abstract void onOperationComplete(AjaxRequestTarget target);

    protected abstract void onCloseClicked(AjaxRequestTarget target);

    public void showCompletionSummary(AjaxRequestTarget target, String message, boolean isError) {
        statusLabel.setDefaultModel(Model.of(message));

        if (isError) {
            progressBar.add(AttributeModifier.replace("class", "progress-bar progress-bar-error"));
        } else {
            progressBar.add(AttributeModifier.replace("style", "width: 100%"));
            progressLabel.setDefaultModel(Model.of("100%"));
        }

        pathLabel.setVisible(false);
        cancelButton.add(AttributeModifier.append("class", " hidden-btn"));
        closeButton.add(AttributeModifier.remove("style"));

        target.add(statusLabel, progressBar, pathLabel, cancelButton, closeButton);
    }
}

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

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxChannel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.TextRequestHandler;
import org.apache.wicket.request.resource.CssResourceReference;

public abstract class ProgressPanel extends Panel {

    private static final long serialVersionUID = 1L;
    private static final int MAX_PATH_DISPLAY_LENGTH = 60;
    private static final int POLLING_INTERVAL_MS = 200;

    private final ProgressTrackingOperationProgress progress;
    private final Runnable startOperation;
    private final AtomicBoolean operationStarted = new AtomicBoolean(false);

    private final Label statusLabel;
    private final WebMarkupContainer progressBar;
    private final Label progressLabel;
    private final Label pathLabel;
    private final AjaxLink<Void> cancelButton;
    private final AjaxLink<Void> closeButton;
    private final AbstractDefaultAjaxBehavior progressBehavior;

    public ProgressPanel(String id, ProgressTrackingOperationProgress progress, Runnable startOperation) {
        super(id);
        setOutputMarkupId(true);
        this.progress = progress;
        this.startOperation = startOperation;

        add(statusLabel = createStatusLabel());
        add(progressLabel = createProgressLabel());
        add(progressBar = createProgressBar(progressLabel));
        add(pathLabel = createPathLabel());
        add(cancelButton = createCancelButton());
        add(closeButton = createCloseButton());
        add(progressBehavior = createProgressBehavior());
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(
                new CssResourceReference(ProgressPanel.class, "ProgressPanel.css")));
        response.render(OnDomReadyHeaderItem.forScript(buildPollingScript()));
    }

    protected abstract void onCloseClicked(AjaxRequestTarget target);

    private Label createStatusLabel() {
        Label label = new Label("statusLabel", Model.of("Starting..."));
        label.setOutputMarkupId(true);
        return label;
    }

    private WebMarkupContainer createProgressBar(Label label) {
        WebMarkupContainer bar = new WebMarkupContainer("progressBar");
        bar.setOutputMarkupId(true);
        bar.add(AttributeModifier.replace("style", "width: 0%"));
        bar.add(label);
        return bar;
    }

    private Label createProgressLabel() {
        Label label = new Label("progressLabel", Model.of("0%"));
        label.setOutputMarkupId(true);
        return label;
    }

    private Label createPathLabel() {
        Label label = new Label("pathLabel", Model.of(""));
        label.setOutputMarkupId(true);
        label.setOutputMarkupPlaceholderTag(true);
        return label;
    }

    private AjaxLink<Void> createCancelButton() {
        AjaxLink<Void> button = new AjaxLink<Void>("cancelButton") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.setChannel(new AjaxChannel("cancel-op", AjaxChannel.Type.ACTIVE));
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                progress.cancel();
                setEnabled(false);
                statusLabel.setDefaultModel(Model.of("Cancelling..."));
                target.add(this, statusLabel);
            }
        };
        button.setOutputMarkupId(true);
        return button;
    }

    private AjaxLink<Void> createCloseButton() {
        AjaxLink<Void> button = new AjaxLink<Void>("closeButton") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                onCloseClicked(target);
            }
        };
        button.setOutputMarkupId(true);
        return button;
    }

    private AbstractDefaultAjaxBehavior createProgressBehavior() {
        return new AbstractDefaultAjaxBehavior() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void respond(AjaxRequestTarget target) {
                startOperationIfNeeded();
                String payload = ProgressPanelJsonBuilder.buildProgressPayload(progress);
                RequestCycle.get().scheduleRequestHandlerAfterCurrent(
                        new TextRequestHandler("application/json", "UTF-8", payload));
            }
        };
    }

    private void startOperationIfNeeded() {
        if (startOperation == null) {
            return;
        }
        if (operationStarted.compareAndSet(false, true)) {
            startOperation.run();
        }
    }

    private String buildPollingScript() {
        ProgressPanelJsonBuilder.PollingConfig config = new ProgressPanelJsonBuilder.PollingConfig(
                progressBehavior.getCallbackUrl().toString(),
                getMarkupId(),
                POLLING_INTERVAL_MS,
                MAX_PATH_DISPLAY_LENGTH,
                statusLabel.getMarkupId(),
                progressBar.getMarkupId(),
                progressLabel.getMarkupId(),
                pathLabel.getMarkupId(),
                cancelButton.getMarkupId(),
                closeButton.getMarkupId()
        );
        return "FolderContextMenus.startProgressPolling(" + ProgressPanelJsonBuilder.buildPollingConfig(config) + ");";
    }
}

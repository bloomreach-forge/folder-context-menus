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

    private final ProgressTrackingOperationProgress progress;
    private final Label statusLabel;
    private final WebMarkupContainer progressBar;
    private final Label progressLabel;
    private final Label pathLabel;
    private final AjaxLink<Void> cancelButton;
    private final AjaxLink<Void> closeButton;
    private final AbstractDefaultAjaxBehavior progressBehavior;
    private final Runnable startOperation;
    private final java.util.concurrent.atomic.AtomicBoolean operationStarted = new java.util.concurrent.atomic.AtomicBoolean(false);

    public ProgressPanel(String id, ProgressTrackingOperationProgress progress, Runnable startOperation) {
        super(id);
        setOutputMarkupId(true);
        this.progress = progress;
        this.startOperation = startOperation;

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

        progressBehavior = new AbstractDefaultAjaxBehavior() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void respond(AjaxRequestTarget target) {
                startOperationIfNeeded();
                String payload = buildProgressPayload();
                RequestCycle.get().scheduleRequestHandlerAfterCurrent(
                        new TextRequestHandler("application/json", "UTF-8", payload));
            }
        };
        add(progressBehavior);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(
                new CssResourceReference(ProgressPanel.class, "ProgressPanel.css")));
        response.render(OnDomReadyHeaderItem.forScript(buildPollingScript()));
    }

    protected abstract void onOperationComplete(AjaxRequestTarget target);

    protected abstract void onCloseClicked(AjaxRequestTarget target);

    private void startOperationIfNeeded() {
        if (startOperation == null) {
            return;
        }
        if (operationStarted.compareAndSet(false, true)) {
            startOperation.run();
        }
    }

    private String buildProgressPayload() {
        StringBuilder payload = new StringBuilder(256);
        boolean[] first = new boolean[] { true };
        payload.append('{');

        appendJsonField(payload, first, "current", progress.getCurrentCount());
        appendJsonField(payload, first, "total", progress.getTotalCount());
        appendJsonField(payload, first, "percent", progress.getProgressPercentage());
        appendJsonField(payload, first, "path", progress.getCurrentPath());
        appendJsonField(payload, first, "eta", progress.getEstimatedTimeRemaining());
        appendJsonField(payload, first, "cancelled", progress.isCancelled());
        appendJsonField(payload, first, "completed", progress.isCompleted());

        ProgressCompletionSummary summary = progress.getCompletionSummary();
        if (summary != null) {
            if (!first[0]) {
                payload.append(',');
            }
            first[0] = false;
            payload.append("\"summary\":{");
            boolean[] summaryFirst = new boolean[] { true };
            appendJsonField(payload, summaryFirst, "message", summary.getMessage());
            appendJsonField(payload, summaryFirst, "error", summary.isError());
            payload.append('}');
        }

        payload.append('}');
        return payload.toString();
    }

    private String buildPollingScript() {
        StringBuilder config = new StringBuilder(256);
        boolean[] first = new boolean[] { true };
        config.append('{');
        appendJsonField(config, first, "url", progressBehavior.getCallbackUrl().toString());
        appendJsonField(config, first, "panelId", getMarkupId());
        appendJsonField(config, first, "intervalMs", 200);
        appendJsonField(config, first, "maxPathLength", MAX_PATH_DISPLAY_LENGTH);
        appendJsonField(config, first, "statusId", statusLabel.getMarkupId());
        appendJsonField(config, first, "progressBarId", progressBar.getMarkupId());
        appendJsonField(config, first, "progressLabelId", progressLabel.getMarkupId());
        appendJsonField(config, first, "pathId", pathLabel.getMarkupId());
        appendJsonField(config, first, "cancelId", cancelButton.getMarkupId());
        appendJsonField(config, first, "closeId", closeButton.getMarkupId());
        config.append('}');

        return "FolderContextMenus.startProgressPolling(" + config + ");";
    }

    private static void appendJsonField(StringBuilder builder, boolean[] first, String key, long value) {
        appendJsonFieldName(builder, first, key);
        builder.append(value);
    }

    private static void appendJsonField(StringBuilder builder, boolean[] first, String key, int value) {
        appendJsonFieldName(builder, first, key);
        builder.append(value);
    }

    private static void appendJsonField(StringBuilder builder, boolean[] first, String key, boolean value) {
        appendJsonFieldName(builder, first, key);
        builder.append(value);
    }

    private static void appendJsonField(StringBuilder builder, boolean[] first, String key, String value) {
        appendJsonFieldName(builder, first, key);
        if (value == null) {
            builder.append("null");
        } else {
            builder.append('"').append(jsonEscape(value)).append('"');
        }
    }

    private static void appendJsonFieldName(StringBuilder builder, boolean[] first, String key) {
        if (!first[0]) {
            builder.append(',');
        }
        first[0] = false;
        builder.append('"').append(key).append("\":");
    }

    private static String jsonEscape(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '\b':
                    escaped.append("\\b");
                    break;
                case '\f':
                    escaped.append("\\f");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    if (ch < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) ch));
                    } else {
                        escaped.append(ch);
                    }
            }
        }
        return escaped.toString();
    }
}

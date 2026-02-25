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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

final class ProgressPanelJsonBuilder {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ProgressPanelJsonBuilder() {
    }

    static String buildProgressPayload(ProgressTrackingOperationProgress progress) {
        ObjectNode json = MAPPER.createObjectNode();

        if (progress.isFinalizing()) {
            json.put("finalizing", true)
                .put("finalizingCount", progress.getFinalizingCount())
                .put("cancelled", progress.isCancelled())
                .put("completed", progress.isCompleted());
        } else {
            json.put("current", progress.getCurrentCount())
                .put("total", progress.getTotalCount())
                .put("percent", progress.getProgressPercentage())
                .put("eta", progress.getEstimatedTimeRemaining())
                .put("cancelled", progress.isCancelled())
                .put("completed", progress.isCompleted());

            String path = progress.getCurrentPath();
            if (path == null) {
                json.putNull("path");
            } else {
                json.put("path", path);
            }
        }

        ProgressCompletionSummary summary = progress.getCompletionSummary();
        if (summary != null) {
            json.putObject("summary")
                    .put("message", summary.getMessage())
                    .put("error", summary.isError());
        }

        return toJsonString(json);
    }

    static String buildPollingConfig(PollingConfig config) {
        ObjectNode json = MAPPER.createObjectNode()
                .put("url", config.url)
                .put("panelId", config.panelId)
                .put("intervalMs", config.intervalMs)
                .put("maxPathLength", config.maxPathLength)
                .put("statusId", config.statusId)
                .put("progressBarId", config.progressBarId)
                .put("progressLabelId", config.progressLabelId)
                .put("pathId", config.pathId)
                .put("cancelId", config.cancelId)
                .put("closeId", config.closeId);

        return toJsonString(json);
    }

    private static String toJsonString(ObjectNode json) {
        try {
            return MAPPER.writeValueAsString(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize JSON", e);
        }
    }

    static final class PollingConfig {
        final String url;
        final String panelId;
        final int intervalMs;
        final int maxPathLength;
        final String statusId;
        final String progressBarId;
        final String progressLabelId;
        final String pathId;
        final String cancelId;
        final String closeId;

        PollingConfig(String url, String panelId, int intervalMs, int maxPathLength,
                      String statusId, String progressBarId, String progressLabelId,
                      String pathId, String cancelId, String closeId) {
            this.url = url;
            this.panelId = panelId;
            this.intervalMs = intervalMs;
            this.maxPathLength = maxPathLength;
            this.statusId = statusId;
            this.progressBarId = progressBarId;
            this.progressLabelId = progressLabelId;
            this.pathId = pathId;
            this.cancelId = cancelId;
            this.closeId = closeId;
        }
    }
}

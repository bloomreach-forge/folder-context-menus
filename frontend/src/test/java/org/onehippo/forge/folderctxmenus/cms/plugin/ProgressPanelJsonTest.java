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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ProgressPanelJsonTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private ProgressTrackingOperationProgress progress;

    @Before
    public void setUp() {
        progress = new ProgressTrackingOperationProgress();
    }

    @Test
    public void buildProgressPayload_shouldContainAllFields() throws Exception {
        progress.updateProgress(50, 100, "/content/documents/test");

        String json = ProgressPanelJsonBuilder.buildProgressPayload(progress);
        JsonNode node = MAPPER.readTree(json);

        assertEquals(50, node.get("current").asLong());
        assertEquals(100, node.get("total").asLong());
        assertEquals(50, node.get("percent").asInt());
        assertEquals("/content/documents/test", node.get("path").asText());
        assertTrue(node.has("eta"));
        assertFalse(node.get("cancelled").asBoolean());
        assertFalse(node.get("completed").asBoolean());
    }

    @Test
    public void buildProgressPayload_withCompletionSummary_shouldIncludeSummaryObject() throws Exception {
        progress.updateProgress(100, 100, "/done");
        progress.setCompletionSummary(new ProgressCompletionSummary("Successfully copied 100 items", false));
        progress.markCompleted();

        String json = ProgressPanelJsonBuilder.buildProgressPayload(progress);
        JsonNode node = MAPPER.readTree(json);

        assertTrue(node.has("summary"));
        JsonNode summary = node.get("summary");
        assertEquals("Successfully copied 100 items", summary.get("message").asText());
        assertFalse(summary.get("error").asBoolean());
    }

    @Test
    public void buildProgressPayload_withErrorSummary_shouldIncludeErrorFlag() throws Exception {
        progress.updateProgress(50, 100, "/error");
        progress.setCompletionSummary(new ProgressCompletionSummary("Error after 50 items", true));

        String json = ProgressPanelJsonBuilder.buildProgressPayload(progress);
        JsonNode node = MAPPER.readTree(json);

        assertTrue(node.get("summary").get("error").asBoolean());
    }

    @Test
    public void buildProgressPayload_shouldEscapeSpecialCharacters() throws Exception {
        progress.updateProgress(1, 10, "/path/with\"quotes\\and\nspecial");

        String json = ProgressPanelJsonBuilder.buildProgressPayload(progress);
        JsonNode node = MAPPER.readTree(json);

        assertEquals("/path/with\"quotes\\and\nspecial", node.get("path").asText());
    }

    @Test
    public void buildProgressPayload_withNullPath_shouldHandleGracefully() throws Exception {
        progress.updateProgress(0, 0, null);

        String json = ProgressPanelJsonBuilder.buildProgressPayload(progress);
        JsonNode node = MAPPER.readTree(json);

        assertTrue(node.get("path").isNull());
    }

    @Test
    public void buildProgressPayload_whenCancelled_shouldReflectCancelledState() throws Exception {
        progress.updateProgress(25, 100, "/test");
        progress.cancel();

        String json = ProgressPanelJsonBuilder.buildProgressPayload(progress);
        JsonNode node = MAPPER.readTree(json);

        assertTrue(node.get("cancelled").asBoolean());
    }

    @Test
    public void buildPollingConfig_shouldContainAllElementIds() throws Exception {
        ProgressPanelJsonBuilder.PollingConfig config = new ProgressPanelJsonBuilder.PollingConfig(
                "http://callback",
                "panel1",
                200,
                60,
                "status1",
                "bar1",
                "label1",
                "path1",
                "cancel1",
                "close1"
        );

        String json = ProgressPanelJsonBuilder.buildPollingConfig(config);
        JsonNode node = MAPPER.readTree(json);

        assertEquals("http://callback", node.get("url").asText());
        assertEquals("panel1", node.get("panelId").asText());
        assertEquals(200, node.get("intervalMs").asInt());
        assertEquals(60, node.get("maxPathLength").asInt());
        assertEquals("status1", node.get("statusId").asText());
        assertEquals("bar1", node.get("progressBarId").asText());
        assertEquals("label1", node.get("progressLabelId").asText());
        assertEquals("path1", node.get("pathId").asText());
        assertEquals("cancel1", node.get("cancelId").asText());
        assertEquals("close1", node.get("closeId").asText());
    }
}

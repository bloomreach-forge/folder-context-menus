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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.junit.Test;

import static org.junit.Assert.*;

public class ProgressCompletionSummaryTest {

    @Test
    public void testSuccessSummary() {
        ProgressCompletionSummary summary = new ProgressCompletionSummary("Completed successfully", false);

        assertEquals("Completed successfully", summary.getMessage());
        assertFalse(summary.isError());
    }

    @Test
    public void testErrorSummary() {
        ProgressCompletionSummary summary = new ProgressCompletionSummary("Operation failed", true);

        assertEquals("Operation failed", summary.getMessage());
        assertTrue(summary.isError());
    }

    @Test
    public void testNullMessage() {
        ProgressCompletionSummary summary = new ProgressCompletionSummary(null, false);

        assertNull(summary.getMessage());
        assertFalse(summary.isError());
    }

    @Test
    public void testImplementsSerializable() {
        ProgressCompletionSummary summary = new ProgressCompletionSummary("test", false);

        assertTrue(summary instanceof Serializable);
    }

    @Test
    public void testSerializationRoundTrip() throws Exception {
        ProgressCompletionSummary original = new ProgressCompletionSummary("Serialization test", true);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(original);
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ProgressCompletionSummary deserialized;
        try (ObjectInputStream ois = new ObjectInputStream(bais)) {
            deserialized = (ProgressCompletionSummary) ois.readObject();
        }

        assertEquals(original.getMessage(), deserialized.getMessage());
        assertEquals(original.isError(), deserialized.isError());
    }
}

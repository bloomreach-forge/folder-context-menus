/*
 * Copyright 2025 Bloomreach (https://www.bloomreach.com)
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.jcr.RepositoryException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PublishedContentExceptionTest {

    @Test
    void shouldExtendRepositoryException() {
        PublishedContentException ex = new PublishedContentException(Collections.emptyList());
        assertInstanceOf(RepositoryException.class, ex);
    }

    @Test
    void messageIncludesDocumentCount() {
        List<String> paths = Arrays.asList("/a", "/b", "/c");
        PublishedContentException ex = new PublishedContentException(paths);
        assertTrue(ex.getMessage().contains("3"), "Message should contain the document count");
    }

    @Test
    void getDocumentPaths_returnsAllPaths() {
        List<String> paths = Arrays.asList("/content/a", "/content/b");
        PublishedContentException ex = new PublishedContentException(paths);
        assertEquals(paths, ex.getDocumentPaths());
    }

    @Test
    void getDocumentPaths_isUnmodifiable() {
        List<String> paths = Arrays.asList("/content/a");
        PublishedContentException ex = new PublishedContentException(paths);
        assertThrows(UnsupportedOperationException.class,
                () -> ex.getDocumentPaths().add("/content/extra"));
    }

    @Test
    void singleDocument_hasCorrectMessage() {
        PublishedContentException ex = new PublishedContentException(
                Collections.singletonList("/content/docs/news/article"));
        assertTrue(ex.getMessage().contains("1"));
    }
}

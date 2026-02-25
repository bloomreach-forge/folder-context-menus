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

import org.junit.Test;

import static org.junit.Assert.*;

public class FolderActionDocumentArgumentsTest {

    @Test
    public void defaultConstructor_allFieldsNullByDefault() {
        FolderActionDocumentArguments args = new FolderActionDocumentArguments();

        assertNull(args.getSourceFolderIdentifier());
        assertNull(args.getSourceFolderName());
        assertNull(args.getSourceFolderUriName());
        assertNull(args.getSourceFolderNodeType());
    }

    @Test
    public void setAndGetSourceFolderIdentifier() {
        FolderActionDocumentArguments args = new FolderActionDocumentArguments();
        args.setSourceFolderIdentifier("abc-123");

        assertEquals("abc-123", args.getSourceFolderIdentifier());
    }

    @Test
    public void setAndGetSourceFolderName() {
        FolderActionDocumentArguments args = new FolderActionDocumentArguments();
        args.setSourceFolderName("My News Folder");

        assertEquals("My News Folder", args.getSourceFolderName());
    }

    @Test
    public void setAndGetSourceFolderUriName() {
        FolderActionDocumentArguments args = new FolderActionDocumentArguments();
        args.setSourceFolderUriName("my-news-folder");

        assertEquals("my-news-folder", args.getSourceFolderUriName());
    }

    @Test
    public void setAndGetSourceFolderNodeType() {
        FolderActionDocumentArguments args = new FolderActionDocumentArguments();
        args.setSourceFolderNodeType("hippostd:folder");

        assertEquals("hippostd:folder", args.getSourceFolderNodeType());
    }

    @Test
    public void settersAreIndependent_settingOneFieldDoesNotAffectOthers() {
        FolderActionDocumentArguments args = new FolderActionDocumentArguments();
        args.setSourceFolderIdentifier("id-1");

        assertNull(args.getSourceFolderName());
        assertNull(args.getSourceFolderUriName());
        assertNull(args.getSourceFolderNodeType());
    }

    @Test
    public void allFieldsCanBeOverwritten() {
        FolderActionDocumentArguments args = new FolderActionDocumentArguments();
        args.setSourceFolderName("First");
        args.setSourceFolderName("Second");

        assertEquals("Second", args.getSourceFolderName());
    }

    @Test
    public void serializationRoundTrip_preservesAllFields() throws Exception {
        FolderActionDocumentArguments original = new FolderActionDocumentArguments();
        original.setSourceFolderIdentifier("id-42");
        original.setSourceFolderName("Events");
        original.setSourceFolderUriName("events");
        original.setSourceFolderNodeType("hippostd:folder");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(original);
        }

        FolderActionDocumentArguments deserialized;
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            deserialized = (FolderActionDocumentArguments) ois.readObject();
        }

        assertEquals(original.getSourceFolderIdentifier(), deserialized.getSourceFolderIdentifier());
        assertEquals(original.getSourceFolderName(), deserialized.getSourceFolderName());
        assertEquals(original.getSourceFolderUriName(), deserialized.getSourceFolderUriName());
        assertEquals(original.getSourceFolderNodeType(), deserialized.getSourceFolderNodeType());
    }
}

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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UUIDUtilsTest {

    @Test
    void isValidPattern_withValidLowercaseUUID_returnsTrue() {
        assertTrue(UUIDUtils.isValidPattern("550e8400-e29b-41d4-a716-446655440000"));
    }

    @Test
    void isValidPattern_withValidUppercaseUUID_returnsTrue() {
        assertTrue(UUIDUtils.isValidPattern("550E8400-E29B-41D4-A716-446655440000"));
    }

    @Test
    void isValidPattern_withValidMixedCaseUUID_returnsTrue() {
        assertTrue(UUIDUtils.isValidPattern("550e8400-E29B-41d4-a716-446655440000"));
    }

    @Test
    void isValidPattern_withAllZerosUUID_returnsTrue() {
        assertTrue(UUIDUtils.isValidPattern("00000000-0000-0000-0000-000000000000"));
    }

    @Test
    void isValidPattern_withAllFsUUID_returnsTrue() {
        assertTrue(UUIDUtils.isValidPattern("ffffffff-ffff-ffff-ffff-ffffffffffff"));
    }

    @Test
    void isValidPattern_withNull_returnsFalse() {
        assertFalse(UUIDUtils.isValidPattern(null));
    }

    @Test
    void isValidPattern_withEmptyString_returnsFalse() {
        assertFalse(UUIDUtils.isValidPattern(""));
    }

    @Test
    void isValidPattern_withBlankString_returnsFalse() {
        // StringUtils.isEmpty(" ") is false, but pattern won't match whitespace
        assertFalse(UUIDUtils.isValidPattern("   "));
    }

    @Test
    void isValidPattern_withPlainText_returnsFalse() {
        assertFalse(UUIDUtils.isValidPattern("not-a-uuid"));
    }

    @Test
    void isValidPattern_withTooShortFirstSegment_returnsFalse() {
        // First segment is 7 hex chars instead of 8
        assertFalse(UUIDUtils.isValidPattern("550e840-e29b-41d4-a716-446655440000"));
    }

    @Test
    void isValidPattern_withInvalidHexCharacter_returnsFalse() {
        // 'g' is not a valid hex character
        assertFalse(UUIDUtils.isValidPattern("550e8400-e29b-41d4-a716-44665544000g"));
    }

    @Test
    void isValidPattern_withMissingDashes_returnsFalse() {
        assertFalse(UUIDUtils.isValidPattern("550e8400e29b41d4a716446655440000"));
    }

    @Test
    void isValidPattern_withExtraSegment_returnsFalse() {
        assertFalse(UUIDUtils.isValidPattern("550e8400-e29b-41d4-a716-446655440000-extra"));
    }
}

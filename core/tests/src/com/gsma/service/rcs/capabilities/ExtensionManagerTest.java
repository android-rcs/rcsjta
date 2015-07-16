/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.gsma.service.rcs.capabilities;

import com.gsma.rcs.core.ims.service.extension.ExtensionManager;

import java.util.HashSet;
import java.util.Set;

import android.test.AndroidTestCase;

public class ExtensionManagerTest extends AndroidTestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testGetExtensionsMultipleEntriesInSet() {
        Set<String> extensions = new HashSet<String>();
        extensions.add("extension1");
        extensions.add("extension2");
        extensions.add("extension3");
        String concatenatedExtensions = ExtensionManager.getExtensions(extensions);
        assertTrue(concatenatedExtensions.matches("extension[1-3];extension[1-3];extension[1-3]"));
        Set<String> newExtensions = ExtensionManager.getExtensions(concatenatedExtensions);
        assertEquals(newExtensions, extensions);
    }

    public void testGetExtensionsEmptyTokens() {
        Set<String> newExtensions = ExtensionManager.getExtensions("; ;");
        assertTrue(newExtensions.isEmpty());
    }

    public void testGetExtensionsEmptySet() {
        Set<String> extensions = new HashSet<String>();
        assertEquals("", ExtensionManager.getExtensions(extensions));
    }

    public void testGetExtensionsSingleEntryInSet() {
        Set<String> extensions = new HashSet<String>();
        extensions.add("extension1");
        assertEquals("extension1", ExtensionManager.getExtensions(extensions));
    }

}

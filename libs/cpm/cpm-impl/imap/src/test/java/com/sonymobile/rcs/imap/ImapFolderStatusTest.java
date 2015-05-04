/*
 * Copyright (C) 2015 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.sonymobile.rcs.imap;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ImapFolderStatusTest {

    @Test
    public void testBasicCoverage() {
        ImapFolderStatus status = new ImapFolderStatus("foldername");
        status.setExists(3);
        assertEquals(3, status.getExists());
        status.setNextUid(123);
        assertEquals(123, status.getNextUid());
        status.setRecent(4);
        assertEquals(4, status.getRecent());
        status.setUidValidity(1112223334);
        assertEquals(1112223334, status.getUidValidity());
        status.setFieldValue(ImapFolderStatus.StatusField.UNSEEN, 5);
        assertEquals(5, status.getUnseen());
        status.setUnseen(6);
        assertEquals(6, status.getUnseen());
    }

}

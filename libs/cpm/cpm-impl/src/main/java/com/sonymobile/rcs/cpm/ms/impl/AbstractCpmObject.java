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

package com.sonymobile.rcs.cpm.ms.impl;

import com.sonymobile.rcs.cpm.ms.CpmObject;
import com.sonymobile.rcs.cpm.ms.CpmObjectFolder;
import com.sonymobile.rcs.cpm.ms.SessionHistoryFolder;
import com.sonymobile.rcs.imap.ImapException;
import com.sonymobile.rcs.imap.ImapMessage;
import com.sonymobile.rcs.imap.ImapService;

import java.io.IOException;

public class AbstractCpmObject implements CpmObject {

    private final ImapMessage mImapMessage;

    private final MessageFolderImpl mFolder;

    protected AbstractCpmObject(ImapMessage msg, MessageFolderImpl folder) {
        mImapMessage = msg;
        this.mFolder = folder;
    }

    @Override
    public int getStorageId() {
        return mImapMessage.getUid();
    }

    @Override
    public CpmObjectFolder getFolder() {
        return mFolder;
    }

    @Override
    public String getContributionId() {
        if (mFolder instanceof SessionHistoryFolder)
            return mFolder.getName();
        else
            return null;
    }

    @Override
    public String getConversationId() {
        if (mFolder instanceof SessionHistoryFolder)
            return mFolder.getParent().getName();
        else
            return mFolder.getName();
    }

    public ImapMessage getImapMessage() {
        return mImapMessage;
    }

    protected ImapService getImapService() throws IOException, ImapException {
        mFolder.select();
        return mFolder.getImapService();
    }

}

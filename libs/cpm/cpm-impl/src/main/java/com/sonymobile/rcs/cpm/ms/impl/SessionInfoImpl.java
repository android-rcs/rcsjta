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

import com.sonymobile.rcs.cpm.ms.SessionInfo;
import com.sonymobile.rcs.imap.ImapMessage;

public class SessionInfoImpl extends AbstractCpmObject implements SessionInfo {

    protected SessionInfoImpl(ImapMessage msg, MessageFolderImpl folder) {
        super(msg, folder);
    }

    @Override
    public String getSubject() {
        return getImapMessage().getSubject();
    }

    @Override
    public String getFrom() {
        return getImapMessage().getFrom();
    }

    @Override
    public long getDate() {
        return getImapMessage().getDateAsDate();
    }

    @Override
    public String getInReplyToContributionId() {
        return getImapMessage().getInReplyToContributionId();
    }

}

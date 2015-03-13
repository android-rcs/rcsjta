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

import com.sonymobile.rcs.cpm.ms.CpmMessageStoreException;
import com.sonymobile.rcs.cpm.ms.RootFolder;
import com.sonymobile.rcs.imap.ImapFolder;
import com.sonymobile.rcs.imap.ImapService;

import java.util.Collection;

/**
 * This is a utility class not meant to be used outside this library.
 */
public class BasicFolderNode implements FolderNode {

    private FolderNode parent;

    private final ImapFolder imapFolder;

    private Collection<? extends FolderNode> subFolders;

    public BasicFolderNode(FolderNode parent, ImapFolder folder) {
        super();
        this.parent = parent;
        this.imapFolder = folder;
    }

    @Override
    public ImapFolder getImapFolder() {
        return imapFolder;
    }

    @Override
    public String getName() {
        return imapFolder.getName();
    }

    @Override
    public String getPath() {
        return imapFolder.getFullPath();
    }

    @Override
    public Collection<? extends FolderNode> getSubFolders() throws CpmMessageStoreException {
        return subFolders;
    }

    public void setSubFolders(Collection<? extends FolderNode> subFolders) {
        this.subFolders = subFolders;
    }

    @Override
    public FolderNode getParent() {
        return parent;
    }

    // protected String getSubFolderPath(String folderName) {
    //
    // return path + getSeparator() + folderName;
    // }

    @Override
    public RootFolder getRoot() {
        if (this instanceof RootFolder)
            return (RootFolder) this;
        return parent.getRoot();
    }

    // TODO find alternative
    @Override
    public ImapService getImapService() {
        return ((CommonMessageStoreImpl) getRoot().getMessageStore()).getImapService();
    }

}

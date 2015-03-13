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
 * This is a basic node interface for navigation facility
 */
public interface FolderNode {

    /**
     * @return the associated imap folder
     */
    public ImapFolder getImapFolder();

    /**
     * Returns the underlying Imap Service that was used to read this folder.
     * 
     * @return
     */
    public ImapService getImapService();

    /**
     * Convenience method. Same as {@link #getImapFolder()}.getPath() Returns the path name with the
     * correct separator
     * 
     * @return
     */
    public String getPath();

    /**
     * Will recursively point to the root ( node with no parent )
     * 
     * @return
     */
    public RootFolder getRoot();

    public FolderNode getParent();

    /**
     * Convenience method. Same as {@link #getImapFolder()}.getName() The folder name
     * 
     * @return
     */
    public String getName();

    /**
     * The subfolders
     * 
     * @return
     * @throws CpmMessageStoreException
     */
    public Collection<? extends FolderNode> getSubFolders() throws CpmMessageStoreException;

}

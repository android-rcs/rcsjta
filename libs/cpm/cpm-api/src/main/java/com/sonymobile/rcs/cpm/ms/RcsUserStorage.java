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

package com.sonymobile.rcs.cpm.ms;

import java.util.Set;

public interface RcsUserStorage extends RootFolder {

    /**
     * Returns all the user folders
     * 
     * @return the user folders
     * @throws MessageStoreException
     */
    public Set<UserFolder> getUserFolders() throws CpmMessageStoreException;

    /**
     * Creates a user folder
     * 
     * @param folderName the name of the folder
     * @return the user folder created
     * @throws MessageStoreException
     */
    public UserFolder addUserFolder(String folderName) throws CpmMessageStoreException;

    /**
     * Returns the user folder by name
     * 
     * @param folderName
     * @return the user folder
     * @throws MessageStoreException
     */
    public UserFolder getUserFolder(String folderName) throws CpmMessageStoreException;

    /**
     * Remove a conversation or a user folder
     * 
     * @param conversationOrUserFolder the folder to remove
     * @throws MessageStoreException
     */
    public void remove(CpmObjectFolder conversationOrUserFolder) throws CpmMessageStoreException;

}

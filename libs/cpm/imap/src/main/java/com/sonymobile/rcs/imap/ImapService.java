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

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

public interface ImapService extends Closeable {

    public String AUTH_MECHANISM_PLAIN = "PLAIN";

    public void init() throws IOException, ImapException;

    public boolean isUidMode();

    public void setUidMode(boolean uidmode);

    public void setFetchMetadataBody(boolean metadataBody);

    public boolean isFetchMetadataBody();

    public void setFetchMetadataEnvelope(boolean metadataEnvelope);

    public boolean isFetchMetadataEnvelope();

    public void setAuthenticationDetails(String username, String password, String mechanism,
            String token, boolean useSASL);

    public boolean isAvailable();

    // /public IOService getIOService();

    /**
     * Will call NOOP. Reset automatic logout. Returns last changes.
     * 
     * @return
     * @throws IOException
     * @throws ImapException
     */
    public ImapFolderStatus noop() throws IOException, ImapException;

    public void login() throws IOException, ImapException;

    public void startTLS() throws IOException, ImapException;

    public void logout() throws IOException, ImapException;

    public void unselect() throws IOException, ImapException;

    public void expunge() throws IOException, ImapException;

    public boolean create(String newFolder) throws IOException, ImapException;

    public boolean delete(String folderPath) throws IOException, ImapException;

    public boolean rename(String oldName, String newName) throws IOException, ImapException;

    public ImapFolderStatus select(String folderPath) throws IOException, ImapException;

    public ImapFolderStatus getFolderStatus(String folderPath,
            ImapFolderStatus.StatusField... fields) throws IOException, ImapException;

    public List<ImapFolder> getFolders(String parentPath, boolean recursive) throws IOException,
            ImapException;

    public ImapNamespace getPersonalNamespace() throws ImapException, IOException;

    /**
     * Retrieves the list of capabilities. Always returns the same list for one server so the values
     * can be cached permanently.
     * http://www.iana.org/assignments/imap-capabilities/imap-capabilities.xhtml
     * 
     * @return
     * @throws IOException
     */
    public List<String> getCapabilities() throws IOException, ImapException;

    /**
     * Helper method. Same as {@link #getCapabilities()}.contains(string)
     * 
     * @param cname
     * @return true if the remote server supports that capability
     * @throws IOException
     * @throws ImapException
     */
    public boolean isCapabilitySupported(String cname) throws ImapException, IOException;

    public ImapFolderStatus examine(String folderPath) throws IOException, ImapException;

    /**
     * Returns SORTED ids of the search
     * 
     * @param search
     * @return
     * @throws IOException
     * @throws ImapException
     */
    public int[] searchMessages(Search search) throws IOException, ImapException;

    /**
     * CPM : When a Message Storage Client needs to store a message object, a file transfer history
     * object or a standalone Media Object into a folder on the Message Storage Server, the Message
     * Storage Client SHALL send to the Message Storage Server an APPEND request as defined in
     * [RFC3501] including the name of the folder and the data of the message object, file transfer
     * history object or standalone Media Object data. The Message Storage Client MAY include an
     * initial set of metadata flags in the APPEND request towards the Message Storage Server, as
     * defined in [RFC3501]. The Message Storage Client also MAY include a set of metadata
     * annotations in the APPEND request towards the Message Storage Server, as defined in
     * [RFC5257].
     */
    public int append(String folderPath, List<Flag> flags, Part part) throws IOException,
            ImapException;

    public ImapMessage fetchMessageById(int id) throws IOException, ImapException;

    /**
     * Returns messages with same order as they are returned by the server.
     * 
     * @param string
     * @return
     * @throws IOException
     * @throws ImapException
     */
    public List<ImapMessage> fetchMessages(String string) throws IOException, ImapException;

    // public Part fetchMessageBody(int id) throws IOException, IMAPException;

    public ImapMessageMetadata fetchMessageMetadataById(int id) throws IOException, ImapException;

    public List<ImapMessageMetadata> fetchMessageMetadataList(String spec) throws IOException,
            ImapException;

    public void addFlags(Object uid, Flag... flags) throws IOException, ImapException;

    public void removeFlags(Object uid, Flag... flags) throws IOException, ImapException;

    public boolean copy(Object uid, String destinationPath) throws IOException, ImapException;

    public void setIdling(boolean idle) throws IOException, ImapException;

    public boolean isIdling();

    public long getLastTimeRequested();

    public String getFolderMetadata(String folderPath) throws IOException, ImapException;

    public void setFolderMetadata(String folderPath, String comment) throws IOException,
            ImapException;

    public ImapFolder getRootFolder(String path);

}

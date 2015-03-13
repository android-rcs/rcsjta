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

package com.sonymobile.rcs.cpm.ms.impl.mock;

import com.sonymobile.rcs.imap.Flag;
import com.sonymobile.rcs.imap.ImapException;
import com.sonymobile.rcs.imap.ImapFolder;
import com.sonymobile.rcs.imap.ImapFolderStatus;
import com.sonymobile.rcs.imap.ImapMessage;
import com.sonymobile.rcs.imap.ImapMessageMetadata;
import com.sonymobile.rcs.imap.ImapNamespace;
import com.sonymobile.rcs.imap.ImapService;
import com.sonymobile.rcs.imap.Part;
import com.sonymobile.rcs.imap.Search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MockIMAPService implements ImapService {

    private boolean connected;

    private boolean loggedIn;

    private boolean uidMode;

    private String selectedFolder;

    private List<String> capabilities;

    private List<String> folders = new ArrayList<String>();

    private static int generatedId = 0;

    private ImapNamespace personalNamespace;

    private final List<ImapMessage> messages = new ArrayList<ImapMessage>();

    @Override
    public ImapFolder getRootFolder(String path) {
        ImapFolder f = new ImapFolder(path, path, getSeparator());
        return f;
    }

    @Override
    public boolean isCapabilitySupported(String cname) throws ImapException, IOException {
        return getCapabilities().contains(cname);
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    @Override
    public void init() throws IOException, ImapException {
        connected = true;
        login();
    }

    @Override
    public boolean isAvailable() {
        return connected;
    }

    @Override
    public void close() throws IOException {
        connected = false;
        loggedIn = false;
    }

    @Override
    public boolean isUidMode() {
        return uidMode;
    }

    @Override
    public void setUidMode(boolean uidmode) {
        this.uidMode = uidmode;
    }

    // @Override
    // public boolean isConnected() {
    // return connected;
    // }
    //
    // @Override
    // public void connect() throws IOException {
    // connected = true;
    // }

    @Override
    public ImapFolderStatus noop() throws IOException {
        return null;
    }

    @Override
    public void setAuthenticationDetails(String username, String password, String mechanism,
            String token, boolean useSASL) {
    }

    @Override
    public void login() throws IOException, ImapException {
        loggedIn = true;
    }

    @Override
    public void startTLS() throws IOException, ImapException {
    }

    @Override
    public void logout() throws IOException, ImapException {
        loggedIn = false;
    }

    @Override
    public void unselect() throws IOException, ImapException {
        selectedFolder = null;
    }

    @Override
    public void expunge() throws IOException, ImapException {
    }

    @Override
    public boolean create(String newFolder) throws IOException, ImapException {
        folders.add(newFolder);
        return true;
    }

    @Override
    public boolean delete(String folderName) throws IOException, ImapException {
        folders.remove(folderName);
        return true;
    }

    @Override
    public boolean rename(String oldName, String newName) throws IOException, ImapException {
        return true;
    }

    @Override
    public ImapFolderStatus select(String folderName) throws IOException, ImapException {
        selectedFolder = folderName;
        ImapFolderStatus st = new ImapFolderStatus(selectedFolder);
        st.setExists(getMessagesInFolder(selectedFolder).size());
        return st;
    }

    public String getSelectedFolder() {
        return selectedFolder;
    }

    @Override
    public ImapFolderStatus getFolderStatus(String folderPath,
            com.sonymobile.rcs.imap.ImapFolderStatus.StatusField... fields) throws IOException,
            ImapException {
        ImapFolderStatus st = new ImapFolderStatus(folderPath);
        st.setExists(getMessagesInFolder(folderPath).size());
        return st;
    }

    @Override
    public List<ImapFolder> getFolders(String parent, boolean recursive) throws IOException,
            ImapException {
        System.out.println("getfolders " + parent + ":" + recursive + ":" + parent.length());
        List<ImapFolder> result = new ArrayList<ImapFolder>();

        for (String path : folders) {
            if (path.startsWith(parent)) {
                int i = path.lastIndexOf(getSeparator());
                String name = path.substring(i + 1);

                ImapFolder f = new ImapFolder(path, name, getSeparator());

                if ((!recursive && parent.length() == i) || recursive)
                    result.add(f);
            }

        }

        return result;
    }

    @SuppressWarnings("unused")
    private boolean isImmediateParentOf(String parent, String childpath) {
        System.out.println(parent + " is parent of " + childpath + "?");
        boolean b = parent.length() == childpath.lastIndexOf(getSeparator());
        System.out.println("" + b);
        return b;
    }

    private String getSeparator() {
        if (personalNamespace == null) {
            return "/";
        } else {
            return personalNamespace.getDelimiter();
        }
    }

    @Override
    public ImapNamespace getPersonalNamespace() {
        return personalNamespace;
    }

    public void setPersonalNamespace(ImapNamespace np) {
        this.personalNamespace = np;
    }

    @Override
    public List<String> getCapabilities() throws IOException, ImapException {
        return capabilities;
    }

    public void setCapabilities(List<String> capabilities) {
        this.capabilities = capabilities;
    }

    @Override
    public ImapFolderStatus examine(String folder) throws IOException, ImapException {
        ImapFolderStatus st = new ImapFolderStatus(folder);
        st.setExists(getMessagesInFolder(folder).size());
        return st;
    }

    @Override
    public int[] searchMessages(Search search) throws IOException, ImapException {
        return null;
    }

    @Override
    public synchronized int append(String folderPath, List<Flag> flags, Part part)
            throws IOException, ImapException {
        generatedId++;

        ImapMessageMetadata meta = new ImapMessageMetadata(generatedId);
        ImapMessage m = new ImapMessage(generatedId, meta, part);

        m.setFolderPath(folderPath);

        messages.add(m);

        return generatedId;
    }

    @Override
    public ImapMessage fetchMessageById(int id) throws IOException, ImapException {
        for (ImapMessage m : messages) {
            if (m.getUid() == id)
                return m;
        }
        return null;
    }

    // @Override
    // public Part fetchMessageBody(int id) throws IOException, IMAPException {
    // for (IMAPMessage m : messages) {
    // if (m.getUid() == id) return m.getBody();
    // }
    // return null;
    // }

    @Override
    public ImapMessageMetadata fetchMessageMetadataById(int id) throws IOException, ImapException {
        for (ImapMessage m : messages) {
            if (m.getUid() == id)
                return m.getMetadata();
        }
        return null;
    }

    @Override
    public List<ImapMessageMetadata> fetchMessageMetadataList(String spec) throws IOException,
            ImapException {
        if (selectedFolder == null)
            throw new ImapException("no selected folder");
        List<ImapMessageMetadata> li = new ArrayList<ImapMessageMetadata>();

        if (spec.equals("1:*")) {

            for (ImapMessage msg : messages) {
                if (selectedFolder.equals(msg.getFolderPath())) {
                    li.add(msg.getMetadata());
                }
            }
        }

        return li;
    }

    private List<ImapMessage> getMessagesInFolder(String folderName) {
        List<ImapMessage> li = new ArrayList<ImapMessage>();
        for (ImapMessage msg : messages) {
            if (folderName.equals(msg.getFolderPath())) {
                li.add(msg);
            }
        }
        return li;
    }

    @Override
    public void addFlags(Object uid, Flag... flags) throws IOException, ImapException {
    }

    @Override
    public void removeFlags(Object uid, Flag... flags) throws IOException, ImapException {
    }

    @Override
    public boolean copy(Object uid, String folder) throws IOException, ImapException {
        return false;
    }

    @Override
    public void setIdling(boolean idle) throws IOException, ImapException {
    }

    @Override
    public boolean isIdling() {
        return false;
    }

    @Override
    public long getLastTimeRequested() {
        return 0;
    }

    @Override
    public String getFolderMetadata(String fullName) throws IOException, ImapException {
        return null;
    }

    @Override
    public void setFolderMetadata(String fullName, String comment) throws IOException,
            ImapException {
    }

    @Override
    public List<ImapMessage> fetchMessages(String string) throws IOException, ImapException {
        if (string.equals("1:*")) {
            return getMessagesInFolder(selectedFolder);
        } else {
            Integer uid = Integer.parseInt(string);
            for (ImapMessage m : messages) {
                if (m.getUid() == uid) {
                    return Arrays.asList(m);
                }
            }
        }
        return null;
    }

    @Override
    public void setFetchMetadataBody(boolean metadataBody) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isFetchMetadataBody() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setFetchMetadataEnvelope(boolean metadataEnvelope) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isFetchMetadataEnvelope() {
        // TODO Auto-generated method stub
        return false;
    }

}

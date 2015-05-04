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

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default IMAP client implementation. Must take a IoService for its input/output operations.
 */
public class DefaultImapService implements ImapService {

    private final IoService mIoService;

    private int mTagIndex;

    private String mCurrentTag;

    private List<String> mCapabilities;

    private ImapNamespace mPersonalNamespace;// , othersNamespace, publicNamespace;

    private boolean mIdling;

    private ImapFolderStatus mSelectedFolderStatus;

    private long mLastTimeRequested;

    private boolean mUidMode = true;

    private boolean mMetadataEnvelope;

    private boolean mMetadataBody;

    // credentials
    private String mUsername, mPassword, mMechanism, mToken;

    private boolean mUseSASL;

    private boolean mAutoconnect = true;

    private static Logger sLogger = Logger.getLogger(DefaultImapService.class.getName());

    public DefaultImapService(IoService ioService) {
        this.mIoService = ioService;
        if (this.mIoService == null)
            throw new NullPointerException();
    }

    private String getPathSeparator(String path) {
        return mPersonalNamespace.getDelimiter();
    }

    private synchronized Part readPart(String a) throws IOException, ImapException {
        // Example : * 3 FETCH (BODY[] {55}

        int size = Integer.parseInt(a.substring(a.indexOf('{') + 1, a.indexOf('}')));
        if (sLogger.isLoggable(Level.FINE)) {
            sLogger.fine("Reading " + size + " characters");
        }
        StringBuilder sb = new StringBuilder();

        int read = 0;

        while (read < size) {
            String line = ioReadLine();
            read += line.length() + 2;
            sb.append(line);
            sb.append(ImapUtil.CRLF);
        }

        String payload = sb.toString();

        Part p = new Part();

        p.fromPayload(payload);

        return p;
    }

    private boolean isUntagged(String resp) {
        return resp.startsWith("* ");
    }

    private void checkCapability(String string) throws ImapException {
        if (!mCapabilities.contains(string)) {
            throw new CapabilityNotSupportedException(string);
        }
    }

    private int getUidPlus(String response) {
        if (uidPlusSupported()) {
            int i = response.indexOf('[');
            int j = response.indexOf(']');
            String[] arr = response.substring(i + 1, j).split(" ");
            int id = Integer.parseInt(arr[2]);
            return id;
        } else {
            return -1;
        }
    }

    private boolean uidPlusSupported() {
        return mCapabilities.contains("UIDPLUS");
    }

    private void updateLastTimeRequested() {
        mLastTimeRequested = System.currentTimeMillis();
    }

    private synchronized String ioReadLine() throws IOException, ImapException {
        autoConnect();
        return mIoService.readLine();
    }

    private synchronized void ioWriteln(String payload) throws IOException, ImapException {
        mIoService.writeln(payload);
    }

    private void ioStartTLSHandshake() throws IOException {
        mIoService.startTLSHandshake();
    }

    private synchronized void autoConnect() throws IOException, ImapException {
        if (mAutoconnect && !mIoService.isConnected()) {
            mSelectedFolderStatus = null;

            mIoService.connect();
            String welcome = mIoService.readLine();
            if (sLogger.isLoggable(Level.FINE)) {
                sLogger.fine("IMAP Server Welcome message : " + welcome);
            }
            login();
        }
    }

    private synchronized void initNamespace() throws IOException, ImapException {
        checkCapability("NAMESPACE");

        writeCommand("NAMESPACE");
        String spec = ioReadLine().trim().replace("NIL", "()"); // remove '* NAMESPACE'
        ioReadLine();
        spec = spec.substring(13, spec.length() - 1);

        String[] array = spec.split("\\) \\(");
        if (array.length > 0 && array[0].length() > 0)
            mPersonalNamespace = new ImapNamespace(array[0]);

        // ignore other namespaces
        // if (array.length>1 && array[1].length() > 0) othersNamespace = new
        // IMAPNamespace(array[1]);
        // if (array.length>2 && array[2].length() > 0) publicNamespace = new
        // IMAPNamespace(array[2]);
    }

    private String uidTag() {
        if (isUidMode())
            return "UID ";
        else
            return "";
    }

    @SuppressWarnings("unchecked")
    private List<String> readToEndOfResponse() throws IOException, ImapException {
        String ok = mCurrentTag + " OK";
        String l = ioReadLine();

        checkResponseNotBad(l);

        List<String> response = null;
        while (!l.startsWith(ok)) {
            if (response == null) {
                response = new ArrayList<String>();
            }
            response.add(l);
            l = ioReadLine();
        }
        if (response == null) {
            response = Collections.EMPTY_LIST;
        }

        return response;
    }

    private void checkResponseNotBad(String line) throws ImapException {
        boolean b = line.startsWith(mCurrentTag + " BAD") || line.startsWith(mCurrentTag + " NO");
        if (b) {
            throw new ImapException("Server message : " + line);
        }
    }

    private boolean checkResponseOk(String line) {
        return line.startsWith(mCurrentTag + " OK");
    }

    private boolean isTagged(String line) {
        return line.startsWith(mCurrentTag + " ");
    }

    private void writeCommand(String... command) throws IOException, ImapException {
        autoConnect();
        StringBuilder sb = new StringBuilder();

        sb.append("a");
        sb.append(mTagIndex++);
        mCurrentTag = sb.toString();

        for (String c : command) {
            if (c == null || c.length() == 0)
                continue;
            sb.append(" ");
            sb.append(c);
        }
        String s = sb.toString();

        ioWriteln(s);
        updateLastTimeRequested();
    }

    /**
     * Generic request with ok/nok response. Returns true if success. To be used internally.
     * 
     * @param command
     * @return
     * @throws IOException
     * @throws ImapException
     */
    private synchronized boolean simpleCommand(String... command) throws IOException, ImapException {
        writeCommand(command);
        String response = null;
        while (true) {
            response = ioReadLine();
            if (isTagged(response)) {
                break;
            }
        }
        checkResponseNotBad(response);
        return checkResponseOk(response);
    }

    /**
     * Only use for testing
     */
    protected void resetLastTimeRequested() {
        mLastTimeRequested = 0;
    }

    @Override
    public ImapFolder getRootFolder(String path) {
        return new ImapFolder(path, path, getPathSeparator(path));
    }

    public void setAutoconnect(boolean autoconnect) {
        this.mAutoconnect = autoconnect;
    }

    public boolean isAutoconnect() {
        return mAutoconnect;
    }

    @Override
    public synchronized void init() throws IOException, ImapException {
        autoConnect();
    }

    public IoService getIoService() {
        return mIoService;
    }

    @Override
    public ImapNamespace getPersonalNamespace() throws ImapException, IOException {
        return mPersonalNamespace;
    }

    @Override
    public List<ImapFolder> getFolders(String parentPath, boolean recursive) throws IOException,
            ImapException {
        String p = "";
        if (parentPath != null && parentPath.length() > 0) {
            p = parentPath + getPathSeparator(parentPath);
        }

        if (recursive) {
            p += "*";
        } else {
            p += "%";
        }

        writeCommand("LIST", "\"\"", "\"" + p + "\"");
        List<String> li = readToEndOfResponse();
        List<ImapFolder> folders = new ArrayList<ImapFolder>();
        for (String spec : li) {
            spec = spec.substring(6).trim();
            String s = spec.substring(0, spec.length() - 1);// remove * LIST, last "

            int i = s.lastIndexOf('"');
            String path = s.substring(i + 1);

            String name = path;
            // FIXME when personal namespace is null
            String delimiter = getPersonalNamespace().getDelimiter();

            int delimIndex = name.lastIndexOf(delimiter);
            if (delimIndex != -1) {
                name = name.substring(delimIndex + 1);
            }

            folders.add(new ImapFolder(path, name, delimiter));
        }
        return folders;
    }

    @Override
    public synchronized boolean create(String newFolder) throws IOException, ImapException {
        return simpleCommand("CREATE", newFolder);
    }

    @Override
    public synchronized boolean delete(String folderName) throws IOException, ImapException {
        try {
            return simpleCommand("DELETE", folderName);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public synchronized boolean rename(String oldName, String newName) throws IOException,
            ImapException {
        return simpleCommand("RENAME", oldName, newName);
    }

    @Override
    public boolean isCapabilitySupported(String cname) throws ImapException, IOException {
        return getCapabilities().contains(cname);
    }

    public void clearCapabilities() {
        this.mCapabilities = null;
    }

    @Override
    public synchronized List<String> getCapabilities() throws ImapException, IOException {
        if (mCapabilities != null)
            return mCapabilities;

        writeCommand("CAPABILITY");

        mCapabilities = new ArrayList<String>();

        String resp = null;

        while (true) {
            resp = ioReadLine();
            if (resp.startsWith("* CAPABILITY")) {
                break;
            } else if (isUntagged(resp)) {
                continue;
            } else {
                throw new ImapException("CAPABILITY error : " + resp);
            }
        }

        StringTokenizer stk = new StringTokenizer(resp);
        stk.nextToken(); // *
        stk.nextToken(); // CAPABILITY

        while (stk.hasMoreElements()) {
            String e = stk.nextToken();
            mCapabilities.add(e);
        }

        checkResponseOk(ioReadLine());

        return new ArrayList<String>(mCapabilities);
    }

    @Override
    public synchronized void unselect() throws IOException, ImapException {
        checkCapability("UNSELECT");
        simpleCommand("UNSELECT");
        mSelectedFolderStatus = null;
    }

    @Override
    public synchronized ImapFolderStatus select(String folderName) throws IOException,
            ImapException {
        if (mSelectedFolderStatus != null
                && folderName.equals(mSelectedFolderStatus.getFolderName())) {
            return mSelectedFolderStatus;
        }
        writeCommand("SELECT " + folderName);
        List<String> li = readToEndOfResponse();
        if (li == null || li.size() == 0) {
            throw new ImapException("Select folder wont return any status : " + folderName);
        }
        mSelectedFolderStatus = new ImapFolderStatus(folderName, li);
        return mSelectedFolderStatus;
    }

    @Override
    public synchronized ImapFolderStatus getFolderStatus(String folderName,
            ImapFolderStatus.StatusField... fields) throws IOException {
        try {
            // A042 STATUS blurdybloop (UIDNEXT MESSAGES)
            StringBuilder sb = new StringBuilder();

            if (fields == null || fields.length == 0) { // all fields
                fields = ImapFolderStatus.StatusField.values();
            }

            sb.append('(');
            for (int i = 0; i < fields.length; i++) {
                if (i > 0)
                    sb.append(' ');
                sb.append(fields[i].toString());
            }
            sb.append(')');

            writeCommand("STATUS", folderName, sb.toString());
            // * STATUS "INBOX" (UIDNEXT 17 UIDVALIDITY 229669678)
            String result = ioReadLine();
            if (result.startsWith("* STATUS")) {
                ioReadLine(); // END OF COMMAND

                ImapFolderStatus fs = new ImapFolderStatus(folderName);
                int i = result.indexOf('(');
                int j = result.indexOf(')');
                result = result.substring(i + 1, j);
                String[] data = result.split(" ");
                for (int k = 0; k < (data.length - 1); k += 2) {
                    ImapFolderStatus.StatusField sf = ImapFolderStatus.StatusField.valueOf(data[k]);
                    int v = Integer.valueOf(data[k + 1]);
                    fs.setFieldValue(sf, v);
                }

                return fs;
            }
        } catch (IOException ioe) {
            throw ioe;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public synchronized void logout() throws IOException, ImapException {
        simpleCommand("LOGOUT");
    }

    @Override
    public synchronized void expunge() throws IOException, ImapException {
        simpleCommand("EXPUNGE");
    }

    @Override
    public synchronized ImapFolderStatus noop() throws IOException, ImapException {
        writeCommand("NOOP");
        List<String> li = readToEndOfResponse();
        if (li != null && mSelectedFolderStatus != null) {
            String selectedFolderName = mSelectedFolderStatus.getFolderName();
            return new ImapFolderStatus(selectedFolderName, li);
        }
        return null;
    }

    @Override
    public synchronized ImapFolderStatus examine(String folder) throws IOException, ImapException {
        writeCommand("EXAMINE", folder);
        List<String> li = readToEndOfResponse();
        return new ImapFolderStatus(folder, li);
    }

    @Override
    public synchronized int[] searchMessages(Search search) throws IOException, ImapException {
        writeCommand(uidTag() + "SEARCH" + search.toString());
        String line = ioReadLine();
        checkResponseNotBad(line);

        boolean ok = checkResponseOk(ioReadLine());

        int index = line.indexOf("SEARCH") + 6;
        if (ok && line.length() > index) {
            String[] arr = line.substring(index).trim().split(" ");
            int[] li = new int[arr.length];
            for (int i = 0; i < li.length; i++) {
                li[i] = Integer.parseInt(arr[i]);
            }
            Arrays.sort(li);
            return li;
        } else {
            // search is empty
            return new int[0];
        }
    }

    @Override
    public synchronized void close() throws IOException {
        if (mIoService.isConnected()) {
            mIoService.close();
        }
    }

    @Override
    public synchronized boolean isAvailable() {
        return mIoService != null && mIoService.isConnected();
    }

    @Override
    public void setFetchMetadataBody(boolean metadataBody) {
        this.mMetadataBody = metadataBody;
    }

    @Override
    public void setFetchMetadataEnvelope(boolean metadataEnvelope) {
        this.mMetadataEnvelope = metadataEnvelope;
    }

    @Override
    public boolean isFetchMetadataBody() {
        return mMetadataBody;
    }

    @Override
    public boolean isFetchMetadataEnvelope() {
        return mMetadataEnvelope;
    }

    @Override
    public boolean isUidMode() {
        return mUidMode;
    }

    @Override
    public void setUidMode(boolean uidmode) {
        this.mUidMode = uidmode;
    }

    @Override
    public long getLastTimeRequested() {
        return mLastTimeRequested;
    }

    @Override
    public synchronized void setAuthenticationDetails(String username, String password,
            String mechanism, String token, boolean useSASL) {
        this.mUsername = username;
        this.mPassword = password;
        this.mMechanism = mechanism;
        this.mToken = token;
        this.mUseSASL = useSASL;
    }

    @Override
    public synchronized void login() throws IOException, ImapException {
        if (mUseSASL) {
            if (mMechanism == null) {
                mMechanism = AUTH_MECHANISM_PLAIN;
            }

            String token = mToken;

            if (token == null) {
                token = "\0" + mUsername + "\0" + mPassword;
                token = ImapUtil.encodeBase64(token.getBytes());
            }

            writeCommand("AUTHENTICATE", mMechanism);
            String l = ioReadLine();
            if (l.startsWith("+")) {
                ioWriteln(token);
            } else {
                checkResponseNotBad(l);
                throw new ImapException("Authentication not supported : " + mMechanism);
            }
        } else {
            writeCommand("LOGIN", mUsername, mPassword);
        }

        String resp = ioReadLine();
        checkResponseNotBad(resp);
        // login executed successfully
        if (mCapabilities == null) {
            getCapabilities();
        }
        if (mPersonalNamespace == null && isCapabilitySupported("NAMESPACE")) {
            initNamespace();
        }
    }

    @Override
    public void startTLS() throws IOException, ImapException {
        checkCapability("STARTTLS");
        boolean b = simpleCommand("STARTTLS");
        if (b) {
            ioStartTLSHandshake();
        } else {
            throw new ImapException("STARTTLS command failed");
        }
    }

    @Override
    public synchronized int append(String folderName, List<Flag> flags, Part part)
            throws IOException, ImapException {
        String payload = part.toPayload();
        // append INBOX (\Seen) {310}
        writeCommand("APPEND", folderName, ImapUtil.getFlagsAsString(flags), "{" + payload.length()
                + "}");
        String ok = ioReadLine();
        if (!ok.startsWith("+"))
            return -1;
        ioWriteln(payload + ImapUtil.CRLF);

        while (true) {
            ok = ioReadLine();
            if (isTagged(ok)) {
                break;
            }
        }

        checkResponseNotBad(ok);

        return getUidPlus(ok);
    }

    @Override
    public synchronized ImapMessage fetchMessageById(int uid) throws IOException, ImapException {
        List<ImapMessage> msgLi = fetchMessages("" + uid);
        if (msgLi.size() != 1)
            return null;

        // IMAPMessageMetadata metadata = fetchMessageMetadataById(uid);
        // IMAPMessage m = msgLi.get(0);
        // m.setMetadata(metadata);

        return msgLi.get(0);
    }

    @Override
    public List<ImapMessage> fetchMessages(String spec) throws IOException, ImapException {
        List<ImapMessage> messages = null;
        synchronized (mIoService) {
            writeCommand(uidTag() + "FETCH", "" + spec, "BODY.PEEK[]");
            messages = new ArrayList<ImapMessage>();

            while (true) {
                String a = ioReadLine();
                if (a.startsWith("* ")) {
                    int uid = -1;
                    StringTokenizer stk = new StringTokenizer(a);
                    while (stk.hasMoreTokens()) {
                        if (stk.nextToken().endsWith("UID")) {
                            uid = Integer.parseInt(stk.nextToken());
                            break;
                        }
                    }
                    Part p = readPart(a);
                    ImapMessage m = new ImapMessage(uid, null, p);
                    messages.add(m);
                } else {
                    checkResponseOk(a);
                    break;
                }

            }
        }
        return messages;
    }

    @Override
    public synchronized ImapMessageMetadata fetchMessageMetadataById(int id) throws IOException,
            ImapException {

        // (FLAGS INTERNALDATE RFC822.SIZE ENVELOPE BODY)
        // ALL (FLAGS INTERNALDATE RFC822.SIZE ENVELOPE)
        // FAST (FLAGS INTERNALDATE RFC822.SIZE)
        // FULL (FLAGS INTERNALDATE RFC822.SIZE ENVELOPE BODY)
        String macro = "FAST";
        if (mMetadataEnvelope & !mMetadataBody)
            macro = "ALL";
        else if (!mMetadataEnvelope & mMetadataBody)
            macro = "(FLAGS INTERNALDATE RFC822.SIZE BODY)";
        else if (mMetadataEnvelope & mMetadataBody)
            macro = "FULL";

        writeCommand(uidTag() + "FETCH", "" + id, macro);
        String line = ioReadLine();

        checkResponseNotBad(line);
        ImapMessageMetadata m;
        try {
            m = ImapMessageMetadata.parseMetadata(id, line);
        } catch (ParseException e) {
            throw new ImapException("Cannot parse from server: " + line);
        }
        checkResponseNotBad(ioReadLine());

        return m;
    }

    @Override
    public synchronized List<ImapMessageMetadata> fetchMessageMetadataList(String spec)
            throws IOException, ImapException {
        List<ImapMessageMetadata> li = null;
        synchronized (mIoService) {
            String macro = "FAST";
            if (mMetadataEnvelope & !mMetadataBody)
                macro = "ALL";
            else if (!mMetadataEnvelope & mMetadataBody)
                macro = "(FLAGS INTERNALDATE RFC822.SIZE BODY)";
            else if (mMetadataEnvelope & mMetadataBody)
                macro = "FULL";

            writeCommand(uidTag() + "FETCH", spec, macro);

            li = new ArrayList<ImapMessageMetadata>();
            String a = null;
            while (true) {
                a = ioReadLine();
                checkResponseNotBad(a);
                if (isTagged(a))
                    break;
                int id = Integer.parseInt(a.split(" ")[1]);
                try {
                    li.add(ImapMessageMetadata.parseMetadata(id, a));
                } catch (ParseException e) {
                    throw new ImapException("Cannot parse from server: " + a);
                }
            }
            checkResponseOk(a);
        }
        return li;
    }

    @Override
    public synchronized void addFlags(Object uid, Flag... flags) throws IOException, ImapException {
        writeCommand(uidTag() + "STORE", uid.toString(), "+FLAGS", ImapUtil.getFlagsAsString(flags));
        readToEndOfResponse();
    }

    @Override
    public synchronized void removeFlags(Object uid, Flag... flags) throws IOException,
            ImapException {
        writeCommand(uidTag() + "STORE", uid.toString(), "-FLAGS", ImapUtil.getFlagsAsString(flags));
        readToEndOfResponse();
    }

    @Override
    public synchronized boolean copy(Object uid, String folder) throws IOException, ImapException {
        return simpleCommand("COPY", uid.toString(), folder);
    }

    @Override
    public boolean isIdling() {
        return mIdling;
    }

    @Override
    public synchronized void setIdling(boolean idle) throws IOException, ImapException {
        checkCapability("IDLE");
        if (mIdling ^ idle) {
            if (idle) {
                writeCommand("IDLE");
                String line = ioReadLine();
                if (line.startsWith("+")) {
                    mIdling = true;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                while (mIdling) {
                                    // TODO NIS idle parse next line
                                    // String eventString = ioReadLine();

                                    Thread.sleep(20);
                                    synchronized (DefaultImapService.this) {
                                        DefaultImapService.this.notify();
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                        }
                    }).start();

                }
            } else {
                // when finished execute DONE
                ioWriteln("DONE");
                mIdling = false;
                try {
                    wait();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // readLine();
            }

        }
    }

    @Override
    public String getFolderMetadata(String fullName) throws IOException, ImapException {
        checkCapability("METADATA");
        // C: a GETMETADATA "INBOX" /private/comment
        // S: * METADATA "INBOX" (/private/comment "My comment")
        // S: a OK GETMETADATA complete
        String COMMENT_TYPE = "/private/comment";
        writeCommand("GETMETADATA", fullName, COMMENT_TYPE);
        String r = ioReadLine();

        checkResponseNotBad(r);

        int i = r.indexOf(COMMENT_TYPE) + COMMENT_TYPE.length();
        i = r.indexOf('"', i) + 1;
        int j = r.lastIndexOf('"');

        checkResponseOk(ioReadLine());

        return r.substring(i, j);
    }

    @Override
    public void setFolderMetadata(String fullName, String comment) throws IOException,
            ImapException {
        checkCapability("METADATA");
        // a SETMETADATA INBOX (/private/comment "My new comment")
        simpleCommand("SETMETADATA", fullName, "(/private/comment \"" + comment + "\"");
    }

}

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

package com.sonymobile.rcs.imap.mock;

import com.sonymobile.rcs.imap.IoService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.PushbackReader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockIoService implements IoService {

    private boolean DEBUG = false;

    private boolean mConnected = false;

    private PrintWriter mOut = new PrintWriter(System.out);

    private String mClientName = "C";

    private String mFolderPreffix = "";

    private String mFolderSeparator = ".";

    private String mTag;

    private String mCommand;

    private static final String CRLF = "\r\n";

    private boolean mNextResponseOk = true;

    private boolean mSayNoIfNotOk = false; // otherwise BAD

    private PushbackReader mPbReader = new PushbackReader(new StringReader(
            "* OK Mock IMAP4rev1 Server is ready." + CRLF), 2000);

    private BufferedReader mReader = new BufferedReader(mPbReader);

    private String[] mCapabilities = {
        "IMAP4rev1"
    };

    private Map<String, String[]> mNextResponsesPerCommand = new HashMap<String, String[]>();

    /**
     * Enable debug to look at input/output between "client" and "server"
     */
    public void debug() {
        DEBUG = true;
    }

    public void debugStop() {
        DEBUG = false;
    }

    public void setNextResponseOk(boolean nextResponseOk) {
        this.mNextResponseOk = nextResponseOk;
    }

    public void setSayNoIfNotOk(boolean sayNoIfNotOk) {
        this.mSayNoIfNotOk = sayNoIfNotOk;
    }

    /**
     * Insert the next reply content You can skip "* [command] " if applicable
     * 
     * @param command the command name
     * @param responses the ordered list of lines that will be replied by the server
     */
    public void setNextResponseForCommand(String command, String... responses) {
        command = command.toUpperCase();
        boolean skip = false;
        for (int i = 0; i < responses.length; i++) {
            if (skip)
                continue;
            else if (responses[i].startsWith("* ")) {
                skip = true;
                continue;
            } else {
                responses[i] = "* " + command + " " + responses[i];
            }
        }

        mNextResponsesPerCommand.put(command, responses);
    }

    public void setFolderSeparator(String folderSeparator) {
        this.mFolderSeparator = folderSeparator;
    }

    public String getFolderSeparator() {
        return mFolderSeparator;
    }

    public void setFolderPreffix(String folderPreffix) {
        this.mFolderPreffix = folderPreffix;
    }

    public String getFolderPreffix() {
        return mFolderPreffix;
    }

    public void addCapability(String c) {
        if (Arrays.asList(mCapabilities).contains(c))
            return;
        int i = mCapabilities.length;
        mCapabilities = Arrays.copyOf(mCapabilities, i + 1);
        mCapabilities[i] = c;
    }

    private String getCapabilitiesAsString() {
        String s = "";
        for (int i = 0; i < mCapabilities.length; i++) {
            s += " " + mCapabilities[i];
        }
        return s.trim();
    }

    public void addCompleted() throws IOException {
        if (mNextResponseOk)
            addNextResponse(mTag + " OK " + mCommand + " completed.");
        else if (mSayNoIfNotOk) {
            addNextResponse(mTag + " NO " + mCommand + " just failed.");
        } else if (!mSayNoIfNotOk) {
            addNextResponse(mTag + " BAD " + mCommand + " doesnt work like that.");
        }
    }

    private void addNextResponse(String... responses) throws IOException {
        List<String> li = Arrays.asList(responses);
        Collections.reverse(li);

        for (String string : li) {
            string += CRLF;
            mPbReader.unread(string.toCharArray());
        }
    }

    @Override
    public void close() throws IOException {
        mConnected = false;
    }

    @Override
    public boolean isConnected() {
        return mConnected;
    }

    @Override
    public void connect() throws IOException {
        mConnected = true;
    }

    @Override
    public String readLine() throws IOException {
        String s = mReader.readLine();
        log("S: " + s);
        return s;
    }

    @Override
    public void writeln(String payload) throws IOException {
        log(mClientName + ": " + payload);

        int i = payload.indexOf(' ');
        if (payload.length() > 3 && i != -1 && i < 20) {

            mCommand = payload.toUpperCase().substring(i + 1);
            mTag = payload.substring(0, i);
            int j = mCommand.indexOf(' ');

            if (mCommand.startsWith("UID")) {
                j = mCommand.indexOf(' ', j + 1);
            }

            if (j != -1)
                mCommand = mCommand.substring(0, j);

            if (mCommand.startsWith("CAPABILITY")) {
                addCompleted();
                addNextResponse("* CAPABILITY " + getCapabilitiesAsString());
            } else if (mCommand.startsWith("LOGIN")) {
                addCompleted();
            } else if (mCommand.startsWith("NAMESPACE")) {
                addCompleted();
                addNextResponse("* NAMESPACE ((\"" + mFolderPreffix + "\" \"" + mFolderSeparator
                        + "\")) NIL NIL");
            } else {
                addCompleted();

                String[] responses = mNextResponsesPerCommand.get(mCommand);
                if (responses != null) {
                    addNextResponse(responses);
                }
            }
        }

    }

    @Override
    public void startTLSHandshake() throws IOException {
        log("<Start TLS handshake>");
    }

    @Override
    public String read(int size) throws IOException {
        char[] arr = new char[size];
        mReader.read(arr);
        String s = new String(arr);
        log("S: " + s);
        return s;
    }

    private synchronized void log(String s) {
        if (DEBUG) {
            mOut.println(s.trim());
            mOut.flush();
        }
    }

    public void reset() {
        debugStop();
        mNextResponsesPerCommand.clear();
    }

}

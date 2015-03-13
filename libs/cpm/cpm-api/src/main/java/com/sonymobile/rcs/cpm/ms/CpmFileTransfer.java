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

/**
 * (RCS) File Transfer History Objects will always be stored in their corresponding Conversation
 * History or Session History folders (see sections 5.2.2 and 5.2.4)
 */
public interface CpmFileTransfer extends CpmObject {

    public static int AdHoc = 2;
    public static int PreDefined = 1; // not applicable for RCS
    public static int OneToOne = 0;

    /**
     * Returns the type
     * 
     * @return the type
     */
    public int getType();

    /**
     * Returns the participants
     * 
     * @return the list of participants
     */
    public Set<Participant> getParticipants();

    /**
     * Returns the files
     * 
     * @return the file items
     */
    public Set<FileItem> getFiles();

    /**
     * Returns the unique file assuming there is only one in this session
     * 
     * @return
     */
    public FileItem getFile();

    public String getFrom();

}

/*
 * Example taken from the specification: From: John Doe <jdoe@machine.example> Date: Fri, 21 Nov
 * 1997 09:55:06 -0600 Conversation-ID: f81d4fae-7dec-11d0-a765-00a0c91e6bf6 Contribution-ID:
 * abcdef-1234-5678-90ab-cdef01234567 InReplyTo-Contribution-ID:
 * 01234567-89ab-cdef-0123-456789abcdef Content-type: multipart/related;boundary=cpm;
 * type=”Application/X-CPM-File-Transfer” --cpm Content-Type: Application/X-CPM-File-Transfer
 * <file-object> <cid>cid:<1234@example.com></cid> <sdp> i=This is my latest picture a=sendonly
 * a=file-selector:name:"My picture.jpg" type:image/jpeg size:4092 a=file-disposition:render
 * a=file-date:creation:"Mon, 15 May 2006 15:01:31 +0300" </sdp> </file-object> --cpm Content-Type:
 * image/jpeg Content-Transfer-Encoding: binary Content-ID: <1234@example.com> ... My picture.jpg...
 */


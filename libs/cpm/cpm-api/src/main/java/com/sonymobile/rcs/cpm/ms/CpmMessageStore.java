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

/**
 * In RCS 5.1, a Network-based Common Message Store is used to store messages (standalone text or
 * multimedia and chat messages). An RCS user will have control over the messages to be stored in
 * their message storage. The network-based common message storage allows a user to improve their
 * organization of their stored messages. In addition to this, the Network-Based Common Message
 * Store is used to provide storage for all messages sent and received by a client supporting the
 * RCS 5.1 text and multimedia messaging service which also includes any other messages that they
 * receive. The RCS 5.1 network-based common message storage supports synchronization of stored
 * objects with the local storage in all registered RCS devices The storage is always subject to
 * operator-controlled message size and storage quota limitations. Relevant storage usage
 * information can be collected to allow a service provider to apply usage based charges. (RCSACS
 * P.193) If the Network-based Common Message Store is available, the device should synchronize with
 * the Network-based Common Message Store when a user is about to initiate a chat with another user.
 * Note, however, that this may not be desirable in roaming scenarios. Local Conversation History
 * The terminal/client supports a locally stored conversation. A Network-based Common Message Store
 * for the chat sessions may be used to synchronize the messages between devices. It also allows the
 * user to keep a back-up of important conversations in the network. In the device, alignment is
 * expected between the local Conversation History and the synchronization with the Network-based
 * Common Message Store. How this alignment is done is left up to the device implementation.
 */
public interface CpmMessageStore extends MessageStore {

    public RcsUserStorage getRcsUserStorage();

    // /**
    // * When a Message Storage Client needs to generate a reference for (part of) a message object,
    // a file transfer history object or a
    // * stand-alone Media Object, the Message Storage Client SHALL send to the Message Storage
    // Server a GENURLAUTH
    // * request as defined in [RFC4467] including an IMAP URL (as per [RFC5092] and [RFC4467])
    // pointing to the (part of) the
    // * object for which a reference needs to be created.
    // * @param referenceUrl
    // * @return
    // * @throws MessageStoreException
    // */
    // public CPMMessage fetchByReference(String referenceUrl) throws CPMMessageStoreException;
    //
    //
    // /**
    // * Generates a secure IMAP url to retrieve the message
    // * @return the url
    // * @throws MessageStoreException
    // */
    // public String generateReferenceUrl(CPMMessage message) throws CPMMessageStoreException;

}

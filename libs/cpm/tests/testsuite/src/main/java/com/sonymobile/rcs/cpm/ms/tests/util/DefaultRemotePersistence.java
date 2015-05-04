
package com.sonymobile.rcs.cpm.ms.tests.util;

import com.sonymobile.rcs.imap.Flag;
import com.sonymobile.rcs.imap.ImapFolder;
import com.sonymobile.rcs.imap.ImapMessage;
import com.sonymobile.rcs.imap.ImapMessageMetadata;
import com.sonymobile.rcs.imap.ImapService;

import java.io.IOException;
import java.util.List;

public class DefaultRemotePersistence implements RemotePersistence {

    private final ImapService imap;

    public DefaultRemotePersistence(ImapService is) {
        this.imap = is;
    }

    @Override
    public void init(String username, String password) {
        try {
            imap.init();
        } catch (Exception e) {
            throw new RuntimeException("Cannot connect to imap server, cannot start test for user "
                    + username, e);
        }

        imap.setAuthenticationDetails(username, password, null, null, false);
    }

    @Override
    public void close() throws IOException {
        imap.close();
    }

    @Override
    public void setMessageSeen(String messageId, String remoteGroupId) throws Exception {
        int uid = -1;
        ImapMessage m = getImapMessage(messageId, remoteGroupId);
        if (m != null) {
            uid = m.getUid();
        }
        if (uid == -1)
            throw new Exception("IMAP Message with ID " + messageId + " not found in session "
                    + remoteGroupId);
        imap.addFlags(uid, Flag.Seen);
    }

    @Override
    public void setMessageDeleted(String messageId, String remoteGroupId) throws Exception {
        int uid = -1;
        ImapMessage m = getImapMessage(messageId, remoteGroupId);
        if (m != null) {
            uid = m.getUid();
        }
        if (uid == -1)
            throw new Exception("IMAP Message with ID " + messageId + " not found in session "
                    + remoteGroupId);
        imap.addFlags(uid, Flag.Deleted);
    }

    @Override
    public void doExpunge() throws Exception {
        imap.expunge();
        imap.close();
    }

    @Override
    public boolean isMessageDeleted(String messageId, String sessionId) throws Exception {
        ImapMessage m = getImapMessage(messageId, sessionId);
        if (m != null) {
            return m.getMetadata().getFlags().contains(Flag.Deleted);
        } else
            return true;
    }

    @Override
    public boolean isMessageSeen(String messageId, String sessionId) throws Exception {
        ImapMessage m = getImapMessage(messageId, sessionId);
        if (m != null) {
            return m.getMetadata().getFlags().contains(Flag.Seen);
        } else
            return false;
    }

    private ImapMessage getImapMessage(String messageId, String remoteGroupId) throws Exception {
        List<ImapFolder> li = imap.getFolders("cpmdefault", true);
        String path = null;
        for (ImapFolder f : li) {
            if (f.getName().equals(remoteGroupId)) {
                path = f.getFullPath();
                break;
            }
        }
        imap.select(path.toString());
        List<ImapMessage> messages = imap.fetchMessages("1:*");
        for (ImapMessage m : messages) {
            if (messageId.equals(m.getMessageId())) {
                ImapMessageMetadata meta = imap.fetchMessageMetadataById(m.getUid());
                m.setMetadata(meta);
                return m;
            }
        }

        return null;
    }

}

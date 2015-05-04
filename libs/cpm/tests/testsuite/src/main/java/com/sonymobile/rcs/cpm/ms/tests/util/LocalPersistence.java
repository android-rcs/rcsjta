
package com.sonymobile.rcs.cpm.ms.tests.util;



/**
 * 
 * 
 * @author 23054187
 *
 */
public interface LocalPersistence {
    
    public void clearGroups();

    public void clearFileTransfers();

    public void clearMessages();

    public void deleteChatMessage(String messageId);

    public boolean isChatMessageRead(String id);

    public String getChatMessageMimeType(String id);

    public long getChatMessageTimestamp(String id);

    public long getGroupChatTimestamp(String id);

    public String getGroupChatSubject(String id);

    public boolean groupChatExists(String id);

    public String getChatMessageChatId(String id);

    public String getChatMessageContent(String id);

    public boolean chatMessageExists(String id);

    public boolean fileTransferExists(String id);

    public String getFileTransferFileName(String id);

    public String getFileTransferFileUri(String id);

    public String getFileTransferContact(String id);

    public String getGroupChatParticipants(String groupId);

    public void insertChatMessage(String messageId, String chatId, String contact, String content,
            long timestamp, String mimeType, int status, int reasonCode,
            int readStatus, int direction);

    public void insertFileTransfer(String ftId, String chatId, String contactId, String fileUri,
            String fileName, String mimeType, String icon, String iconMimeType,
            int direction, long size, long transferred, long timestamp,
            int state, int reasonCode, int readStatus);

    public void insertGroupChat(String chatId, int state, String subject, int direction,
            long timestamp,
            int reasonCode, String participants);

}

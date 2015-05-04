package com.sonymobile.rcs.cpm.ms.tests.util;

import java.io.Closeable;



/**
 * For testing assertion purposes.
 * 
 * @author 23054187
 *
 */
public interface RemotePersistence extends Closeable {
    
    public void init(String username, String password);
    
    public void setMessageSeen(String messageId, String remoteGroupId) throws Exception;

    public void setMessageDeleted(String messageId, String remoteGroupId) throws Exception;

    public void doExpunge() throws Exception;

    public boolean isMessageDeleted(String messageId, String sessionId) throws Exception;

    public boolean isMessageSeen(String messageId, String sessionId) throws Exception;

}

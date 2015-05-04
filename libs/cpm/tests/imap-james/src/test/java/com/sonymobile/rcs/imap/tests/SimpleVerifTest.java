
package com.sonymobile.rcs.imap.tests;

import static org.junit.Assert.assertTrue;

import com.sonymobile.rcs.imap.DefaultImapService;
import com.sonymobile.rcs.imap.Flag;
import com.sonymobile.rcs.imap.ImapException;
import com.sonymobile.rcs.imap.Part;
import com.sonymobile.rcs.imap.SocketIoService;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

/**
 * <p>
 * sudo ./apache-james-3.0-beta4/bin/james console
 * </p>
 */
public class SimpleVerifTest {

    @Before
    public void createUserAccount() throws Exception {
        JamesUtil.createUser("testuser@localhost", "1234", false);
    }

    @Test
    public void testSimple() throws IOException, ImapException {
        System.out.println("integration simple system out here");

        DefaultImapService srv = new DefaultImapService(new SocketIoService("imap://localhost"));
        srv.setAuthenticationDetails("testuser@localhost", "1234", null, null, true);
        srv.select("INBOX");

        Part content = new Part();
        content.setContent("hello world");
        content.setHeader("From", "test@hello.com");

        srv.append("mysubfolder", Arrays.asList(Flag.Recent), content);

        srv.delete("mysubfolder");
        boolean created = srv.create("mysubfolder");

        assertTrue(created);

        srv.select("mysubfolder");

        srv.append("mysubfolder", Arrays.asList(Flag.Recent), content);

        srv.logout();
        srv.close();
    }

}

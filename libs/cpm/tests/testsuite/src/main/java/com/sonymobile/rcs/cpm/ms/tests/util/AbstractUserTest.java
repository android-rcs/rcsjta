
package com.sonymobile.rcs.cpm.ms.tests.util;

import com.sonymobile.rcs.cpm.ms.MessageStore;
import com.sonymobile.rcs.cpm.ms.impl.CommonMessageStoreImpl;
import com.sonymobile.rcs.cpm.ms.impl.sync.DefaultSyncMediator;
import com.sonymobile.rcs.cpm.ms.sync.SyncMediator;
import com.sonymobile.rcs.cpm.ms.sync.SyncReport;
import com.sonymobile.rcs.imap.DefaultImapService;
import com.sonymobile.rcs.imap.ImapService;
import com.sonymobile.rcs.imap.ImapUtil;
import com.sonymobile.rcs.imap.IoService;
import com.sonymobile.rcs.imap.SocketIoService;

import junit.framework.TestCase;

import java.util.Calendar;
import java.util.TimeZone;

public abstract class AbstractUserTest extends TestCase {

    private SyncMediator syncMediator;

    private String username = null;

    protected RemotePersistence imap;

    protected boolean prepareImap = true;

    protected LocalPersistence db = null;// DummyDB.getInstance();

    public AbstractUserTest(String name) {
        super(name);
    }

    public void setRemotePersistence(RemotePersistence imap) {
        this.imap = imap;
    }

    public void setLocalPersistence(LocalPersistence dbHelper) {
        db = dbHelper;
    }

    @Override
    protected void setUp() throws Exception {

        this.username = getClass().getSimpleName().toLowerCase().replace("test", "")
                + "@sonymobile.com";

        // mediatorBuilder.setUsername(username);
        // mediatorBuilder.setPassword("1234");

        // syncMediator = mediatorBuilder.createMediator();
        imap = null; // builder.createImapService();

        db.clearGroups();
        db.clearFileTransfers();
        db.clearMessages();

        if (prepareImap)
            prepareImap();
    }

    public SyncMediator createMediator(String username, String password) {
        // LOCAL
        MessageStore localStore = null;// new LocalStore(DummyLocalPersistence.getInstance());

        // REMOTE
        IoService ioService = new SocketIoService("imap://localhost");
        ImapService imapService = new DefaultImapService(ioService);
        imapService.setAuthenticationDetails(username, password, null, null, false);

        MessageStore remoteStore = new CommonMessageStoreImpl("cpmdefault", imapService);

        DefaultSyncMediator mediator = new DefaultSyncMediator(localStore, remoteStore);
        return mediator;
    }

    private void prepareImap() throws Exception {
        // PopulateIMAP.populateAccount(username, "../syncadapter/src/test/imapserver");
    }

    @Override
    protected void tearDown() throws Exception {
        // TODO Auto-generated method stub
        super.tearDown();
    }

    protected boolean executeSync() throws Exception {
        return checkReport(syncMediator.execute());
    }

    private boolean checkReport(SyncReport report) throws Exception {
        if (report != null && report.getException() != null)
            throw report.getException();
        else
            return report.isSuccess();
    }

    protected long asTimestamp(int year, int month, int date, int hourOfDay, int minute,
            int second, String timeZone) {
        Calendar c = Calendar.getInstance();
        c.set(year, month, date, hourOfDay, minute, second);
        c.set(Calendar.MILLISECOND, 0);
        c.setTimeZone(TimeZone.getTimeZone(timeZone));

        return c.getTimeInMillis();
    }

    protected static byte[] decodeBase64(String content) {
        return ImapUtil.decodeBase64(content.getBytes());
    }

    public String getUsername() {
        return username;
    }

}

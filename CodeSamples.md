See source code at https://code.google.com/p/rcsjta/source/browse/?name=integration#git%2Ftests%2Fsamples

# Contact service API #

See source code at https://code.google.com/p/rcsjta/source/browse/tests/samples/src/com/gsma/rcs/api/ContactSampleTest.java?name=integration

## Example 1 - Format a contact phone number: ##

Any phone number used in the TAPI should be first formatted into a `ContactId` object which is an abstraction of the phone number. So applications can use any phone number format: national or international. An `IllegalArgumentException` is thrown in case of unsupported format.

```
        try {
            ContactId contact =  ContactUtil.getInstance(mContext).formatContact("06 81 63 90 59");
            Log.i(TAG, "Formatted contact: " + contact.toString());
        } catch (RcsPermissionDeniedException e) {
            Log.e(TAG, "Permission denied");
        }
```

## Example 2 - Instanciates the Contact service API: ##

```
        contactApi = new ContactService(mContext, apiServiceListener);
        contactApi.connect();
```

## Example 3 - Blocks a remote contact: ##

Here we can block a remote contact. So any communication (chat, FT, ...) coming from this remote contact will be blocked and redirected to the corresponding spambox.

```
        try {
            contactApi.blockContact(remote);
        } catch (RcsServiceNotAvailableException e) {
            Log.e(TAG, "RCS service not available");
        } catch (RcsPersistentStorageException e) {
            Log.e(TAG, "RCS service not available");
        } catch (RcsGenericException e) {
            Log.e(TAG, "Unexpected error", e);
        }
```

Then you can unblock:

```
        try {
            contactApi.unblockContact(remote);
        } catch (RcsServiceNotAvailableException e) {
            Log.e(TAG, "RCS service not available");
        } catch (RcsPersistentStorageException e) {
            Log.e(TAG, "RCS service not available");
        } catch (RcsGenericException e) {
            Log.e(TAG, "Unexpected error", e);
        }
```

## Example 4- Reads info of a remote RCS contact: ##

Here we get the info by calling a method of the API (see also the content provider solution).
RCS info contains mainly the registration state, the blocking state, the display name and the capabilities of the contact.

```
        try {
            RcsContact contact = contactApi.getRcsContact(remote);
            if (contact != null) {
                Log.i(TAG, "Contact info:");
                Log.i(TAG, "- Display name: " + contact.getDisplayName());
                Log.i(TAG, "- Online: " + contact.isOnline());
                Log.i(TAG, "- Blocked: " + contact.isBlocked());
            } else {
                Log.i(TAG, "Contact not found");
            }
        } catch (RcsServiceNotAvailableException e) {
            Log.e(TAG, "RCS service not available");
        } catch (RcsPersistentStorageException e) {
            Log.e(TAG, "RCS service not available");
        } catch (RcsGenericException e) {
            Log.e(TAG, "Unexpected error", e);
        }
```

## Example 5 - Gets RCS contacts online: ##

Here we get the list by calling a method of the API (see also the content provider solution).

```
        try {
            Set<RcsContact> contacts = contactApi.getRcsContactsOnline();
            Log.i(TAG, "Number of contacts online: " + contacts.size());
        } catch (RcsServiceNotAvailableException e) {
            Log.e(TAG, "RCS service not available");
        } catch (RcsPersistentStorageException e) {
            Log.e(TAG, "RCS service not available");
        } catch (RcsGenericException e) {
            Log.e(TAG, "Unexpected error", e);
        }
```

## Example 6-1 - Gets all RCS contacts: ##

```
        try {
            Set<RcsContact> contacts = contactApi.getRcsContacts();
            Log.i(TAG, "Number of RCS contacts: " + contacts.size());
        } catch (RcsServiceNotAvailableException e) {
            Log.e(TAG, "RCS service not available");
        } catch (RcsPersistentStorageException e) {
            Log.e(TAG, "RCS service not available");
        } catch (RcsGenericException e) {
            Log.e(TAG, "Unexpected error", e);
        }
```

## Example 6-2 - Gets all RCS contacts: ##

Here we get the info directly from the content provider of the device. Note that you can adapt the SQL request to filter as you want.

```
TODO
```

## Example 7 - Gets RCS contacts supporting a given extension: ##

Here we get the list by calling a method of the API (see also the content provider solution).

```
        try {
            Set<RcsContact> contacts = contactApi.getRcsContactsSupporting("ext.game");
            Log.i(TAG, "Number of contacts supporting the extension 'ext.game': " + contacts.size());
        } catch (RcsServiceNotAvailableException e) {
            Log.e(TAG, "RCS service not available");
        } catch (RcsPersistentStorageException e) {
            Log.e(TAG, "RCS service not available");
        } catch (RcsGenericException e) {
            Log.e(TAG, "Unexpected error", e);
        }
```


---


# Capability service API #

See source code at https://code.google.com/p/rcsjta/source/browse/tests/samples/src/com/gsma/rcs/api/CapabilitySampleTest.java?name=integration

## Example 1 - Instanciates the Capability service API: ##

```
        capabilityApi = new CapabilityService(mContext, apiServiceListener);
        capabilityApi.connect();
```

## Example 2 - Reads capabilities of a remote contact (method 1): ##

Here we get the info by calling a method of the API (see also the content provider solution).
Here there is no network request to refresh the capabilities.

```
        try {
            Capabilities capa = capabilityApi.getContactCapabilities(remote);
            if (capa != null) {
                Log.i(TAG, "Capabilities:");
                Log.i(TAG, "- chat support: " + capa.isImSessionSupported());
                Log.i(TAG, "- FT support: " + capa.isFileTransferSupported());
                Log.i(TAG, "- Extensions: " + capa.getSupportedExtensions().size());
            } else {
                Log.i(TAG, "Capabilities not found");
            }
        } catch (RcsServiceNotAvailableException e) {
            Log.e(TAG, "RCS service not available");
        } catch (RcsPersistentStorageException e) {
            Log.e(TAG, "RCS service not available");
        } catch (RcsGenericException e) {
            Log.e(TAG, "Unexpected error", e);
        }
```

## Example 3 - Reads capabilities of a remote contact (method 2): ##

Here we get the info directly from the content provider of the device. Note that you can adapt the SQL request to filter as you want.

```
        String contactNumber = remote.toString();
        Uri uri = Uri.withAppendedPath(CapabilitiesLog.CONTENT_URI, contactNumber);
        Cursor cursor = mContext.getContentResolver().query(uri, null, null, null, null);
        assertNotNull(cursor);
        if (cursor.moveToFirst()) {
            Log.i(TAG, "Capabilities:");
            Log.i(TAG, "- chat support: " + cursor.getInt(
                    cursor.getColumnIndex(CapabilitiesLog.CAPABILITY_IM_SESSION)));
            Log.i(TAG, "- FT support: " + cursor.getInt(
                    cursor.getColumnIndex(CapabilitiesLog.CAPABILITY_FILE_TRANSFER)));
            Log.i(TAG, "- Extensions: " + cursor.getString(
                    cursor.getColumnIndex(CapabilitiesLog.CAPABILITY_EXTENSIONS)));
        } else {
            Log.i(TAG, "Capabilities not found");
        }
        cursor.close();
```

## Example 4 - Requests a refresh of the capabilities for a remote contact: ##

Here we request a refresh of the capabilities by sending a SIP OPTIONS to the remote via the Telco network. Then the result is received via callback (see next chapter).

```
        try {
            capabilityApi.requestContactCapabilities(remote);
        } catch (RcsServiceNotAvailableException e) {
            Log.e(TAG, "RCS service not available");
        } catch (RcsServiceNotRegisteredException e) {
            Log.e(TAG, "RCS service not registered");
        } catch (RcsGenericException e) {
            Log.e(TAG, "Unexpected error", e);
        }
```

## Example 5 - Monitor contacts capabilities update: ##

The global listener permits to monitor the capabilities update of any contact.

```
        CapabilitiesListener listener = new CapabilitiesListener() {
            public void onCapabilitiesReceived(ContactId contact, Capabilities capabilities) {
                Log.i(TAG, "Capabilities of " + contact.toString() + ":");
                Log.i(TAG, "- chat support: " + capabilities.isImSessionSupported());
                Log.i(TAG, "- FT support: " + capabilities.isFileTransferSupported());
                Log.i(TAG, "- Video share support: " + capabilities.isVideoSharingSupported());
                Log.i(TAG, "- Image share support: " + capabilities.isImageSharingSupported());
                Log.i(TAG, "- Geoloc share support: " + capabilities.isGeolocPushSupported());
                Log.i(TAG, "- Extensions: " + capabilities.getSupportedExtensions().size());
            }
        };
        try {
            capabilityApi.addCapabilitiesListener(listener);
        } catch (RcsServiceNotAvailableException e) {
            Log.e(TAG, "RCS service not available");
        } catch (RcsGenericException e) {
            Log.e(TAG, "Unexpected error", e);
        }
```


---


# 1-1 Chat service API #

See source code at https://code.google.com/p/rcsjta/source/browse/tests/samples/src/com/gsma/rcs/api/ChatSampleTest.java?name=integration

## Example 1 - Send a chat message ##

If there is no ongoing session with the remote contact, a new SIP session is created to send the first message, else the existing session is reused. In case of coverage problem, the message is queued and sent later.

```
        try {
            OneToOneChat chat = chatApi.getOneToOneChat(remote);
            chat.sendMessage("Hello world!");
        } catch (RcsServiceNotAvailableException e) {
            Log.e(TAG, "RCS service not available");
        } catch (RcsPersistentStorageException e) {
            Log.e(TAG, "RCS service not available");
        } catch (RcsGenericException e) {
            Log.e(TAG, "Unexpected error", e);
        }
```

## Example 2 - Receives a chat message ##

```
    <receiver android:name="SingleChatInvitationReceiver" >
        <intent-filter>
            <action android:name="com.gsma.services.rcs.chat.action.NEW_ONE_TO_ONE_CHAT_MESSAGE" />
        </intent-filter>
    </receiver>    
```

```
    public class SingleChatInvitationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ContactId remote = intent.getParcelableExtra(OneToOneChatIntent.EXTRA_CONTACT);
            String msgId = intent.getStringExtra(OneToOneChatIntent.EXTRA_MESSAGE_ID);
            Log.i(TAG, "Receive message " + msgId  + " from " + remote.toString());
        }
    }
```

## Example 3 - Opens the chat conversation ##

```
        try {
            OneToOneChat chat = chatApi.getOneToOneChat(remote);
            chat.openChat();
        } catch (RcsServiceNotAvailableException e) {
            Log.e(TAG, "RCS service not available");
        } catch (RcsGenericException e) {
            Log.e(TAG, "Unexpected error", e);
        }       
```

## Example 4 - Send a read report to the remote contact ##

Here we send a report to the remote contact saying that the message as been read.
Note that the delivery report is sent automatically by the RCS service.

```
        try {
            chatApi.markMessageAsRead(msgId);
        } catch (RcsServiceNotAvailableException e) {
            Log.e(TAG, "RCS service not available");
        } catch (RcsPersistentStorageException e) {
            Log.e(TAG, "RCS service not available");
        } catch (RcsGenericException e) {
            Log.e(TAG, "Unexpected error", e);
        }       
```


---


# Group Chat service API #

```
TODO
```


---


# File Transfer service API #

```
TODO
```
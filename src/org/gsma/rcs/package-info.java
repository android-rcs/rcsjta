/**
 * The RCS-e stack is implemented into an Android background service
 * which offers a high level API: the RCS API.
 *
 * The RCS API is a client/server interface based on database
 * providers, AIDL API & Intents. Several UI may be connected at a time
 * to manage RCS events and to interact with the single stack instance
 * running in background.
 *
 * The RCS API permits to implement RCS application (e.g. enhanced
 * address book, content sharing, chat, widgets) by hiding RCS
 * protocols complexity.
 *
 * The RCS API offers the following API:
 * Terms and conditions API: accept/reject terms by end user.
 * Capability API: contact capabilities discovery.
 * Contacts API: RCS contact management and integration with the native address book.
 * Presence API: social presence sharing, presence subscription & publishing, anonymous fetch.
 * Rich call API: image sharing & video sharing during CS call.
 * Messaging API: 1-1 chat, group chat and file transfer.
 * Media API: media player & renderer.
 * Events log API: chat & file transfer history, rich call history and aggregation with classic log (calls, SMS, MMS).
 * RCS settings database provider: application and stack settings.
 *
 * ---------------
 *
 *
 * The RCS API uses the following Android concepts: Intents mechanism
 * to broadcast incoming events (e.g. notification) and incoming
 * invitations to any Android activity or broadcast receiver which are
 * declared in the device.  AIDL interfaces to initiate and to manage
 * session in real time (start, session monitoring, stop). Session
 * events are managed thanks to callback mechanism.
 * 
 * Methods of the RCS API throw an exception if the IMS core layer is
 * not initialized or not registered to the IMS platform.
 * 
 * Note: Remote application exceptions are not yet supported by the
 * AIDL SDK, a generic AIDL exception is thrown instead. 
 * 
 * How to use Intents?
 * 
 * By dynamically registering an instance of a class with
 * Context.registerReceiver() or by using the <receiver> tag in your
 * AndroidManifest.xml. Then the type of requested event is fixed in
 * the Intent filter associated to the receiver, each RCS API has its
 * own list of available intents.
 * 
 * How to use the AIDL interface?
 * 
 * The RCS API has also API callbacks to monitor in real time the IMS
 * connection status:
 *  
 * ---------------
 * PASTE DIAGRAMS HERE
 * ---------------

 * * ---------------
 *
 * 2.2. RCS permissions
 *
 * 2.2.1. RCS permission
 *
 * An application uses the classic RCS API should declare the
 * following permission in its manifest file:
 *
 * <uses-permission android:name="com.orangelabs.rcs.permission.RCS"/>
 *
 * An application using this permission should be signed
 * (i.e. protection is “signature level”) with the same certificate as
 * the RCS stack. This permits to avoid third party applications to
 * call RCS API which is for native applications only.
 *
 * 2.2.2. RCS extension permission
 *
 * An application uses the Generic SIP API should declare the
 * following permission in its manifest file:
 *
 * <uses-permission android:name="com.orangelabs.rcs.permission.RCS_EXTENSION"/>
 *
 * Here there is no protection at the signature level. This permits to
 * any third party application declaring this permission to use the
 * Generic SIP API and to offer new services on top of the RCS stack.
 *
 * 2.3. Contact identity
 *
 * All contacts are formatted into international format by the RCS
 * API. So any contact format (Phone number: national or
 * international, SIP-URI, Tel-URI) may be used as an input to the RCS
 * API methods.
 *
 * 
 * This API is common to all other API offering an AIDL interface:
 * Capability API, Presence API, Rich call API and messaging API.
 * 
 * This API is used to manage the connection to the Android service
 * implementing the RCS-e stack (.i.e server part of the RCS API).
 * 
 * For example, this API may be useful: To detect if the Android
 * service has been shutdown in order to disable a menu in UI part.
 * To check if the API is well connected to the Android service.
 *
 * ---------------
 *
 * "IMS API" Stuff
 *
 * This API is common to all other API offering an AIDL interface:
 * Capability API, Presence API, Rich call API, messaging API and
 * generic SIP API.
 *
 * This API is used to manage the connection with the IMS platform.
 *
 * For example, this API may be used: To detect an IMS disconnection
 * in order to disable a menu in UI part.  To get the current IMS
 * connection status in order to enable or not a RCS menu.
 *
 * See classes:
 * ImsEventListener
 * ImsApiIntents
 */
package org.gsma.rcs;

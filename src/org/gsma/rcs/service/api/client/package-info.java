/**
 * The RCS API uses the following Android concepts: Intents mechanism to
 * broadcast incoming events (e.g. notification) and incoming invitations
 * to any Android activity or broadcast receiver which are declared in
 * the device.  AIDL interfaces to initiate and to manage session in real
 * time (start, session monitoring, stop). Session events are managed
 * thanks to callback mechanism.
 * 
 *    Methods of the RCS API throw an exception if the IMS core layer is not
 *    initialized or not registered to the IMS platform.
 * 
 *    Note: Remote application exceptions are not yet supported by the
 *    AIDL SDK, a generic AIDL exception is thrown instead.
 * 
 * 
 *    How to use Intents?
 * 
 *    By dynamically registering an instance of a class with
 *    Context.registerReceiver() or by using the <receiver> tag in your
 *    AndroidManifest.xml. Then the type of requested event is fixed in
 *    the Intent filter associated to the receiver, each RCS API has its
 *    own list of available intents.
 * 
 *    How to use the AIDL interface?
 * 
 *    The RCS API has also API callbacks to monitor in real time the IMS
 *    connection status:
 * 
 *  
 * ---------------
 * 
 *    This API is common to all other API offering an AIDL interface:
 *    Capability API, Presence API, Rich call API and messaging API.
 * 
 *    This API is used to manage the connection to the Android service
 *    implementing the RCS-e stack (.i.e server part of the RCS API).
 * 
 *    For example, this API may be useful: To detect if the Android
 *    service has been shutdown in order to disable a menu in UI part.
 *    To check if the API is well connected to the Android service.


"IMS API" Stuff

This API is common to all other API offering an AIDL interface: Capability API, Presence API, Rich call API, messaging API and generic SIP API.

This API is used to manage the connection with the IMS platform.

For example, this API may be used:
To detect an IMS disconnection in order to disable a menu in UI part.
To get the current IMS connection status in order to enable or not a RCS menu.

See classes:
ImsEventListener
ImsApiIntents


 */
package org.gsma.rcs.service.api.client;

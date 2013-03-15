/**
 * Joyn (RCS), IMS and SIP session tools
 * <p>
 * This API permits to implement new additional IMS services without
 * any impact in the RCS stack. This generic SIP API manages only the
 * signaling flow and is independent from the media part which should
 * be supported by the application using the SIP API.
 * <p>
 * The IMS service implemented thanks to the SIP API is identified by
 * a feature tag.
 * <p>
 * Outgoing usecase:
 * <p>
 * 1. An outgoing INVITE request is sent by using the SIP API and by
 * using a feature tag associated to the requested IMS service.
 * <p>
 * 2. The session may be managed via the SIP API: cancel the session,
 * session update, terminate the session.
 * <p>
 * Incoming usecase:
 * <p>
 * 1. An incoming INVITE request is received in the RCS-e stack.
 * <p>
 * 2. A feature tag is extracted from the Contact header of the
 * incoming request.
 * <p>
 * 3. A SIP Intent based on the feature tag is broadcasted on the
 * device.
 * <p>
 * 4. If an application triggers the SIP Intent, the incoming session
 * invitation is processed (e.g. UI, sound, .etc) and may be accepted
 * (i.e. 200 OK) or rejected (e.g. 486 Busy) by the application or by
 * the ned user.
 * <p>
 * 5. If the incoming session is accepted, the SIP API permits to
 * manage the session: session update, terminate the session.
 * <p>
 * This package manages the SIP session on behalf of the
 * application. The application does not manually manages the SIP
 * session through this API. For instance, most mechanisms are
 * listeners to react on state changes and getter methods to query the
 * state of the session. There are not setter methods.
 * <p>
 * This is comparable to the Android VoIP API, see
 * <code>android.net.sip.SipManager</code> and
 * <code>android.net.sip.ISipSession</code> and
 * <p>
 * The "SIP API" and "Session API" names are confusing, "Extended
 * Service API" is a possible alternative.
 */
package org.gsma.joyn.session;

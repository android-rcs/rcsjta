/**
 * This API permits to implement new additional IMS services without
 * any impact in the RCS stack. This generic SIP API manages only the
 * signaling flow and is independent from the media part which should
 * be supported by the application using the SIP API.
 * 
 * The IMS service implemented thanks to the SIP API is identified by
 * a feature tag.
 * 
 * Outgoing usecase:
 * 
 * 1. An outgoing INVITE request is sent by using the SIP API and by
 * using a feature tag associated to the requested IMS service.
 *
 * 2. The session may be managed via the SIP API: cancel the session,
 * session update, terminate the session.
 * 
 * Incoming usecase:
 * 
 * 1. An incoming INVITE request is received in the RCS-e stack.
 *
 * 2. A feature tag is extracted from the Contact header of the
 * incoming request.
 *
 * 3. A SIP Intent based on the feature tag is broadcasted on the
 * device.
 *
 * 4. If an application triggers the SIP Intent, the incoming session
 * invitation is processed (e.g. UI, sound, .etc) and may be accepted
 * (i.e. 200 OK) or rejected (e.g. 486 Busy) by the application or by
 * the ned user. 
 *
 * 5. If the incoming session is accepted, the SIP API permits to
 * manage the session: session update, terminate the session.
 */
package  org.gsma.rcs.sip;

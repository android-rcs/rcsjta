/**
 * The RCS service is implemented into an Android background service
 * which offers a high level API: the RCS Terminal API.
 * <p>
 * The RCS API is a client/server interface based on database
 * providers, AIDL API & Intents. Several UI may be connected at a time
 * to manage RCS events and to interact with the single stack instance
 * running in background.
 * <p>
 * The RCS API permits to implement RCS application (e.g. enhanced
 * address book, content sharing app, chat view, widgets) by hiding RCS
 * protocols complexity.
 * <p>
 * The RCS API offers the following service API:<br>
 * - Capability API: contact capabilities discovery.<br>
 * - Chat API: 1-1 chat and group chat.<br>
 * - File Transfer API: transfer a file.<br>
 * - Video Share API: live video sharing during a CS call.<br>
 * - Image Share API: image sharing during a CS call.<br>
 * - UX API: links third party applications with RCS applications.<br>
 * <p>
 * The RCS API uses the following Android concepts:<br>
 * - Intents mechanism to broadcast incoming events (e.g. notification) and
 * incoming invitations to any Android activity or broadcast receiver which are
 * declared in the device.<br>
 * - AIDL interfaces to initiate and to manage session in real time (start,
 * session monitoring, stop). Session events are managed thanks to callback mechanism.<br>
 * <p>
 * Note: Methods of the RCS API throw an exception if the RCS service is not available, not
 * initialized or not registered to the IMS platform.
 * <p>
 * Note: Remote application exceptions are not yet supported by the
 * AIDL SDK, a generic AIDL exception is thrown instead.
 * <p>
 * Note: The supported formats for a contact used as a method parameter are:<br>
 * - Phone number in national or international format (e.g. +33xxx).<br>
 * - SIP address (e.g. "John" <sip:+33xxx@domain.com>).<br>
 * - SIP-URI (e.g. sip:+33xxx@domain.com).<br>
 * - Tel-URI (e.g. tel:+33xxx).<br>
 */
package com.gsma.services.rcs;

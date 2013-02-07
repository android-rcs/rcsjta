/**
 * This API is an abstraction of the internal RCS database which contains all the RCS info associated to each contact of the address book.
 *
 * A contact has the following RCS info: Type of contact (RCS-e compliant, Share presence, .etc). Supported Capabilities (Image share, Chat, .etc). Social presence info (fretext, photo-icon, .etc).
 *
 * The additional RCS info for contacts are linked into the native address book database thanks to the ContactContract API of the Android SDK (from 2.x).
 * 
 * Note: this API is not based on an AIDL interface and may be used even if the RCS service is stopped.
*/

package org.gsma.rcs.service.api.client.contacts;

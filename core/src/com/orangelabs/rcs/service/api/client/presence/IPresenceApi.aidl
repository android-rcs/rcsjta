package com.orangelabs.rcs.service.api.client.presence;

import android.graphics.Bitmap;
import com.orangelabs.rcs.service.api.client.presence.PresenceInfo;
import com.orangelabs.rcs.service.api.client.presence.FavoriteLink;
import com.orangelabs.rcs.service.api.client.presence.Geoloc;

/**
 * Presence API
 */
interface IPresenceApi {

	// Set my presence info
	boolean setMyPresenceInfo(in PresenceInfo info);

	// Invite a contact to share its presence
	boolean inviteContact(in String contact);
	
	// Accept the sharing invitation
	boolean acceptSharingInvitation(in String contact);
	
	// Reject the sharing invitation
	boolean rejectSharingInvitation(in String contact);
	
	// Ignore the sharing invitation
	void ignoreSharingInvitation(in String contact);
	
	// Revoke a shared contact
	boolean revokeContact(in String contact);

	// Unrevoke a revoked contact
	boolean unrevokeContact(in String contact);

	// Unblock a blocked contact
	boolean unblockContact(in String contact);

    // Get the list of granted contacts
	List<String> getGrantedContacts();
	
    // Get the list of revoked contacts
	List<String> getRevokedContacts();

    // Get the list of blocked contacts
	List<String> getBlockedContacts();
}

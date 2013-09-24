/*******************************************************************************
 * Software Name : RCS
 *
 * Copyright © 2010 France Telecom S.A.
 ******************************************************************************/

package com.orangelabs.rcs.common.dialog;

import java.util.ArrayList;

import com.orangelabs.rcs.common.ContactUtils;
import com.orangelabs.rcs.common.PhoneNumber;
import com.orangelabs.rcs.eab.PresenceSharingInvitation;
import com.orangelabs.rcs.eab.R;
import com.orangelabs.rcs.service.api.client.ClientApiException;
import com.orangelabs.rcs.service.api.client.contacts.ContactInfo;
import com.orangelabs.rcs.service.api.client.contacts.ContactsApi;
import com.orangelabs.rcs.service.api.client.presence.PresenceApi;
import com.orangelabs.rcs.service.api.client.presence.PresenceInfo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Handler;

/**
 * Dialog utility class
 * 
 * <li>Manages dialogs that are callable from whatever activity
 * <li>For RCS presence sharing related dialogs, the result is sent back to the calling activity, on the form of messages (KO or OK)
 * <li>The calling activity must implement a handler to receive those messages and is responsible for the subsequent actions to do.
 */
public class EabDialogUtils{
	
	// Return messages codes to be received by activities' handlers 
	public final static int DELETE_CONTACT_RESULT_OK = 1;
	public final static int DELETE_CONTACT_RESULT_KO = 2;
	public final static int DELETE_MULTIPLE_CONTACTS_RESULT_OK = 3;
	public final static int DELETE_MULTIPLE_CONTACTS_RESULT_KO = 4;
	public final static int REMOVE_CANCELLED_INVITATION_RESULT = 5;
	
	// Presence management associated dialog actions
	// When adding a new dialog, create a new entry
	private final static int DELETE_CONTACT = 1;
	private final static int REMOVE_CANCELLED_INVITATION = 2;

	/**
	 * Show a dialog with the given parameters 
	 * 
	 * @param activity String 
	 * @param title String 
	 * @param msg String
	 * @param positiveText String 
	 * @param neutralText String
	 * @param negativeText String
	 * @param positiveListener DialogInterface.OnClickListener
	 * @param neutralListener DialogInterface.OnClickListener
	 * @param negativeListener DialogInterface.OnClickListener
	 * @param cancelable boolean
	 * @param iconResource int
	 * @return
	 */
    public static AlertDialog showDialog(final Activity activity, 
    		String title, 
    		String msg, 
    		String positiveText,
    		String neutralText,
    		String negativeText,
    		DialogInterface.OnClickListener positiveListener, 
    		DialogInterface.OnClickListener neutralListener,
    		DialogInterface.OnClickListener negativeListener,
    		boolean cancelable,
    		int iconResource) {
    	AlertDialog.Builder builder = new AlertDialog.Builder(activity);
    	builder.setMessage(msg);
    	if (title!=null){
    		builder.setTitle(title);
    	}
    	builder.setCancelable(false);
    	if (positiveText!=null){
    		builder.setPositiveButton(positiveText, positiveListener);
    	}
    	if (negativeText!=null){
    		builder.setNegativeButton(negativeText, negativeListener);
    	}
    	if (neutralText!=null){
    		builder.setNeutralButton(neutralText, neutralListener);
    	}
    	builder.setCancelable(cancelable);
    	if (iconResource!=0){
    		builder.setIcon(iconResource);
    	}
    	AlertDialog alert = builder.create();
    	alert.show();
    	return alert;
    }
    
    /**
     * Show the block invitation dialog
     * 
     * @param activity
     * @param handler
     * @param contactName
     * @param contactNumber
     * @param presenceApi
     */
    public static void showBlockInvitationDialog(Activity activity, 
    		Handler handler, 
    		String contactName, 
    		String contactNumber, 
    		PresenceApi presenceApi){

    	// No title
    	// Text is blockContact + contactName
    	// Choices are Block / Cancel
    	// Confirmation on block
    	showPresenceManagementDialog(
    			BLOCK_INVITATION,
    			-1,
    			-1,
    			true,
    			false,
    			false,
    			activity.getString(R.string.rcs_eab_blockConfirmation_title),
    			activity.getString(R.string.rcs_eab_blockConfirmation,contactName), 
    			activity.getString(R.string.rcs_eab_blockSharing),
    			null,
    			activity.getString(R.string.rcs_common_cancel),
    			activity, 
    			handler, 
    			contactName, 
    			contactNumber, 
    			presenceApi);
    }
    
    /**
     * Show the unblock invitation dialog
     * 
     * @param activity
     * @param handler
     * @param contactName
     * @param contactNumber
     * @param presenceApi
     */
    public static void showUnblockInvitationDialog(Activity activity, 
    		Handler handler, 
    		String contactName, 
    		String contactNumber, 
    		PresenceApi presenceApi){

    	// Title title_contact_in_blacklist_notification + name
    	// Text is label_isInBlacklist + contactName
    	// Choices are Unblock / Cancel
    	// No confirmations
    	showPresenceManagementDialog(
    			UNBLOCK_INVITATION,
    			-1,
    			-1,
    			false,
    			false,
    			false,
    			activity.getString(R.string.rcs_eab_title_contact_in_blacklist_notification,contactName),
    			activity.getString(R.string.rcs_eab_label_isInBlacklist), 
    			activity.getString(R.string.rcs_eab_unblock),
    			null,
    			activity.getString(R.string.rcs_common_cancel),
    			activity, 
    			handler, 
    			contactName, 
    			contactNumber, 
    			presenceApi);
    }

    /**
     * Show the invite contact dialog
     * 
     * @param activity
     * @param handler
     * @param contactName
     * @param contactNumber
     * @param presenceApi
     */
    public static void showInviteContactDialog(Activity activity, 
    		Handler handler, 
    		String contactName, 
    		String contactNumber, 
    		PresenceApi presenceApi){
    	
    	// Title R.string.inviteConfirmation_title
    	// Text is cancelInvitationTo + contactName
    	// Choices are Yes / No
    	// No confirmation
    	showPresenceManagementDialog(
    			INITIATE_INVITATION,
    			-1,
    			-1,
    			false,
    			false,
    			false,
    			activity.getString(R.string.rcs_eab_inviteConfirmation_title),
    			activity.getString(R.string.rcs_eab_inviteConfirmation,contactName), 
    			activity.getString(R.string.rcs_common_send),
    			null,
    			activity.getString(R.string.rcs_common_cancel),
    			activity, 
    			handler, 
    			contactName, 
    			contactNumber, 
    			presenceApi);
    }

    /**
     * Show the delete contact dialog
     * 
     * @param activity
     * @param handler
     * @param contactName
     * @param contactNumbers List of RCS contacts to revoke
     * @param presenceApi
     */
    public static void showDeleteContactDialog(final Activity activity, 
    		final Handler handler, 
    		final String contactName, 
    		final ArrayList<PhoneNumber> contactNumbers, 
    		final PresenceApi presenceApi){
    	
    	if (contactNumbers.size()==1){
    		// Only one contact to revoke
    		
    		// Title R.string.deleteConfirmation_title
    		// Text is R.string.deleteConfirmation
    		// Choices are Yes / No
    		// No confirmation
	    	showPresenceManagementDialog(
	    			DELETE_CONTACT,
	    			-1,
	    			-1,
	    			false,
	    			false,
	    			false,
	    			activity.getString(R.string.rcs_eab_deleteEnrichedContactTitle),
	    			activity.getString(R.string.rcs_eab_deleteConfirmationActive), 
	    			activity.getString(R.string.rcs_eab_yes),
	    			null,
	    			activity.getString(R.string.rcs_eab_no),
	    			activity, 
	    			handler, 
	    			contactName, 
	    			contactNumbers.get(0).number, 
	    			presenceApi);
    	}else{
    		// Multiple contacts to revoke
    		// Title R.string.deleteConfirmation_title
    		// Text is R.string.deleteConfirmation
    		// Choices are Yes / No
    		// No confirmation
    		DialogInterface.OnClickListener positiveListener = new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface dialog, int button) {

    				// If we do not need a confirmation, clicking is enough to begin the presenceApi job
    				final ProgressDialog progressDialog = showProgressDialog(activity, DELETE_CONTACT);
    				Thread positiveThread = new Thread(new Runnable(){
    					public void run(){
    						boolean result = false;
    						// Revoke all the contacts
    						for (int i=0;i<contactNumbers.size();i++){
	    						try {
	    							result = result || presenceApi.revokeContact(contactNumbers.get(i).number);
	    						} catch (ClientApiException e) {
	
	    						}
    						}
    						if (result){
    							// If everything was ok, send an OK message to the handler
    							handler.sendEmptyMessage(DELETE_MULTIPLE_CONTACTS_RESULT_OK);
    						}else{
    							handler.sendEmptyMessage(DELETE_MULTIPLE_CONTACTS_RESULT_KO);
    						}

    						handler.post(new Runnable(){
    							public void run(){
    								progressDialog.dismiss();        					
    							}
    						});
    					}

    				});

    				positiveThread.start();
    			}
    		};

    		boolean cancelable = true;
    		int iconResource = R.drawable.rcs_common_flower_21x21;	
    		showDialog(activity, 
    				activity.getString(R.string.rcs_eab_deleteEnrichedContactTitle),
    				activity.getString(R.string.rcs_eab_deleteMultipleConfirmationActive), 
    				activity.getString(R.string.rcs_eab_yes),
    				null,
    				activity.getString(R.string.rcs_eab_no),
    				positiveListener, 
    				null, 
    				null, 
    				cancelable, 
    				iconResource);
    	}
    }
    
    /**
     * Show the revoke contact dialog
     * 
     * @param activity
     * @param handler
     * @param contactName
     * @param contactNumber
     * @param presenceApi
     */
    public static void showRevokeContactDialog(Activity activity, 
    		Handler handler, 
    		String contactName, 
    		String contactNumber, 
    		PresenceApi presenceApi){
    	
    	// Title R.string.revokeConfirmation_title
    	// Text is R.string.revokeConfirmation
    	// Choices are R.string.stopSharing / Cancel
    	// No confirmation
    	showPresenceManagementDialog(
    			REVOKE_INVITATION,
    			-1,
    			-1,
    			false,
    			false,
    			false,
    			activity.getString(R.string.rcs_eab_revokeConfirmation_title),
    			activity.getString(R.string.rcs_eab_revokeConfirmation), 
    			activity.getString(R.string.rcs_eab_stopSharing),
    			null,
    			activity.getString(R.string.rcs_common_cancel),
    			activity, 
    			handler, 
    			contactName, 
    			contactNumber, 
    			presenceApi);
    }
    
    /**
     * Show the cancel invitation dialog
     * 
     * @param activity
     * @param handler
     * @param contactName
     * @param contactNumber
     * @param presenceApi
     */
    public static void showCancelInvitationDialog(Activity activity, 
    		Handler handler, 
    		String contactName, 
    		String contactNumber, 
    		PresenceApi presenceApi){
    	
    	// No title
    	// Text is cancelInvitationTo + contactName
    	// Choices are Yes / No
    	// No confirmation
    	showPresenceManagementDialog(
    			CANCEL_INVITATION,
    			-1,
    			-1,
    			false,
    			false,
    			false,
    			activity.getString(R.string.rcs_eab_cancelConfirmation_title),
    			activity.getString(R.string.rcs_eab_cancelConfirmation, contactName), 
    			activity.getString(R.string.rcs_eab_yes),
    			null,
    			activity.getString(R.string.rcs_eab_no),
    			activity, 
    			handler, 
    			contactName, 
    			contactNumber, 
    			presenceApi);
    }

    /**
     * Show the received invitation dialog
     * 
     * @param activity
     * @param handler
     * @param contactName
     * @param contactNumber
     * @param presenceApi
     */
    public static void showReceivedInvitationDialog(Activity activity, 
    		Handler handler, 
    		String contactName, 
    		String contactNumber, 
    		PresenceApi presenceApi){
    	// The invitation may have been cancelled before it was answered
    	// In this case, we show the cancelled invitation dialog instead
    	ContactsApi contactsApi = new ContactsApi(activity);
    	
    	int status = contactsApi.getContactInfo(contactNumber).getRcsStatus();
    	if (status==ContactInfo.RCS_CANCELLED){
    		showCancelledInvitationDialog(activity, handler, contactName, contactNumber, presenceApi);
    		return;
    	}
    	
    	// Title is title_presence_invitation_notification
    	// Text is label_presence_invitation_question + contactName
    	// Choices are : Accept, Decline, Ignore
    	// Confirmation on Decline (neutral position)
    	showPresenceManagementDialog(
    			ACCEPT_INVITATION,
    			BLOCK_INVITATION,
    			IGNORE_INVITATION,
    			false,
    			true,
    			false,
    			activity.getString(R.string.rcs_eab_title_presence_invitation_notification),
    			activity.getString(R.string.rcs_eab_label_presence_invitation_question, contactName),
    			activity.getString(R.string.rcs_common_accept_invitation),
    			activity.getString(R.string.rcs_common_decline_invitation),
    			activity.getString(R.string.rcs_eab_label_ignore_invitation),
    			activity, 
    			handler, 
    			contactName, 
    			contactNumber, 
    			presenceApi);
    }

    /**
     * Show the cancelled invitation dialog
     * 
     * @param activity
     * @param handler
     * @param contactName
     * @param contactNumber
     * @param presenceApi
     */
    public static void showCancelledInvitationDialog(Activity activity, 
    		Handler handler, 
    		String contactName, 
    		String contactNumber, 
    		PresenceApi presenceApi){
    	
    	// Title is title_cancelled_presence_invitation_notification
    	// Text is label_cancelled_presence_invitation_question + contactName
    	// Choices are : Invite contact, Remove invitation, Later
    	showPresenceManagementDialog(INITIATE_INVITATION, 
    			REMOVE_CANCELLED_INVITATION, 
    			IGNORE_INVITATION, 
    			false, 
    			false, 
    			false, 
    			activity.getString(R.string.rcs_eab_title_presence_invitation_notification),
    			activity.getString(R.string.rcs_eab_label_cancelled_presence_invitation_question, contactName),
    			activity.getString(R.string.rcs_eab_invite_contact),
    			activity.getString(R.string.rcs_eab_remove_cancelled_invitation),
    			activity.getString(R.string.rcs_eab_label_ignore_invitation),
    			activity, 
    			handler, 
    			contactName, 
    			contactNumber, 
    			presenceApi);
    }
    
    /**
     * Show the presence management dialog
     * 
     * @param POSITIVE_CASE
     * @param NEUTRAL_CASE
     * @param NEGATIVE_CASE
     * @param confirmPositiveChoice
     * @param confirmNeutralChoice
     * @param confirmNegativeChoice
     * @param title
     * @param msg
     * @param positiveText
     * @param neutralText
     * @param negativeText
     * @param activity
     * @param handler
     * @param contactName
     * @param contactNumber
     * @param presenceApi
     */
    private static void showPresenceManagementDialog(final int POSITIVE_CASE, 
    		final int NEUTRAL_CASE, 
    		final int NEGATIVE_CASE, 
    		final boolean confirmPositiveChoice,
    		final boolean confirmNeutralChoice,
    		final boolean confirmNegativeChoice,
    		String title,
    		String msg, 
    		String positiveText, 
    		String neutralText, 
    		String negativeText, 
    		final Activity activity, 
    		final Handler handler, 
    		final String contactName, 
    		final String contactNumber, 
    		final PresenceApi presenceApi){
    	
    	DialogInterface.OnClickListener positiveListener = null;
    	if (POSITIVE_CASE!=-1) positiveListener = new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int button) {

    			if (!confirmPositiveChoice){
    				// If we do not need a confirmation, clicking is enough to begin the presenceApi job
    				final ProgressDialog progressDialog = showProgressDialog(activity, POSITIVE_CASE);
    				Thread positiveThread = new Thread(new Runnable(){
    					public void run(){
    						boolean result = false;
    						try {
    							switch(POSITIVE_CASE){
    							case UNBLOCK_INVITATION:
    								result = presenceApi.unblockContact(contactNumber);
    								break;
    							case BLOCK_INVITATION:
    								result = presenceApi.rejectSharingInvitation(contactNumber);
    								break;
    							case CANCEL_INVITATION:
    								result = presenceApi.revokeContact(contactNumber);
    								break;
    							case ACCEPT_INVITATION:
    								// Remove the notification
    								PresenceSharingInvitation.removeSharingInvitationNotification(activity);
    								// Before accepting the presence sharing invitation, we check if the contact needs to be created
    						    	ContactUtils.acceptRcsContact(activity, contactNumber);
    								result = presenceApi.acceptSharingInvitation(contactNumber);
    								break;
    							case INITIATE_INVITATION:
    								result = presenceApi.inviteContact(contactNumber);
    								break;
    							case REVOKE_INVITATION:
    								result = presenceApi.revokeContact(contactNumber);
    								break;
    							case DELETE_CONTACT:
    								result = presenceApi.revokeContact(contactNumber);
    								break;
    							}
    						} catch (ClientApiException e) {

    						}
    						if (result){
    							// If everything was ok, send an OK message to the handler
    							switch(POSITIVE_CASE){
    							case UNBLOCK_INVITATION:
    								handler.sendEmptyMessage(UNBLOCK_INVITATION_RESULT_OK);
    								break;
    							case BLOCK_INVITATION:
    								handler.sendEmptyMessage(BLOCK_INVITATION_RESULT_OK);
    								break;
    							case CANCEL_INVITATION:
    								handler.sendEmptyMessage(CANCEL_INVITATION_RESULT_OK);
    								break;
    							case ACCEPT_INVITATION:
    								handler.sendEmptyMessage(ACCEPT_INVITATION_RESULT_OK);
    								break;
    							case INITIATE_INVITATION:
    								handler.sendEmptyMessage(INITIATE_INVITATION_RESULT_OK);
    								break;
    							case REVOKE_INVITATION:
    								handler.sendEmptyMessage(REVOKE_INVITATION_RESULT_OK);
    								break;
    							case DELETE_CONTACT:
    								handler.sendEmptyMessage(DELETE_CONTACT_RESULT_OK);
    								break;
    							}
    						}else{
    							// Else, send a result KO
    							switch(POSITIVE_CASE){
    							case UNBLOCK_INVITATION:
    								handler.sendEmptyMessage(UNBLOCK_INVITATION_RESULT_KO);
    								break;
    							case BLOCK_INVITATION:
    								handler.sendEmptyMessage(BLOCK_INVITATION_RESULT_KO);
    								break;
    							case CANCEL_INVITATION:
    								handler.sendEmptyMessage(CANCEL_INVITATION_RESULT_KO);
    								break;
    							case ACCEPT_INVITATION:
    								handler.sendEmptyMessage(ACCEPT_INVITATION_RESULT_KO);
    								break;
    							case INITIATE_INVITATION:
    								handler.sendEmptyMessage(INITIATE_INVITATION_RESULT_KO);
    								break;
    							case REVOKE_INVITATION:
    								handler.sendEmptyMessage(REVOKE_INVITATION_RESULT_KO);
    								break;
    							case DELETE_CONTACT:
    								handler.sendEmptyMessage(DELETE_CONTACT_RESULT_KO);
    								break;
    							}
    						}

    						handler.post(new Runnable(){
    							public void run(){
    								progressDialog.dismiss();        					
    							}
    						});
    					}

    				});

    				positiveThread.start();
    			}else{
    				// We need a confirmation
    				showPresenceConfirmationDialog(POSITIVE_CASE, 
    						activity, 
    						handler, 
    						contactName, 
    						contactNumber, 
    						presenceApi);
    			}
    		}
    	};


    	DialogInterface.OnClickListener neutralListener = null;
    	if (NEUTRAL_CASE!=-1) neutralListener = new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int button) {

    			if (!confirmNeutralChoice){
    				final ProgressDialog progressDialog = showProgressDialog(activity, NEUTRAL_CASE);
    				Thread neutralThread = new Thread(new Runnable(){
    					public void run(){
    						boolean result = false;
    						try {
    							switch(NEUTRAL_CASE){
    							case BLOCK_INVITATION:
    								// Remove the notification
    								PresenceSharingInvitation.removeSharingInvitationNotification(activity);
    						    	// Before declining the presence sharing invitation, we check if no entry has to be deleted from RCS database
    						    	ContactUtils.refuseRcsContact(activity, contactNumber);
    								result = presenceApi.rejectSharingInvitation(contactNumber);
    								break;
    							case REMOVE_CANCELLED_INVITATION:
    								// Remove the notification
    								PresenceSharingInvitation.removeSharingInvitationNotification(activity);
    						    	// Before removing the cancelled presence sharing invitation
    								ContactsApi contactsApi = new ContactsApi(activity);
    								contactsApi.removeCancelledPresenceInvitation(contactNumber);
    								break;
    							}
    						} catch (ClientApiException e) {

    						}
    						if (result){
    							// If everything was ok, send an OK message to the handler
    							switch(NEUTRAL_CASE){
    							case BLOCK_INVITATION:
    								handler.sendEmptyMessage(BLOCK_INVITATION_RESULT_OK);
    								break;
    							case REMOVE_CANCELLED_INVITATION:
    								handler.sendEmptyMessage(REMOVE_CANCELLED_INVITATION_RESULT);
    								break;
    							}
    						}else{
    							// Else, send a result KO
    							switch(NEUTRAL_CASE){
    							case BLOCK_INVITATION:
    								handler.sendEmptyMessage(BLOCK_INVITATION_RESULT_KO);
    								break;
    							case REMOVE_CANCELLED_INVITATION:
    								handler.sendEmptyMessage(REMOVE_CANCELLED_INVITATION_RESULT);
    								break;
    							}
    						}

    						handler.post(new Runnable(){
    							public void run(){
    								progressDialog.dismiss();        					
    							}
    						});
    					}
    				});
    				neutralThread.start();
    			}else{
    				// We need a confirmation
    				showPresenceConfirmationDialog(NEUTRAL_CASE, 
    						activity, 
    						handler, 
    						contactName, 
    						contactNumber, 
    						presenceApi);
    			}
    		}
    	};

    	DialogInterface.OnClickListener negativeListener = new DialogInterface.OnClickListener() {
        		public void onClick(DialogInterface dialog, int button) {

        			if (!confirmNegativeChoice){
        				final ProgressDialog progressDialog = showProgressDialog(activity, NEGATIVE_CASE);
        				Thread negativeThread = new Thread(new Runnable(){
        					public void run(){
        						boolean result = false;
        						try {
        							switch(NEGATIVE_CASE){
        							case IGNORE_INVITATION:
        								// Remove the notification
        								PresenceSharingInvitation.removeSharingInvitationNotification(activity);
        						    	// Before ignoring the presence sharing invitation
        						    	ContactUtils.ignoreRcsContact(activity, contactNumber);
        								presenceApi.ignoreSharingInvitation(contactNumber);
        								break;
        							}
        						} catch (ClientApiException e) {

        						}
        						if (result){
        							// If everything was ok, send an OK message to the handler
        							switch(NEGATIVE_CASE){
        							case IGNORE_INVITATION:
        								handler.sendEmptyMessage(IGNORE_INVITATION_RESULT_OK);
        								break;
        							}
        						}else{
        							// Else, send a result KO
        							switch(NEGATIVE_CASE){
        							case IGNORE_INVITATION:
        								handler.sendEmptyMessage(IGNORE_INVITATION_RESULT_KO);
        								break;
        							}
        						}

        						handler.post(new Runnable(){
        							public void run(){
        								progressDialog.dismiss();        					
        							}
        						});
        					}
        				});
        				negativeThread.start();
        			}else{
        				// We need a confirmation
        				showPresenceConfirmationDialog(NEGATIVE_CASE, 
        						activity, 
        						handler, 
        						contactName, 
        						contactNumber, 
        						presenceApi);
        			}
        		}
        	};

    	boolean cancelable = true;
    	int iconResource = R.drawable.rcs_common_flower_21x21;	
    	showDialog(activity, 
    			title, 
    			msg, 
    			positiveText, 
    			neutralText, 
    			negativeText,
    			positiveListener, 
    			neutralListener, 
    			negativeListener, 
    			cancelable, 
    			iconResource);
    }
    
    /**
     * Show a progress dialog
     * 
     * @param activity
     * @param CASE
     * @return progress dialog
     */
	private static ProgressDialog showProgressDialog(final Activity activity, final int CASE){
		ProgressDialog progressDialog = new ProgressDialog(activity);
		String message = null;
		switch (CASE){
		case UNBLOCK_INVITATION:
			message = activity.getString(R.string.rcs_eab_unblockInProgress);
			break;
		case CANCEL_INVITATION:
			message = activity.getString(R.string.rcs_eab_cancelInviteContactInProgress);
			break;
		case ACCEPT_INVITATION:
			message = activity.getString(R.string.rcs_eab_inviteContactInProgress);
			break;
		case BLOCK_INVITATION:
			message = activity.getString(R.string.rcs_eab_blockInProgress);
			break;
		case INITIATE_INVITATION:
			message = activity.getString(R.string.rcs_eab_inviteContactInProgress);
			break;
		case REVOKE_INVITATION:
			message = activity.getString(R.string.rcs_eab_revokeContactInProgress);
			break;
		case DELETE_CONTACT:
			message = activity.getString(R.string.rcs_eab_revokeContactInProgress);
			break;
		case IGNORE_INVITATION:
			// Nothing to display
			message = null;
			break;
		}
		progressDialog.setMessage(message);
		progressDialog.setIndeterminate(true);
		progressDialog.setCancelable(true);
		if (message!=null){
			progressDialog.show();
		}
		return progressDialog;
	}
	
	/**
	 * Called when a choice that need confirmation has been issued on a presence management dialog
	 * 
	 * @param CHOICE 
	 * @param activity
	 * @param handler
	 * @param contactName
	 * @param contactNumber
	 * @param presenceApi
	 */
	private static void showPresenceConfirmationDialog(final int CHOICE, 
			final Activity activity, 
    		final Handler handler, 
    		final String contactName, 
    		final String contactNumber, 
    		final PresenceApi presenceApi){

		String title = null;
		String msg = null;
		String positiveText = null;
		String negativeText = null;
    	DialogInterface.OnClickListener positiveListener = null;
    	DialogInterface.OnClickListener negativeListener = null;
    	switch(CHOICE){
    	case BLOCK_INVITATION:
    		msg = activity.getString(R.string.rcs_eab_confirm_block, contactName);
    		positiveText = activity.getString(R.string.rcs_eab_yes);
    		negativeText = activity.getString(R.string.rcs_eab_no);

    		positiveListener = new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface dialog, int button) {
    				final ProgressDialog progressDialog = showProgressDialog(activity, CHOICE);
    				Thread positiveThread = new Thread(new Runnable(){
    					public void run(){
    						boolean result = false;
    						try {
								// Remove the notification
								PresenceSharingInvitation.removeSharingInvitationNotification(activity);
						    	// Before declining the presence sharing invitation, we check if no entry has to be deleted from RCS database
						    	ContactUtils.refuseRcsContact(activity, contactNumber);
    							result = presenceApi.rejectSharingInvitation(contactNumber);
    						} catch (ClientApiException e) {

    						}
    						if (result){
    							// If everything was ok, send an OK message to the handler
    							handler.sendEmptyMessage(BLOCK_INVITATION_RESULT_OK);
    						}else{
    							handler.sendEmptyMessage(BLOCK_INVITATION_RESULT_KO);
    						}
    						handler.post(new Runnable(){
    							public void run(){
    								progressDialog.dismiss();        					
    							}
    						});
    					}

    				});
    				positiveThread.start();
    			}
    		};
    		break;
		}
		
    	showDialog(activity, 
    			title, 
    			msg, 
    			positiveText, 
    			null, 
    			negativeText,
    			positiveListener, 
    			null, 
    			negativeListener, 
    			true, 
    			R.drawable.rcs_common_flower_21x21);
	}

	/**
	 * Show an information dialog
	 * 
	 * @param activity
	 * @param handler
	 * @param title
	 * @param msg
	 */
	public static void showInfoDialog(final Activity activity, 
			final Handler handler, 
			final String title,
			final String msg){
		handler.post(new Runnable(){
			public void run(){
				showDialog(activity, 
						title, 
						msg, 
						activity.getString(android.R.string.ok), 
						null, 
						null, 
						null, 
						null, 
						null, 
						true, 
						R.drawable.rcs_common_flower_21x21);
			}
		});
	}
	
}

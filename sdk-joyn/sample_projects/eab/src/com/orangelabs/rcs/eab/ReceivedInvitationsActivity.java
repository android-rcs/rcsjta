/*******************************************************************************
 * Software Name : RCS
 *
 * Copyright © 2010 France Telecom S.A.
 ******************************************************************************/

package com.orangelabs.rcs.eab;

import java.util.List;

import com.orangelabs.rcs.eab.R;
import com.orangelabs.rcs.common.dialog.EabDialogUtils;
import com.orangelabs.rcs.service.api.client.contacts.ContactsApi;

import android.os.Bundle;

/**
 * Displays the RCS profile invitations you received that you have not answered yet.
 * <br>These invitations may also have been cancelled
 * <br>
 * <br>Action available for valid invitations is the same as when just receiving the invitation:
 * <li>Accept it
 * <li>Decline it
 * <li>Answer later
 * 
 * <br>Action available for cancelled invitations is :
 * <li>Invite contact
 * <li>Remove invitation 
 */
public class ReceivedInvitationsActivity extends ManageRcsListActivity{
    
	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
	}
	
	@Override
	public String getPresentationText(){
		return getString(R.string.rcs_eab_received_invitations_presentation);
	}
	
	@Override
	public List<String> getContactsList(){
		ContactsApi contactsApi = new ContactsApi(this);
		return contactsApi.getRcsWillingContacts();
	}
	
	@Override
	public String getListText(String name){
		return getString(R.string.rcs_eab_answerInvitationFrom, name);
	}
	
	@Override
	public void showListDialog(){
		EabDialogUtils.showReceivedInvitationDialog(this, handler, contactName, contactNumber, presenceApi);
	}

}

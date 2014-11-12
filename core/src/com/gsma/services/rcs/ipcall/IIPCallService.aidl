package com.gsma.services.rcs.ipcall;

import com.gsma.services.rcs.IRcsServiceRegistrationListener;
import com.gsma.services.rcs.ipcall.IIPCall;
import com.gsma.services.rcs.ipcall.IIPCallListener;
import com.gsma.services.rcs.ipcall.IIPCallPlayer;
import com.gsma.services.rcs.ipcall.IIPCallRenderer;
import com.gsma.services.rcs.ipcall.IPCallServiceConfiguration;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * IP call service API
 */
interface IIPCallService {

	boolean isServiceRegistered();

	void addEventListener(IRcsServiceRegistrationListener listener);

	void removeEventListener(IRcsServiceRegistrationListener listener);

	IPCallServiceConfiguration getConfiguration();

	List<IBinder> getIPCalls();
	
	IIPCall getIPCall(in String callId);

	IIPCall initiateCall(in ContactId contact, in IIPCallPlayer player, in IIPCallRenderer renderer);
	
	IIPCall initiateVisioCall(in ContactId contact, in IIPCallPlayer player, in IIPCallRenderer renderer);

	void addEventListener2(in IIPCallListener listener);

	void removeEventListener2(in IIPCallListener listener);
	
	int getServiceVersion();
}
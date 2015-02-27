package com.gsma.rcs.service.ipcalldraft;

import com.gsma.services.rcs.IRcsServiceRegistrationListener;
import com.gsma.rcs.service.ipcalldraft.IIPCall;
import com.gsma.rcs.service.ipcalldraft.IIPCallListener;
import com.gsma.rcs.service.ipcalldraft.IIPCallPlayer;
import com.gsma.rcs.service.ipcalldraft.IIPCallRenderer;
import com.gsma.rcs.service.ipcalldraft.IIPCallServiceConfiguration;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.ICommonServiceConfiguration;
import com.gsma.services.rcs.RcsServiceRegistration;

/**
 * IP call service API
 */
interface IIPCallService {

	boolean isServiceRegistered();
	
	int getServiceRegistrationReasonCode();

	void addEventListener(IRcsServiceRegistrationListener listener);

	void removeEventListener(IRcsServiceRegistrationListener listener);

	IIPCallServiceConfiguration getConfiguration();

	List<IBinder> getIPCalls();
	
	IIPCall getIPCall(in String callId);

	IIPCall initiateCall(in ContactId contact, in IIPCallPlayer player, in IIPCallRenderer renderer);
	
	IIPCall initiateVisioCall(in ContactId contact, in IIPCallPlayer player, in IIPCallRenderer renderer);

	void addEventListener2(in IIPCallListener listener);

	void removeEventListener2(in IIPCallListener listener);
	
	int getServiceVersion();
	
	ICommonServiceConfiguration getCommonConfiguration();
}
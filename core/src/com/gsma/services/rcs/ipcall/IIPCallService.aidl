package com.gsma.services.rcs.ipcall;

import com.gsma.services.rcs.IJoynServiceRegistrationListener;
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

	void addServiceRegistrationListener(IJoynServiceRegistrationListener listener);

	void removeServiceRegistrationListener(IJoynServiceRegistrationListener listener); 

	IPCallServiceConfiguration getConfiguration();

	List<IBinder> getIPCalls();
	
	IIPCall getIPCall(in String callId);

	IIPCall initiateCall(in ContactId contact, in IIPCallPlayer player, in IIPCallRenderer renderer);
	
	IIPCall initiateVisioCall(in ContactId contact, in IIPCallPlayer player, in IIPCallRenderer renderer);

	void addEventListener(in IIPCallListener listener);

	void removeEventListener(in IIPCallListener listener);
	
	int getServiceVersion();
}
package com.gsma.services.rcs.ipcall;

import com.gsma.services.rcs.IJoynServiceRegistrationListener;
import com.gsma.services.rcs.ipcall.IIPCall;
import com.gsma.services.rcs.ipcall.IIPCallListener;
import com.gsma.services.rcs.ipcall.INewIPCallListener;
import com.gsma.services.rcs.ipcall.IIPCallPlayer;
import com.gsma.services.rcs.ipcall.IIPCallRenderer;
import com.gsma.services.rcs.ipcall.IPCallServiceConfiguration;

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

	IIPCall initiateCall(in String contact, in IIPCallPlayer player, in IIPCallRenderer renderer, in IIPCallListener listener);
	
	IIPCall initiateVisioCall(in String contact, in IIPCallPlayer player, in IIPCallRenderer renderer, in IIPCallListener listener);

	void addNewIPCallListener(in INewIPCallListener listener);

	void removeNewIPCallListener(in INewIPCallListener listener);
	
	int getServiceVersion();
}
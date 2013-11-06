package org.gsma.joyn.ipcall;

import org.gsma.joyn.IJoynServiceRegistrationListener;
import org.gsma.joyn.ipcall.IIPCall;
import org.gsma.joyn.ipcall.IIPCallListener;
import org.gsma.joyn.ipcall.INewIPCallListener;
import org.gsma.joyn.ipcall.IIPCallPlayer;
import org.gsma.joyn.ipcall.IIPCallRenderer;
import org.gsma.joyn.ipcall.IPCallServiceConfiguration;

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
	
	void addNewIPCallListener(in INewIPCallListener listener);

	void removeNewIPCallListener(in INewIPCallListener listener);
	
	int getServiceVersion();
}
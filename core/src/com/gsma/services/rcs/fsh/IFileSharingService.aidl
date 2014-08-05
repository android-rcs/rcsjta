package com.gsma.services.rcs.fsh;

import com.gsma.services.rcs.IJoynServiceRegistrationListener;
import com.gsma.services.rcs.fsh.IFileSharing;
import com.gsma.services.rcs.fsh.IFileSharingListener;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * File sharing service API
 */
interface IFileSharingService {

	boolean isServiceRegistered();

	void addServiceRegistrationListener(IJoynServiceRegistrationListener listener);

	void removeServiceRegistrationListener(IJoynServiceRegistrationListener listener); 
    
	List<IBinder> getFileSharings();
	
	IFileSharing getFileSharing(in String sharingId);

	IFileSharing shareFile(in ContactId contact, in Uri file);

	void addEventListener(in IFileSharingListener listener);

	void removeEventListener(in IFileSharingListener listener);

	int getServiceVersion();
}
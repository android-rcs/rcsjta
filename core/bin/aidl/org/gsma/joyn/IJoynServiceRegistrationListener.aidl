package org.gsma.joyn;

/**
 * Joyn service registration events listener
 */
interface IJoynServiceRegistrationListener {
	void onServiceRegistered();
	
	void onServiceUnregistered();
}
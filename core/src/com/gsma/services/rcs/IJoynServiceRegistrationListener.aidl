package com.gsma.services.rcs;

/**
 * Joyn service registration events listener
 */
interface IJoynServiceRegistrationListener {
	void onServiceRegistered();
	
	void onServiceUnregistered();
}
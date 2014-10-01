package com.gsma.services.rcs;

/**
 * Rcs service registration events listener
 */
interface IRcsServiceRegistrationListener {

	void onServiceRegistered();

	void onServiceUnregistered();
}
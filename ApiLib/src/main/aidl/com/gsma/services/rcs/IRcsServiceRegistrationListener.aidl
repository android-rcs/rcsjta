package com.gsma.services.rcs;

import com.gsma.services.rcs.RcsServiceRegistration;

/**
 * RCS service registration events listener
 */
interface IRcsServiceRegistrationListener {

	void onServiceRegistered();

	void onServiceUnregistered(in int reasonCode);
}
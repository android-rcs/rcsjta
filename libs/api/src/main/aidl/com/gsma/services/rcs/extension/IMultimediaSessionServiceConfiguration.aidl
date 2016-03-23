package com.gsma.services.rcs.extension;

/**
 * Multimedia session service configuration interface
 */
interface IMultimediaSessionServiceConfiguration {

	int getMessageMaxLength();

	long getMessagingSessionInactivityTimeout(in String serviceId);

	boolean isServiceActivated(in String serviceId);
}
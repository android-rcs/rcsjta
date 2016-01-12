package com.gsma.services.rcs.extension;

/**
 * Multimedia session service configuration interface
 */
interface IMultimediaSessionServiceConfiguration {

	int getMessageMaxLength();

	long getInactivityTimeout();

	boolean isServiceActivated(in String serviceId);
}
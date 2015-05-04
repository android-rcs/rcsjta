package com.gsma.services.rcs.chat;

/**
 * Chat service configuration interface
 */
interface IChatServiceConfiguration {

	long getIsComposingTimeout();

	long getGeolocExpirationTime();
	
	int getGeolocLabelMaxLength();
	
	int getGroupChatMaxParticipants();
	
	int getGroupChatMessageMaxLength();
	
	int getGroupChatMinParticipants();
	
	int getGroupChatSubjectMaxLength();
	
	int getOneToOneChatMessageMaxLength();

	boolean isChatWarnSF();
	
	boolean isGroupChatSupported();
	
	boolean isRespondToDisplayReportsEnabled();
	
	boolean isSmsFallback();
	
	void setRespondToDisplayReports(in boolean enable);
}
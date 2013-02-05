/*
 * Copyright 2013, France Telecom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gsma.rcs.provider.eab;


public final class ContactsManager
{
  // Fields

  // Constructors

  private ContactsManager(android.content.Context arg1){
  }
  // Methods

  public static ContactsManager getInstance(){
    return (ContactsManager) null;
  }
  public static synchronized void createInstance(android.content.Context arg1){
  }
  public org.gsma.rcs.service.api.client.presence.PresenceInfo getMyPresenceInfo(){
    return (org.gsma.rcs.service.api.client.presence.PresenceInfo) null;
  }
  public void revokeContact(java.lang.String arg1) throws ContactsManagerException{
  }
  public void unrevokeContact(java.lang.String arg1) throws ContactsManagerException{
  }
  public void unblockContact(java.lang.String arg1) throws ContactsManagerException{
  }
  public java.lang.String [] getRcsMimeTypes(){
    return (java.lang.String []) null;
  }
  public org.gsma.rcs.service.api.client.contacts.ContactInfo getContactInfo(java.lang.String arg1){
    return (org.gsma.rcs.service.api.client.contacts.ContactInfo) null;
  }
  public java.util.List<java.lang.String> getRcsContactsWithSocialPresence(){
    return (java.util.List) null;
  }
  public java.util.List<java.lang.String> getAllContacts(){
    return (java.util.List) null;
  }
  public java.util.List<java.lang.String> getRcsContacts(){
    return (java.util.List) null;
  }
  public java.util.List<java.lang.String> getAvailableContacts(){
    return (java.util.List) null;
  }
  public java.util.List<java.lang.String> getRcsBlockedContacts(){
    return (java.util.List) null;
  }
  public java.util.List<java.lang.String> getRcsInvitedContacts(){
    return (java.util.List) null;
  }
  public java.util.List<java.lang.String> getRcsWillingContacts(){
    return (java.util.List) null;
  }
  public java.util.List<java.lang.String> getRcsCancelledContacts(){
    return (java.util.List) null;
  }
  public boolean isRcsValidNumber(java.lang.String arg1){
    return false;
  }
  public boolean isImBlockedForContact(java.lang.String arg1){
    return false;
  }
  public boolean isNumberBlocked(java.lang.String arg1){
    return false;
  }
  public boolean isNumberShared(java.lang.String arg1){
    return false;
  }
  public boolean isNumberInvited(java.lang.String arg1){
    return false;
  }
  public boolean isNumberWilling(java.lang.String arg1){
    return false;
  }
  public boolean isNumberCancelled(java.lang.String arg1){
    return false;
  }
  public void setImBlockedForContact(java.lang.String arg1, boolean arg2){
  }
  public java.util.List<java.lang.String> getImBlockedContacts(){
    return (java.util.List) null;
  }
  public java.util.List<java.lang.String> getImSessionCapableContacts(){
    return (java.util.List) null;
  }
  public java.util.List<java.lang.String> getRichcallCapableContacts(){
    return (java.util.List) null;
  }
  public void removeCancelledPresenceInvitation(java.lang.String arg1){
  }
  public void setMyInfo(org.gsma.rcs.service.api.client.presence.PresenceInfo arg1) throws ContactsManagerException{
  }
  public void setMyPhotoIcon(org.gsma.rcs.service.api.client.presence.PhotoIcon arg1) throws ContactsManagerException{
  }
  public void setContactPhotoIcon(java.lang.String arg1, org.gsma.rcs.service.api.client.presence.PhotoIcon arg2) throws ContactsManagerException{
  }
  public void removeMyPhotoIcon() throws ContactsManagerException{
  }
  public void setContactInfo(org.gsma.rcs.service.api.client.contacts.ContactInfo arg1, org.gsma.rcs.service.api.client.contacts.ContactInfo arg2) throws ContactsManagerException{
  }
  public long getAssociatedRcsRawContact(long arg1, java.lang.String arg2){
    return 0l;
  }
  public long createRcsContact(org.gsma.rcs.service.api.client.contacts.ContactInfo arg1, long arg2){
    return 0l;
  }
  public void removeContactPhotoIcon(java.lang.String arg1) throws ContactsManagerException{
  }
  public void setContactSharingStatus(java.lang.String arg1, java.lang.String arg2, java.lang.String arg3) throws ContactsManagerException{
  }
  public int getContactSharingStatus(java.lang.String arg1){
    return 0;
  }
  public void blockContact(java.lang.String arg1) throws ContactsManagerException{
  }
  public void flushContactProvider(){
  }
  public void modifyRcsContactInProvider(java.lang.String arg1, int arg2){
  }
  public boolean isContactRcsActive(java.lang.String arg1){
    return false;
  }
  public void setContactCapabilities(java.lang.String arg1, org.gsma.rcs.service.api.client.capability.Capabilities arg2, int arg3, int arg4){
  }
  public void setContactCapabilities(java.lang.String arg1, org.gsma.rcs.service.api.client.capability.Capabilities arg2){
  }
  public org.gsma.rcs.service.api.client.capability.Capabilities getContactCapabilities(java.lang.String arg1){
    return (org.gsma.rcs.service.api.client.capability.Capabilities) null;
  }
  public void setContactCapabilitiesTimestamp(java.lang.String arg1, long arg2){
  }
  public long createMyContact(){
    return 0l;
  }
  public boolean isSimAssociated(long arg1){
    return false;
  }
  public boolean isRcsAssociated(java.lang.String arg1){
    return false;
  }
  public boolean isOnlySimAssociated(java.lang.String arg1){
    return false;
  }
  public boolean isSimAccount(long arg1){
    return false;
  }
  public java.lang.String getContactPhotoEtag(java.lang.String arg1){
    return (java.lang.String) null;
  }
  public void updateStrings(){
  }
  public void cleanRCSEntries(){
  }
  public void deleteRCSEntries(){
  }
}

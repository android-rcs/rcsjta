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

package org.gsma.joyn.contacts;

import java.lang.String;
import java.util.List;

/**
 * Class ConctactManager. Utility methods for interfacing with the
 * Android SDK ContactsProvider.
 *
 * @author Jean-Marc AUFFRET (Orange)
 * @version 1.0
 * @since 1.0
 */
public final class ContactsManager {

  private ContactsManager(android.content.Context context) {
  }

  public static ContactsManager getInstance(){
    return (ContactsManager) null;
  }
  public static synchronized void createInstance(android.content.Context context){
  }
  public org.gsma.joyn.presence.PresenceInfo getMyPresenceInfo(){
    return (org.gsma.joyn.presence.PresenceInfo) null;
  }
  public void revokeContact(String contact) throws ContactsManagerException{
  }
  public void unrevokeContact(String contact) throws ContactsManagerException{
  }
  public void unblockContact(String contact) throws ContactsManagerException{
  }
  public String [] getRcsMimeTypes(){
    return (String []) null;
  }
  public org.gsma.joyn.contacts.ContactInfo getContactInfo(String arg1){
    return (org.gsma.joyn.contacts.ContactInfo) null;
  }
  public List<String> getRcsContactsWithSocialPresence(){
    return (List<String>) null;
  }
  public List<String> getAllContacts(){
    return (List<String>) null;
  }
  public List<String> getRcsContacts(){
    return (List<String>) null;
  }
  public List<String> getAvailableContacts(){
    return (List<String>) null;
  }
  public List<String> getRcsBlockedContacts(){
    return (List<String>) null;
  }
  public List<String> getRcsInvitedContacts(){
    return (List<String>) null;
  }
  public List<String> getRcsWillingContacts(){
    return (List<String>) null;
  }
  public List<String> getRcsCancelledContacts(){
    return (List<String>) null;
  }
  public boolean isRcsValidNumber(String msisdn){
    return false;
  }
  public boolean isImBlockedForContact(String contact){
    return false;
  }
  public boolean isNumberBlocked(String msisdn){
    return false;
  }
  public boolean isNumberShared(String msisdn){
    return false;
  }
  public boolean isNumberInvited(String msisdn){
    return false;
  }
  public boolean isNumberWilling(String msisdn){
    return false;
  }
  public boolean isNumberCancelled(String msisdn){
    return false;
  }
  public void setImBlockedForContact(String contact, boolean flag){
  }
  public List<String> getImBlockedContacts(){
    return (List<String>) null;
  }
  public List<String> getImSessionCapableContacts(){
    return (List<String>) null;
  }
  public List<String> getRichcallCapableContacts(){
    return (List<String>) null;
  }
  public void removeCancelledPresenceInvitation(String contact){
  }
  public void setMyInfo(org.gsma.joyn.presence.PresenceInfo newPresenceInfo) throws ContactsManagerException{
  }
  public void setMyPhotoIcon(org.gsma.joyn.presence.PhotoIcon photo) throws ContactsManagerException{
  }
  public void setContactPhotoIcon(String contact, org.gsma.joyn.presence.PhotoIcon photo) throws ContactsManagerException{
  }
  public void removeMyPhotoIcon() throws ContactsManagerException{
  }
  public void setContactInfo(org.gsma.joyn.contacts.ContactInfo newInfo, org.gsma.joyn.contacts.ContactInfo oldInfo) throws ContactsManagerException{
  }
  public long getAssociatedRcsRawContact(long arg1, String msisdn){
    return 0l;
  }
  public long createRcsContact(org.gsma.joyn.contacts.ContactInfo info, long rawContactId){
    return 0l;
  }
  public void removeContactPhotoIcon(String contact) throws ContactsManagerException{
  }
  public void setContactSharingStatus(String contact, String status, String reason) throws ContactsManagerException{
  }
  public int getContactSharingStatus(String contact){
    return 0;
  }
  public void blockContact(String contact) throws ContactsManagerException{
  }
  public void flushContactProvider(){
  }
  public void modifyRcsContactInProvider(String contact, int rcsStatus){
  }
    public boolean isContactRcsActive(String contact){
    return false;
  }
    public void setContactCapabilities(String contact, org.gsma.joyn.capability.Capabilities capabilities, int contactType, int registrationState){
  }
  public void setContactCapabilities(String contact, org.gsma.joyn.capability.Capabilities capabilities){
  }
  public org.gsma.joyn.capability.Capabilities getContactCapabilities(String contact){
    return (org.gsma.joyn.capability.Capabilities) null;
  }
  public void setContactCapabilitiesTimestamp(String contact, long timestamp){
  }
  public long createMyContact(){
    return 0l;
  }
  public boolean isSimAssociated(long rawContactId){
    return false;
  }
    public boolean isRcsAssociated(String msisdn){
    return false;
  }
  public boolean isOnlySimAssociated(String msisdn){
    return false;
  }
  public boolean isSimAccount(long rawContactId){
    return false;
  }
    public String getContactPhotoEtag(String contact){
    return (String) null;
  }
  public void updateStrings(){
  }
  public void cleanRCSEntries(){
  }
  public void deleteRCSEntries(){
  }
}

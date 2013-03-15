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

public final class ContactsManager {

  private ContactsManager(android.content.Context arg1){
  }

  public static ContactsManager getInstance(){
    return (ContactsManager) null;
  }
  public static synchronized void createInstance(android.content.Context arg1){
  }
  public org.gsma.joyn.presence.PresenceInfo getMyPresenceInfo(){
    return (org.gsma.joyn.presence.PresenceInfo) null;
  }
  public void revokeContact(String arg1) throws ContactsManagerException{
  }
  public void unrevokeContact(String arg1) throws ContactsManagerException{
  }
  public void unblockContact(String arg1) throws ContactsManagerException{
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
  public boolean isRcsValidNumber(String arg1){
    return false;
  }
  public boolean isImBlockedForContact(String arg1){
    return false;
  }
  public boolean isNumberBlocked(String arg1){
    return false;
  }
  public boolean isNumberShared(String arg1){
    return false;
  }
  public boolean isNumberInvited(String arg1){
    return false;
  }
  public boolean isNumberWilling(String arg1){
    return false;
  }
  public boolean isNumberCancelled(String arg1){
    return false;
  }
  public void setImBlockedForContact(String arg1, boolean arg2){
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
  public void removeCancelledPresenceInvitation(String arg1){
  }
  public void setMyInfo(org.gsma.joyn.presence.PresenceInfo arg1) throws ContactsManagerException{
  }
  public void setMyPhotoIcon(org.gsma.joyn.presence.PhotoIcon arg1) throws ContactsManagerException{
  }
  public void setContactPhotoIcon(String arg1, org.gsma.joyn.presence.PhotoIcon arg2) throws ContactsManagerException{
  }
  public void removeMyPhotoIcon() throws ContactsManagerException{
  }
  public void setContactInfo(org.gsma.joyn.contacts.ContactInfo arg1, org.gsma.joyn.contacts.ContactInfo arg2) throws ContactsManagerException{
  }
  public long getAssociatedRcsRawContact(long arg1, String arg2){
    return 0l;
  }
  public long createRcsContact(org.gsma.joyn.contacts.ContactInfo arg1, long arg2){
    return 0l;
  }
  public void removeContactPhotoIcon(String arg1) throws ContactsManagerException{
  }
  public void setContactSharingStatus(String arg1, String arg2, String arg3) throws ContactsManagerException{
  }
  public int getContactSharingStatus(String arg1){
    return 0;
  }
  public void blockContact(String arg1) throws ContactsManagerException{
  }
  public void flushContactProvider(){
  }
  public void modifyRcsContactInProvider(String arg1, int arg2){
  }
  public boolean isContactRcsActive(String arg1){
    return false;
  }
  public void setContactCapabilities(String arg1, org.gsma.joyn.capability.Capabilities arg2, int arg3, int arg4){
  }
  public void setContactCapabilities(String arg1, org.gsma.joyn.capability.Capabilities arg2){
  }
  public org.gsma.joyn.capability.Capabilities getContactCapabilities(String arg1){
    return (org.gsma.joyn.capability.Capabilities) null;
  }
  public void setContactCapabilitiesTimestamp(String arg1, long arg2){
  }
  public long createMyContact(){
    return 0l;
  }
  public boolean isSimAssociated(long arg1){
    return false;
  }
  public boolean isRcsAssociated(String arg1){
    return false;
  }
  public boolean isOnlySimAssociated(String arg1){
    return false;
  }
  public boolean isSimAccount(long arg1){
    return false;
  }
  public String getContactPhotoEtag(String arg1){
    return (String) null;
  }
  public void updateStrings(){
  }
  public void cleanRCSEntries(){
  }
  public void deleteRCSEntries(){
  }
}

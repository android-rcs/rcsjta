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

package org.gsma.rcs.service.api.client.contacts;


public class ContactsApi
{
  // Constructors

  public ContactsApi(android.content.Context arg1){
  }
  // Methods

  public java.lang.String [] getRcsMimeTypes(){
    return (java.lang.String []) null;
  }
  public ContactInfo getContactInfo(java.lang.String arg1){
    return (ContactInfo) null;
  }
  public java.util.List<java.lang.String> getRcsContactsWithSocialPresence(){
    return (java.util.List<java.lang.String>) null;
  }
  public java.util.List<java.lang.String> getAllContacts(){
    return (java.util.List<java.lang.String>) null;
  }
  public java.util.List<java.lang.String> getRcsContacts(){
    return (java.util.List<java.lang.String>) null;
  }
  public java.util.List<java.lang.String> getRcsContactsAvailable(){
    return (java.util.List<java.lang.String>) null;
  }
  public java.util.List<java.lang.String> getRcsBlockedContacts(){
    return (java.util.List<java.lang.String>) null;
  }
  public java.util.List<java.lang.String> getRcsInvitedContacts(){
    return (java.util.List<java.lang.String>) null;
  }
  public java.util.List<java.lang.String> getRcsWillingContacts(){
    return (java.util.List<java.lang.String>) null;
  }
  public java.util.List<java.lang.String> getRcsCancelledContacts(){
    return (java.util.List<java.lang.String>) null;
  }
  public boolean isRcsValidNumber(java.lang.String arg1){
    return false;
  }
  public boolean isContactImBlocked(java.lang.String arg1){
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
  public java.util.List<java.lang.String> getBlockedContactsForIm(){
    return (java.util.List<java.lang.String>) null;
  }
  public java.util.List<java.lang.String> getImSessionCapableContacts(){
    return (java.util.List<java.lang.String>) null;
  }
  public java.util.List<java.lang.String> getRichcallCapableContacts(){
    return (java.util.List<java.lang.String>) null;
  }
  public void removeCancelledPresenceInvitation(java.lang.String arg1){
  }
  public void blockCapability(java.lang.String arg1, java.lang.String arg2){
  }
  public void blockAllCapabilities(java.lang.String arg1){
  }
  public java.util.List<java.lang.String> getBlockedCapabilities(java.lang.String arg1){
    return (java.util.List<java.lang.String>) null;
  }
}

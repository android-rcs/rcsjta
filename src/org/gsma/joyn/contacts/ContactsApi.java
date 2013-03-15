/*
 * Copyright 2013, France Telecom
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 *    http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gsma.joyn.contacts;

import java.lang.String;

public class ContactsApi {
  // Constructors

  public ContactsApi(android.content.Context arg1){
  }
  // Methods

  public String [] getRcsMimeTypes(){
    return (String []) null;
  }
  public ContactInfo getContactInfo(String arg1){
    return (ContactInfo) null;
  }
  public java.util.List<String> getRcsContactsWithSocialPresence(){
    return (java.util.List<String>) null;
  }
  public java.util.List<String> getAllContacts(){
    return (java.util.List<String>) null;
  }
  public java.util.List<String> getRcsContacts(){
    return (java.util.List<String>) null;
  }
  public java.util.List<String> getRcsContactsAvailable(){
    return (java.util.List<String>) null;
  }
  public java.util.List<String> getRcsBlockedContacts(){
    return (java.util.List<String>) null;
  }
  public java.util.List<String> getRcsInvitedContacts(){
    return (java.util.List<String>) null;
  }
  public java.util.List<String> getRcsWillingContacts(){
    return (java.util.List<String>) null;
  }
  public java.util.List<String> getRcsCancelledContacts(){
    return (java.util.List<String>) null;
  }
  public boolean isRcsValidNumber(String arg1){
    return false;
  }
  public boolean isContactImBlocked(String arg1){
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
  public java.util.List<String> getBlockedContactsForIm(){
    return (java.util.List<String>) null;
  }
  public java.util.List<String> getImSessionCapableContacts(){
    return (java.util.List<String>) null;
  }
  public java.util.List<String> getRichcallCapableContacts(){
    return (java.util.List<String>) null;
  }
  public void removeCancelledPresenceInvitation(String arg1){
  }
  public void blockCapability(String arg1, String arg2){
  }
  public void blockAllCapabilities(String arg1){
  }
  public java.util.List<String> getBlockedCapabilities(String arg1){
    return (java.util.List<String>) null;
  }
}

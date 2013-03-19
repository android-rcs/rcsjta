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
 * Class ContactApi.
 *
 * @author Jean-Marc AUFFRET (Orange)
 * @version 1.0
 * @since 1.0
 */
public class ContactsApi {

  public ContactsApi(android.content.Context arg1){
  }

  public String [] getRcsMimeTypes(){
    return (String []) null;
  }
  public ContactInfo getContactInfo(String arg1){
    return (ContactInfo) null;
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
  public List<String> getRcsContactsAvailable(){
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
  public List<String> getBlockedContactsForIm(){
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
  public void blockCapability(String arg1, String arg2){
  }
  public void blockAllCapabilities(String arg1){
  }
  public List<String> getBlockedCapabilities(String arg1){
    return (List<String>) null;
  }
}

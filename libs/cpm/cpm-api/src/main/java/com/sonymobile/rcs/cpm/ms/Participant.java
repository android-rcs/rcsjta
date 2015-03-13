/*
 * Copyright (C) 2015 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.sonymobile.rcs.cpm.ms;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A participant POJO with name and address
 * 
 * @see CpmGroupState
 */
public class Participant {

    private final String mName;

    private final String mCommunicationAddress;

    /**
     * @param name
     * @param communicationAddress
     */
    public Participant(String name, String communicationAddress) {
        super();
        mName = name.trim();
        mCommunicationAddress = communicationAddress.trim();
    }

    @Override
    public String toString() {
        return mName + " <" + mCommunicationAddress + ">";
    }

    @Override
    public boolean equals(Object obj) {
        return hashCode() == obj.hashCode();
    }

    @Override
    public int hashCode() {
        return mCommunicationAddress.hashCode();
    }

    /**
     * As a string separated by semicolon
     * 
     * @param li
     * @return
     */
    public static String asString(List<Participant> li) {
        String s = "";
        for (int i = 0; i < li.size(); i++) {
            if (i > 0)
                s += ";";
            s += li.get(i).toString();
        }

        return s;
    }

    /**
     * Converts string(s) to a list of participants
     * 
     * @param namesAndAddresses
     * @return the list of participants
     * @see Participant#parseString(String)
     */
    public static Set<Participant> asSet(String... namesAndAddresses) {
        if (namesAndAddresses.length == 1) {
            namesAndAddresses = namesAndAddresses[0].split(";");
        }

        Set<Participant> li = new HashSet<Participant>();
        for (String n : namesAndAddresses) {
            li.add(parseString(n));
        }
        return li;
    }

    /**
     * Convert a string to a participant, pattern is "name &lt; email &gt;"
     * 
     * @param n
     * @return a participant
     */
    public static Participant parseString(String n) {
        // someone name <email address>
        String name = n;
        String addr = null;
        int i = n.indexOf('<');
        int j = n.indexOf('@');
        if (i != -1) {
            name = n.substring(0, i);
            addr = n.substring(i + 1, n.indexOf('>'));
        } else if (j != -1) {
            addr = n.trim(); // no name just address
        }
        return new Participant(name, addr);
    }

    /**
     * @return
     */
    public String getName() {
        return mName;
    }

    /**
     * @return
     */
    public String getCommunicationAddress() {
        return mCommunicationAddress;
    }

}

/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.orangelabs.rcs.core.ims.service.presence.pidf;

import java.util.Vector;

import com.orangelabs.rcs.core.ims.service.presence.pidf.geoloc.Geopriv;

/**
 * PIDF presence document
 * 
 * @author jexa7410
 */
public class PidfDocument {
    private String entity = null;
    private Vector<Tuple> tuplesList = new Vector<Tuple>();
    private Geopriv geopriv = null;
    private Person person = null;

    public PidfDocument(String entity) {
        this.entity = entity;
    }

    public String getEntity() {
        return entity;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person newPerson) {
        if ((person == null) || (newPerson.getTimestamp() >= person.getTimestamp())) {
            person = newPerson;
        }
    }

    public void addTuple(Tuple newTuple) {
        Tuple foundTuple = null;
        String newServiceId = newTuple.getService().getId();
        for (int i = 0; i < tuplesList.size(); i++) {
            Tuple tuple = (Tuple) tuplesList.elementAt(i);
            String serviceId = tuple.getService().getId();
            if (serviceId.equalsIgnoreCase(newServiceId)) {
                foundTuple = tuple;
                break;
            }
        }

        if (foundTuple == null) {
            tuplesList.addElement(newTuple);
        } else if (newTuple.getTimestamp() >= foundTuple.getTimestamp()) {
            tuplesList.remove(foundTuple);
            tuplesList.addElement(newTuple);
        }
    }

    public Vector<Tuple> getTuplesList() {
        return tuplesList;
    }

    public void setTuplesList(Vector<Tuple> tuplesList) {
        this.tuplesList = tuplesList;
    }

    public void setGeopriv(Geopriv geopriv) {
        this.geopriv = geopriv;
    }

    public Geopriv getGeopriv() {
        return geopriv;
    }
}

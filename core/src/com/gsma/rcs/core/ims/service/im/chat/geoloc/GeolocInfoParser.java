/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2015 Sony Mobile Communications Inc.
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
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.core.ims.service.im.chat.geoloc;

import java.io.IOException;
import java.util.StringTokenizer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.gsma.rcs.core.ParseFailureException;
import com.gsma.rcs.utils.DateUtils;
import com.gsma.rcs.utils.logger.Logger;

/**
 * Geolocation info parser
 * 
 * @author vfml3370
 */
public class GeolocInfoParser extends DefaultHandler {

        /*
     * Geoloc-Info SAMPLE: <?xml version="1.0" encoding="UTF-8"?> <rcsenvelope
     * xmlns="urn:gsma:params:xml:ns:rcs:rcs:geolocation"
     * xmlns:rpid="urn:ietf:params:xml:ns:pidf:rpid"
     * xmlns:gp="urn:ietf:params:xml:ns:pidf:geopriv10" xmlns:gml="http://www.opengis.net/gml"
     * xmlns:gs="http://www.opengis.net/pidflo/1.0" entity="tel:+12345678901"> <rcspushlocation
     * id="a123" label ="meeting location" > <rpid:place-type
     * rpid:until="2012-03-15T21:00:00-05:00"> </rpid:place-type> <rpid:time-offset
     * rpid:until="2012-03-15T21:00:00-05:00"></rpid:time-offset> <gp:geopriv> <gp:location-info>
     * <gs:Circle srsName="urn:ogc:def:crs:EPSG::4326"> <gml:pos>48.731964 -3.45829</gml:pos>
     * <gs:radius uom="urn:ogc:def:uom:EPSG::9001">10</gs:radius> </gs:Circle> </gp:location-info>
     * <gp:usage-rules> <gp:retention-expiry>2012-03-15T21:00:00-05:00</gp:retention-expiry>
     * </gp:usage-rules> </gp:geopriv> <timestamp>2012-03-15T16:09:44-05:00</timestamp>
     * </rcspushlocation> </rcsenvelope>
     */

    private StringBuffer accumulator;
    private GeolocInfoDocument geoloc = null;

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    private final InputSource mInputSource;

    /**
     * Constructor
     * 
     * @param inputSource Input source
     */
    public GeolocInfoParser(InputSource inputSource) {
        mInputSource = inputSource;
    }

    /**
     * Parse the PIDF input
     * 
     * @return GeolocInfoParser
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws ParseFailureException
     */
    public GeolocInfoParser parse() throws ParserConfigurationException, SAXException,
            ParseFailureException {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            parser.parse(mInputSource, this);
            return this;

        } catch (IOException e) {
            throw new ParseFailureException("Failed to parse input source!", e);
        }
    }

    public GeolocInfoDocument getGeoLocInfo() {
        return geoloc;
    }

    /**
     * StartDocument
     */
    public void startDocument() {
        if (logger.isActivated()) {
            logger.debug("Start document");
        }
        accumulator = new StringBuffer();
    }

    public void characters(char buffer[], int start, int length) {
        accumulator.append(buffer, start, length);
    }

    /**
     * StartElement
     * @param namespaceURL
     * @param localName
     * @param qname
     * @param attr
     */
    public void startElement(String namespaceURL, String localName, String qname, Attributes attr) {
        accumulator.setLength(0);
        if ("rcsenvelope".equals(localName)) {
            String entity = attr.getValue("entity").trim();
            geoloc = new GeolocInfoDocument(entity);
        } else if ("rcspushlocation".equals(localName)) {
            if (logger.isActivated()) {
                logger.debug("rcspushlocation");
            }
            if (geoloc != null) {
                String label = attr.getValue("label").trim();
                geoloc.setLabel(label);
            }
        }
    }

    /**
     * endElement
     * @param namespaceURL
     * @param localName
     * @param qname
     */
    public void endElement(String namespaceURL, String localName, String qname) {
        if ("radius".equals(localName)) {
            if (geoloc != null) {
                geoloc.setRadius(Float.parseFloat(accumulator.toString().trim()));
            }
        } else if ("retention-expiry".equals(localName)) {
            if (geoloc != null) {
                geoloc.setExpiration(DateUtils.decodeDate(accumulator.toString().trim()));
            }
        } else if ("pos".equals(localName)) {
            if (geoloc != null) {
                StringTokenizer st = new StringTokenizer(accumulator.toString().trim());
                if (st.hasMoreTokens()) {
                    geoloc.setLatitude(Double.parseDouble(st.nextToken()));
                }
                if (st.hasMoreTokens()) {
                    geoloc.setLongitude(Double.parseDouble(st.nextToken()));
                }
            }
        }
    }

    public void endDocument() {
        if (logger.isActivated()) {
            logger.debug("End document");
        }
    }
}

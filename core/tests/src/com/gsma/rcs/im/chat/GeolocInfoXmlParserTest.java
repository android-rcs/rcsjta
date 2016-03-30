/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 ORANGE.
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

package com.gsma.rcs.im.chat;

import com.gsma.rcs.core.ParseFailureException;
import com.gsma.rcs.core.ims.service.im.chat.geoloc.GeolocInfoDocument;
import com.gsma.rcs.core.ims.service.im.chat.geoloc.GeolocInfoParser;
import com.gsma.rcs.utils.DateUtils;

import android.test.AndroidTestCase;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;

import javax.xml.parsers.ParserConfigurationException;


public class GeolocInfoXmlParserTest extends AndroidTestCase {

    private static final String sXmlContentToParse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<rcsenvelope entity=\"tel:+12345678901\" xmlns=\"urn:gsma:params:xml:ns:rcs:rcs:geolocation\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:gp=\"urn:ietf:params:xml:ns:pidf:geopriv10\" xmlns:gs=\"http://www.opengis.net/pidflo/1.0\" xmlns:rpid=\"urn:ietf:params:xml:ns:pidf:rpid\">\n"
            + "     <rcspushlocation id=\"a123\" label=\"meeting location\">\n"
            + "          <rpid:place-type rpid:until=\"2012-03-15T21:00:00-05:00\"/>\n"
            + "          <rpid:time-offset rpid:until=\"2012-03-15T21:00:00-05:00\"/>\n"
            + "          <gp:geopriv>\n"
            + "               <gp:location-info>\n"
            + "                    <gs:Circle srsName=\"urn:ogc:def:crs:EPSG::4326\">\n"
            + "                         <gml:pos>48.731964 -3.45829</gml:pos>\n"
            + "                         <gs:radius uom=\"urn:ogc:def:uom:EPSG::9001\">10</gs:radius>\n"
            + "                    </gs:Circle>\n"
            + "               </gp:location-info>\n"
            + "               <gp:usage-rules>\n"
            + "                    <gp:retention-expiry>2012-03-15T21:00:00-05:00</gp:retention-expiry>\n"
            + "               </gp:usage-rules>\n" + "          </gp:geopriv>\n"
            + "          <timestamp>2012-03-15T16:09:44-05:00</timestamp>\n"
            + "     </rcspushlocation>\n" + "</rcsenvelope>";

    public void testGeolocXmlParser() throws ParseFailureException, SAXException,
            ParserConfigurationException {
        GeolocInfoParser parser = new GeolocInfoParser(new InputSource(new ByteArrayInputStream(
                sXmlContentToParse.getBytes())));
        parser.parse();
        GeolocInfoDocument info = parser.getGeoLocInfo();
        assertEquals(48.731964, info.getLatitude());
        assertEquals(-3.45829, info.getLongitude());
        assertEquals(10.0f, info.getRadius());
        assertEquals("tel:+12345678901", info.getEntity());
        assertEquals(DateUtils.decodeDate("2012-03-15T21:00:00-05:00"), info.getExpiration());
    }
}

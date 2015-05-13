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

package com.gsma.services.rcs.contact;

import com.gsma.services.rcs.RcsGenericException;
import com.gsma.services.rcs.RcsPermissionDeniedException;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.SparseArray;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for validation and unique formatting of phone numbers
 * 
 * @author YPLO6403
 */
public class ContactUtil {

    /**
     * Singleton of ContactUtil
     */
    private static volatile ContactUtil sInstance;

    /**
     * The country code of the device
     */
    private String mCountryCode;

    /**
     * The country area code
     */
    private String mCountryAreaCode;

    /**
     * Application context
     */
    private final Context mCtx;

    /**
     * Regular expression to validate phone numbers
     */
    private final static String REGEXP_CONTACT = "^00\\d{1,15}$|^[+]?\\d{1,15}$|^\\d{1,15}$";

    /**
     * Pattern to check contact
     */
    private final static Pattern PATTERN_CONTACT = Pattern.compile(REGEXP_CONTACT);

    private final static String MSISDN_PREFIX_INTERNATIONAL = "00";

    private final static String COUNTRY_CODE_PREFIX = "+";

    /**
     * Index of Country Code in the array
     */
    private static final int COUNTRY_CODE_IDX = 0;
    /**
     * Index of Country Area Code in the array
     */
    private static final int COUNTRY_AREA_CODE_IDX = 1;

    /**
     * A map between the ISO country code and an array of String containing first the County Code
     * and secondly the Area Code.<br>
     * Note 1 : the Area Code is optional (if it does not exist then it is null).<br>
     * Note 2 : the Country Code is optional (if it does not exist then the string array is null).
     */
    // @formatter:off
      
    private static final SparseArray<String[]> sCountryCodes = new SparseArray<String[]> () {

        {
            {
                put( 202 , new String[] {"30" }); // Greece
                put( 204 , new String[] {"31" , "0" }); // Netherlands (Kingdom of the)
                put( 206 , new String[] {"32" , "0" }); // Belgium
                put( 208 , new String[] {"33" , "0" }); // France
                put( 212 , new String[] {"377" }); // Monaco (Principality of)
                put( 213 , new String[] {"376" }); // Andorra (Principality of)
                put( 214 , new String[] {"34" }); // Spain
                put( 216 , new String[] {"36" , "06" }); // Hungary (Republic of)
                put( 218 , new String[] {"387" , "0" }); // Bosnia and Herzegovina
                put( 219 , new String[] {"385" , "0" }); // Croatia (Republic of)
                put( 220 , new String[] {"381" , "0" }); // Serbia and Montenegro
                put( 222 , new String[] {"39" }); // Italy
                put( 225 , new String[] {"379" }); // Vatican City State
                put( 226 , new String[] {"40" , "0" }); // Romania
                put( 228 , new String[] {"41" , "0" }); // Switzerland (Confederation of)
                put( 230 , new String[] {"420" , "0" }); // Czech Republic
                put( 231 , new String[] {"421" , "0" }); // Slovak Republic
                put( 232 , new String[] {"43" , "0" }); // Austria
                put( 234 , new String[] {"44" , "0" }); // United Kingdom of Great Britain and Northern Ireland
                put( 235 , new String[] {"44" , "0" }); // United Kingdom of Great Britain and Northern Ireland
                put( 238 , new String[] {"45" }); // Denmark
                put( 240 , new String[] {"46" , "0" }); // Sweden
                put( 242 , new String[] {"47" }); // Norway
                put( 244 , new String[] {"358" , "0" }); // Finland
                put( 246 , new String[] {"370" , "8" }); // Lithuania (Republic of)
                put( 247 , new String[] {"371" }); // Latvia (Republic of)
                put( 248 , new String[] {"372" }); // Estonia (Republic of)
                put( 250 , new String[] {"7" , "8" }); // Russian Federation
                put( 255 , new String[] {"380" , "0" }); // Ukraine
                put( 257 , new String[] {"375" , "8" }); // Belarus (Republic of)
                put( 259 , new String[] {"373" , "0" }); // Moldova (Republic of)
                put( 260 , new String[] {"48" }); // Poland (Republic of)
                put( 262 , new String[] {"49" , "0" }); // Germany (Federal Republic of)
                put( 266 , new String[] {"350" }); // Gibraltar
                put( 268 , new String[] {"351" }); // Portugal
                put( 270 , new String[] {"352" }); // Luxembourg
                put( 272 , new String[] {"353" , "0" }); // Ireland
                put( 274 , new String[] {"354" }); // Iceland
                put( 276 , new String[] {"355" , "0" }); // Albania (Republic of)
                put( 278 , new String[] {"356" }); // Malta
                put( 280 , new String[] {"357" }); // Cyprus (Republic of)
                put( 282 , new String[] {"955" , "8" }); // Georgia
                put( 283 , new String[] {"374" , "0" }); // Armenia (Republic of)
                put( 284 , new String[] {"359" , "0" }); // Bulgaria (Republic of)
                put( 286 , new String[] {"90" , "0" }); // Turkey
                put( 288 , new String[] {"298" }); // Faroe Islands
                put( 289 , new String[] {"955" , "8" }); // Abkhazia (Georgia)
                put( 290 , new String[] {"299" }); // Greenland (Denmark)
                put( 292 , new String[] {"378" }); // San Marino (Republic of)
                put( 293 , new String[] {"386" }); // Slovenia (Republic of)
                put( 294 , new String[] {"389" , "0" }); // The Former Yugoslav Republic of Macedonia
                put( 295 , new String[] {"423" }); // Liechtenstein (Principality of)
                put( 297 , new String[] {"382" , "0" }); // Montenegro (Republic of)
                put( 302 , new String[] {"1" , "1" }); // Canada
                put( 308 , new String[] {"508" }); // Saint Pierre and Miquelon (Collectivite territoriale de la Republique francaise)
                put( 310 , new String[] {"1" , "1" }); // United States of America
                put( 311 , new String[] {"1" , "1" }); // United States of America
                put( 312 , new String[] {"1" , "1" }); // United States of America
                put( 313 , new String[] {"1" , "1" }); // United States of America
                put( 314 , new String[] {"1" , "1" }); // United States of America
                put( 315 , new String[] {"1" , "1" }); // United States of America
                put( 316 , new String[] {"1" , "1" }); // United States of America
                put( 330 , new String[] {"1-787" , "1" }); // Puerto Rico
                put( 332 , new String[] {"1-340" , "1" }); // United States Virgin Islands
                put( 334 , new String[] {"52" , "01" }); // Mexico
                put( 338 , new String[] {"1-876" , "1" }); // Jamaica
                put( 340 , new String[] {"590" , "0" }); // Guadeloupe (French Department of)
                put( 342 , new String[] {"1-246" , "1" }); // Barbados
                put( 344 , new String[] {"1-268" , "1" }); // Antigua and Barbuda
                put( 346 , new String[] {"1-345" , "1" }); // Cayman Islands
                put( 348 , new String[] {"1-284" , "1" }); // British Virgin Islands
                put( 350 , new String[] {"1-441" , "1" }); // Bermuda
                put( 352 , new String[] {"1-473" , "1" }); // Grenada
                put( 354 , new String[] {"1-664" , "1" }); // Montserrat
                put( 356 , new String[] {"1-869" , "1" }); // Saint Kitts and Nevis
                put( 358 , new String[] {"1-758" , "1" }); // Saint Lucia
                put( 360 , new String[] {"1-784" , "1" }); // Saint Vincent and the Grenadines
                put( 362 , new String[] {"1-264" , "1" }); // Netherlands Antilles
                put( 363 , new String[] {"297" }); // Aruba
                put( 364 , new String[] {"1-242" , "1" }); // Bahamas (Commonwealth of the)
                put( 365 , new String[] {"1-264" , "1" }); // Anguilla
                put( 366 , new String[] {"1-767" , "1" }); // Dominica (Commonwealth of)
                put( 368 , new String[] {"53" , "0" }); // Cuba
                put( 370 , new String[] {"1-809" , "1" }); // Dominican Republic
                put( 372 , new String[] {"509" }); // Haiti (Republic of)
                put( 374 , new String[] {"1-868" , "1" }); // Trinidad and Tobago
                put( 376 , new String[] {"1-649" , "1" }); // Turks and Caicos Islands
                put( 400 , new String[] {"994" , "0" }); // Azerbaijani Republic
                put( 401 , new String[] {"7-7" , "8" }); // Kazakhstan (Republic of)
                put( 402 , new String[] {"975" }); // Bhutan (Kingdom of)
                put( 404 , new String[] {"91" , "0" }); // India (Republic of)
                put( 405 , new String[] {"91" , "0" }); // India (Republic of)
                put( 410 , new String[] {"92" , "0" }); // Pakistan (Islamic Republic of)
                put( 412 , new String[] {"93" , "0" }); // Afghanistan
                put( 413 , new String[] {"94" , "0" }); // Sri Lanka (Democratic Socialist Republic of)
                put( 414 , new String[] {"95" }); // Myanmar (Union of)
                put( 415 , new String[] {"961" , "0" }); // Lebanon
                put( 416 , new String[] {"962" , "0" }); // Jordan (Hashemite Kingdom of)
                put( 417 , new String[] {"963" , "0" }); // Syrian Arab Republic
                put( 418 , new String[] {"964" }); // Iraq (Republic of)
                put( 419 , new String[] {"965" }); // Kuwait (State of)
                put( 420 , new String[] {"966" , "0" }); // Saudi Arabia (Kingdom of)
                put( 421 , new String[] {"967" , "0" }); // Yemen (Republic of)
                put( 422 , new String[] {"968" }); // Oman (Sultanate of)
                put( 424 , new String[] {"971" , "0" }); // United Arab Emirates
                put( 425 , new String[] {"972" , "0" }); // Israel (State of)
                put( 426 , new String[] {"973" }); // Bahrain (Kingdom of)
                put( 427 , new String[] {"974" }); // Qatar (State of)
                put( 428 , new String[] {"976" , "0" }); // Mongolia
                put( 429 , new String[] {"977" , "0" }); // Nepal
                put( 430 , new String[] {"971" , "0" }); // United Arab Emirates
                put( 431 , new String[] {"971" , "0" }); // United Arab Emirates
                put( 432 , new String[] {"98" , "0" }); // Iran (Islamic Republic of)
                put( 434 , new String[] {"998" , "8" }); // Uzbekistan (Republic of)
                put( 436 , new String[] {"992" , "8" }); // Tajikistan (Republic of)
                put( 437 , new String[] {"996" , "0" }); // Kyrgyz Republic
                put( 438 , new String[] {"993" , "8" }); // Turkmenistan
                put( 440 , new String[] {"81" , "0" }); // Japan
                put( 441 , new String[] {"81" , "0" }); // Japan
                put( 450 , new String[] {"82" , "0" }); // Korea (Republic of)
                put( 452 , new String[] {"84" , "0" }); // Viet Nam (Socialist Republic of)
                put( 454 , new String[] {"852" }); // "Hong Kong, China"
                put( 455 , new String[] {"853" }); // "Macao, China"
                put( 456 , new String[] {"855" , "0" }); // Cambodia (Kingdom of)
                put( 457 , new String[] {"856" }); // Lao People's Democratic Republic
                put( 460 , new String[] {"86" , "0" }); // China (People's Republic of)
                put( 461 , new String[] {"86" , "0" }); // China (People's Republic of)
                put( 466 , new String[] {"886" , "0" }); // "Taiwan, China"
                put( 467 , new String[] {"850" }); // Democratic People's Republic of Korea
                put( 470 , new String[] {"880" , "0" }); // Bangladesh (People's Republic of)
                put( 472 , new String[] {"960" }); // Maldives (Republic of)
                put( 502 , new String[] {"60" , "0" }); // Malaysia
                put( 505 , new String[] {"61" , "0" }); // Australia
                put( 510 , new String[] {"62" , "0" }); // Indonesia (Republic of)
                put( 514 , new String[] {"670" }); // Democratic Republic of Timor-Leste
                put( 515 , new String[] {"63" , "0" }); // Philippines (Republic of the)
                put( 520 , new String[] {"66" , "0" }); // Thailand
                put( 525 , new String[] {"65" }); // Singapore (Republic of)
                put( 528 , new String[] {"673" }); // Brunei Darussalam
                put( 530 , new String[] {"64" , "0" }); // New Zealand
                put( 534 , new String[] {"1-670" , "1" }); // Northern Mariana Islands (Commonwealth of the)
                put( 535 , new String[] {"1-671" , "1" }); // Guam
                put( 536 , new String[] {"674" }); // Nauru (Republic of)
                put( 537 , new String[] {"675" }); // Papua New Guinea
                put( 539 , new String[] {"676" }); // Tonga (Kingdom of)
                put( 540 , new String[] {"677" }); // Solomon Islands
                put( 541 , new String[] {"678" }); // Vanuatu (Republic of)
                put( 542 , new String[] {"679" }); // Fiji (Republic of)
                put( 543 , new String[] {"681" }); // Wallis and Futuna (Territoire franais d'outre-mer)
                put( 544 , new String[] {"1-684" , "1" }); // American Samoa
                put( 545 , new String[] {"686" }); // Kiribati (Republic of)
                put( 546 , new String[] {"687" }); // New Caledonia (Territoire franais d'outre-mer)
                put( 547 , new String[] {"689" }); // French Polynesia (Territoire franais d'outre-mer)
                put( 548 , new String[] {"682" }); // Cook Islands
                put( 549 , new String[] {"685" }); // Samoa (Independent State of)
                put( 550 , new String[] {"691" , "1" }); // Micronesia (Federated States of)
                put( 551 , new String[] {"692" , "1" }); // Marshall Islands (Republic of the)
                put( 552 , new String[] {"680" }); // Palau (Republic of)
                put( 602 , new String[] {"20" , "0" }); // Egypt (Arab Republic of)
                put( 603 , new String[] {"213" , "0" }); // Algeria (People's Democratic Republic of)
                put( 604 , new String[] {"212" , "0" }); // Morocco (Kingdom of)
                put( 605 , new String[] {"216" }); // Tunisia
                put( 606 , new String[] {"281" , "0" }); // Libya (Socialist People's Libyan Arab Jamahiriya)
                put( 607 , new String[] {"220" }); // Gambia (Republic of the)
                put( 608 , new String[] {"221" }); // Senegal (Republic of)
                put( 609 , new String[] {"222" }); // Mauritania (Islamic Republic of)
                put( 610 , new String[] {"223" }); // Mali (Republic of)
                put( 611 , new String[] {"224" }); // Guinea (Republic of)
                put( 612 , new String[] {"225" }); // Cte d'Ivoire (Republic of)
                put( 613 , new String[] {"226" }); // Burkina Faso
                put( 614 , new String[] {"227" }); // Niger (Republic of the)
                put( 615 , new String[] {"228" }); // Togolese Republic
                put( 616 , new String[] {"229" }); // Benin (Republic of)
                put( 617 , new String[] {"230" }); // Mauritius (Republic of)
                put( 618 , new String[] {"231" }); // Liberia (Republic of)
                put( 619 , new String[] {"232" , "0" }); // Sierra Leone
                put( 620 , new String[] {"233" , "0" }); // Ghana
                put( 621 , new String[] {"234" , "0" }); // Nigeria (Federal Republic of)
                put( 622 , new String[] {"235" }); // Chad (Republic of)
                put( 623 , new String[] {"236" }); // Central African Republic
                put( 624 , new String[] {"237" }); // Cameroon (Republic of)
                put( 625 , new String[] {"238" }); // Cape Verde (Republic of)
                put( 626 , new String[] {"239" }); // Sao Tome and Principe (Democratic Republic of)
                put( 627 , new String[] {"240" }); // Equatorial Guinea (Republic of)
                put( 628 , new String[] {"241" }); // Gabonese Republic
                put( 629 , new String[] {"242" }); // Congo (Republic of the)
                put( 630 , new String[] {"242" }); // Democratic Republic of the Congo
                put( 631 , new String[] {"244" , "0" }); // Angola (Republic of)
                put( 632 , new String[] {"245" }); // Guinea-Bissau (Republic of)
                put( 633 , new String[] {"248" }); // Seychelles (Republic of)
                put( 634 , new String[] {"249" , "0" }); // Sudan (Republic of the)
                put( 635 , new String[] {"250" }); // Rwanda (Republic of)
                put( 636 , new String[] {"251" , "0" }); // Ethiopia (Federal Democratic Republic of)
                put( 637 , new String[] {"252" }); // Somali Democratic Republic
                put( 638 , new String[] {"253" }); // Djibouti (Republic of)
                put( 639 , new String[] {"254" , "0" }); // Kenya (Republic of)
                put( 640 , new String[] {"255" , "0" }); // Tanzania (United Republic of)
                put( 641 , new String[] {"256" , "0" }); // Uganda (Republic of)
                put( 642 , new String[] {"257" }); // Burundi (Republic of)
                put( 643 , new String[] {"258" }); // Mozambique (Republic of)
                put( 645 , new String[] {"260" , "0" }); // Zambia (Republic of)
                put( 646 , new String[] {"261" }); // Madagascar (Republic of)
                put( 647 , new String[] {"262" , "0" }); // Reunion (French Department of)
                put( 648 , new String[] {"263" , "0" }); // Zimbabwe (Republic of)
                put( 649 , new String[] {"264" , "0" }); // Namibia (Republic of)
                put( 650 , new String[] {"265" }); // Malawi
                put( 651 , new String[] {"266" }); // Lesotho (Kingdom of)
                put( 652 , new String[] {"267" }); // Botswana (Republic of)
                put( 653 , new String[] {"268" }); // Swaziland (Kingdom of)
                put( 654 , new String[] {"269" }); // Comoros (Union of the)
                put( 655 , new String[] {"27" , "0" }); // South Africa (Republic of)
                put( 657 , new String[] {"291" , "0" }); // Eritrea
                put( 702 , new String[] {"501" }); // Belize
                put( 704 , new String[] {"502" }); // Guatemala (Republic of)
                put( 706 , new String[] {"503" }); // El Salvador (Republic of)
                put( 708 , new String[] {"504" }); // Honduras (Republic of)
                put( 710 , new String[] {"505" }); // Nicaragua
                put( 712 , new String[] {"506" }); // Costa Rica
                put( 714 , new String[] {"507" }); // Panama (Republic of)
                put( 716 , new String[] {"51" , "0" }); // Peru
                put( 722 , new String[] {"54" , "0" }); // Argentine Republic
                put( 724 , new String[] {"55" , "0" }); // Brazil (Federative Republic of)
                put( 730 , new String[] {"56" , "0" }); // Chile
                put( 732 , new String[] {"57" , "09" }); // Colombia (Republic of)
                put( 734 , new String[] {"58" , "0" }); // Venezuela (Bolivarian Republic of)
                put( 736 , new String[] {"591" , "0" }); // Bolivia (Republic of)
                put( 738 , new String[] {"592" }); // Guyana
                put( 742 , new String[] {"594" , "0" }); // French Guiana (French Department of)
                put( 744 , new String[] {"595" , "0" }); // Paraguay (Republic of)
                put( 746 , new String[] {"597" , "0" }); // Suriname (Republic of)
                put( 748 , new String[] {"598" , "0" }); // Uruguay (Eastern Republic of)
                put( 750 , new String[] {"500" }); // Falkland Islands (Malvinas)
            }
        };
    };

    // @formatter:on

    /**
     * Constructor
     * 
     * @param context
     */
    private ContactUtil(Context context) {
        mCtx = context;
    }

    /**
     * Gets a singleton instance of ContactUtil.
     * 
     * @param context the context.
     * @return the singleton instance.
     */
    public static ContactUtil getInstance(Context context) {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (ContactUtil.class) {
            if (sInstance == null) {
                if (context == null) {
                    throw new IllegalArgumentException("Context is null");
                }
                sInstance = new ContactUtil(context);
            }
        }
        return sInstance;
    }

    /**
     * Removes blank and minus characters from contact
     * 
     * @param contact the phone number
     * @return phone string stripped of separators.
     */
    private String stripSeparators(String contact) {
        contact = contact.replaceAll("[ -]", "");
        Matcher matcher = PATTERN_CONTACT.matcher(contact);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    /**
     * Checks the validity of a contact number.
     * 
     * @param contact the contact number.
     * @return Returns true if the given contactId have the syntax of valid RCS contactId. If the
     *         string is too short (1 digit at least), too long (more than 15 digits) or contains
     *         illegal characters (valid characters are digits, space, ‘-‘, leading ‘+’) then it
     *         returns false.
     * @throws RcsPermissionDeniedException if the mobile country code failed to be read and is
     *             required to validate the contact.
     */
    public boolean isValidContact(String contact) throws RcsPermissionDeniedException {
        if (TextUtils.isEmpty(contact)) {
            return false;
        }
        String strippedContact = stripSeparators(contact);
        if (TextUtils.isEmpty(strippedContact)) {
            return false;
        }
        if (strippedContact.startsWith(COUNTRY_CODE_PREFIX)) {
            return true;
        }
        if (strippedContact.startsWith(MSISDN_PREFIX_INTERNATIONAL)) {
            return true;
        }
        /*
         * At this point, the contact is using a local area formatting so the mobile country code is
         * required to validate its format.
         */
        if (mCountryCode == null) {
            getCountryAndAreaCodes();
        }
        /* At this point, the mobile country and area codes are resolved */
        if (TextUtils.isEmpty(mCountryAreaCode)) {
            return true;
        }
        if (strippedContact.startsWith(mCountryAreaCode)) {
            return true;
        }
        return false;
    }

    /**
     * Formats the given contact to uniquely represent a RCS contact phone number.
     * 
     * @param contact the contact phone number to format.
     * @return the ContactId.
     * @throws RcsPermissionDeniedException if the mobile country code failed to be read and is
     *             required to format the contact.
     */
    public ContactId formatContact(String contact) throws RcsPermissionDeniedException {
        if (TextUtils.isEmpty(contact)) {
            throw new IllegalArgumentException("Input parameter is null or empty!");
        }
        String strippedContact = stripSeparators(contact);
        if (TextUtils.isEmpty(strippedContact)) {
            throw new IllegalArgumentException(new StringBuilder("Contact '").append(contact)
                    .append("' has invalid characters or is too long!").toString());
        }
        /* Is Country Code provided ? */
        if (strippedContact.startsWith(COUNTRY_CODE_PREFIX)) {
            return new ContactId(strippedContact);
        }
        /* International numbering with prefix ? */
        if (strippedContact.startsWith(MSISDN_PREFIX_INTERNATIONAL)) {
            return new ContactId(new StringBuilder(COUNTRY_CODE_PREFIX).append(strippedContact,
                    MSISDN_PREFIX_INTERNATIONAL.length(), strippedContact.length()).toString());
        }
        /*
         * The contact is using a local area formatting so the mobile country code is required to
         * validate its format.
         */
        if (mCountryCode == null) {
            getCountryAndAreaCodes();
        }
        /* Local numbering ? */
        if (TextUtils.isEmpty(mCountryAreaCode)) {
            /* No Country Area Code, add Country code to local number */
            return new ContactId(mCountryCode.concat(strippedContact));
        }
        // Local number must start with Country Area Code
        if (strippedContact.startsWith(mCountryAreaCode)) {
            /* Remove Country Area Code and add Country Code */
            return new ContactId(new StringBuilder(mCountryCode).append(strippedContact,
                    mCountryAreaCode.length(), strippedContact.length()).toString());
        }
        throw new IllegalArgumentException(new StringBuilder("Local phone number '")
                .append(strippedContact).append("' should be prefixed with Country Area Code (")
                .append(mCountryAreaCode).append(")").toString());
    }

    private void getCountryAndAreaCodes() throws RcsPermissionDeniedException {
        synchronized (ContactUtil.class) {
            if (mCountryCode != null) {
                return;
            }
            Configuration config = mCtx.getResources().getConfiguration();
            /* Get the country code information associated to the mobile country code */
            String[] countryCodeInfo = sCountryCodes.get(config.mcc);
            if (countryCodeInfo == null) {
                throw new RcsPermissionDeniedException(new StringBuilder(
                        "Failed to get mobile country code (").append(config.mcc).append(")!")
                        .toString());
            }
            /* Get the country code from map */
            String ccWithoutHeader = countryCodeInfo[COUNTRY_CODE_IDX];
            mCountryCode = COUNTRY_CODE_PREFIX.concat(ccWithoutHeader);
            mCountryAreaCode = null;
            if (countryCodeInfo.length == 2) {
                /* Get the country area code from map */
                mCountryAreaCode = countryCodeInfo[COUNTRY_AREA_CODE_IDX];
            }
        }
    }

    /**
     * Gets the user country code.
     * 
     * @return the user country code.
     * @throws RcsPermissionDeniedException if the mobile country code failed to be read.
     */
    public String getMyCountryCode() throws RcsPermissionDeniedException {
        if (mCountryCode == null) {
            getCountryAndAreaCodes();
        }
        return mCountryCode;
    }

    /**
     * Gets the user country area code.
     * 
     * @return the country area code or null if it does not exist.
     * @throws RcsPermissionDeniedException thrown if the mobile country code failed to be read.
     */
    public String getMyCountryAreaCode() throws RcsPermissionDeniedException {
        if (mCountryCode == null) {
            getCountryAndAreaCodes();
        }
        return mCountryAreaCode;
    }

    /**
     * Returns the vCard of a contact. The contact parameter contains the database URI of the
     * contact in the address book. The method returns a Uri to the visit card. The visit card
     * filename has the file extension .vcf and is generated from the address book vCard URI (see
     * Android SDK attribute ContactsContract.Contacts.CONTENT_VCARD_URI which returns the
     * referenced contact formatted as a vCard when opened through openAssetFileDescriptor(Uri,
     * String)).
     * 
     * @param contactUri Contact URI of the contact in the address book
     * @return Uri of vCard
     * @throws RcsGenericException
     */
    public Uri getVCard(Uri contactUri) throws RcsGenericException {
        Cursor cursor = null;
        try {
            cursor = mCtx.getContentResolver().query(contactUri, null, null, null, null);
            /* TODO: Handle cursor when null. */
            int displayNameColIdx = cursor
                    .getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            int lookupKeyColIdx = cursor
                    .getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY);
            if (!cursor.moveToFirst()) {
                return null;
            }
            String lookupKey = cursor.getString(lookupKeyColIdx);
            Uri vCardUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI,
                    lookupKey);
            AssetFileDescriptor fd = mCtx.getContentResolver().openAssetFileDescriptor(vCardUri,
                    "r");

            FileInputStream fis = fd.createInputStream();
            byte[] vCardData = new byte[(int) fd.getDeclaredLength()];
            fis.read(vCardData);

            String name = cursor.getString(displayNameColIdx);
            String fileName = new StringBuilder(Environment.getExternalStorageDirectory()
                    .toString()).append(File.separator).append(name).append(".vcf").toString();
            File vCardFile = new File(fileName);
            if (vCardFile.exists()) {
                vCardFile.delete();
            }

            FileOutputStream fos = new FileOutputStream(vCardFile, true);
            fos.write(vCardData);
            fos.close();

            return Uri.fromFile(vCardFile);

        } catch (IOException e) {
            throw new RcsGenericException(e);

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

}

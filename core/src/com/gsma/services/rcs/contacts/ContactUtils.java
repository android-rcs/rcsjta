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

package com.gsma.services.rcs.contacts;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.gsma.services.rcs.RcsContactFormatException;

/**
 * ContactUtils class for validation and unique formatting of phone numbers
 * 
 * @author YPLO6403
 * 
 */
public class ContactUtils {

	/**
	 * Singleton of ContactUtils
	 */
	private static volatile ContactUtils sInstance;

	/**
	 * The country code of the device (read from settings provider: is null before first provisioning)
	 */
	private final String mCountryCode;

	/**
	 * The country area code(read from settings provider)
	 */
	private final String mCountryAreaCode;

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
	 * A map between the ISO country code and an array of String containing first the County Code and secondly the Area Code.<br>
	 * Note 1 : the Area Code is optional (if it does not exist then it is null).<br>
	 * Note 2 : the Country Code is optional (if it does not exist then the string array is null).
	 */
	private static final Map<String, String[]> sCountryCodes = new HashMap<String, String[]>() {

		private static final long serialVersionUID = 1L;
		// @formatter:off
	{
		 put( "ad" , new String[] {"376"});
		 put( "ae" , new String[] {"971" ,"0"});
		 put( "af" , new String[] {"93" ,"0"});
		 put( "ag" , new String[] {"1-268" ,"1"});
		 put( "ai" , new String[] {"1-264" ,"1"});
		 put( "al" , new String[] {"355" ,"0"});
		 put( "am" , new String[] {"374" ,"0"});
		 put( "an" , new String[] {"599" ,"0"});
		 put( "ao" , new String[] {"244" ,"0"});
		 put( "aq" , null);
		 put( "ar" , new String[] {"54" ,"0"});
		 put( "as" , new String[] {"1-684" ,"1"});
		 put( "at" , new String[] {"43" ,"0"});
		 put( "au" , new String[] {"61" ,"0"});
		 put( "aw" , new String[] {"297"});
		 put( "az" , new String[] {"994" ,"0"});
		 put( "ba" , new String[] {"387" ,"0"});
		 put( "bb" , new String[] {"1-246" ,"1"});
		 put( "bd" , new String[] {"880" ,"0"});
		 put( "be" , new String[] {"32" ,"0"});
		 put( "bf" , new String[] {"226" });
		 put( "bg" , new String[] {"359" ,"0"});
		 put( "bh" , new String[] {"973" });
		 put( "bi" , new String[] {"257" });
		 put( "bj" , new String[] {"229" });
		 put( "bm" , new String[] {"1-441" ,"1"});
		 put( "bn" , new String[] {"673" });
		 put( "bo" , new String[] {"591" ,"0"});
		 put( "br" , new String[] {"55" ,"0"});
		 put( "bs" , new String[] {"1-242" ,"1"});
		 put( "bt" , new String[] {"975" });
		 put( "bv" , null);
		 put( "bw" , new String[] {"267" });
		 put( "by" , new String[] {"375" ,"8"});
		 put( "bz" , new String[] {"501"});
		 put( "ca" , new String[] {"1" ,"1"});
		 put( "cc" , null);
		 put( "cd" , new String[] {"243"});
		 put( "cf" , new String[] {"236"});
		 put( "cg" , new String[] {"242"});
		 put( "ch" , new String[] {"41" ,"0"});
		 put( "ci" , new String[] {"225"});
		 put( "ck" , new String[] {"682"});
		 put( "cl" , new String[] {"56" ,"0"});
		 put( "cm" , new String[] {"237"});
		 put( "cn" , new String[] {"86" ,"0"});
		 put( "co" , new String[] {"57" ,"09"});
		 put( "cr" , new String[] {"506"});
		 put( "cu" , new String[] {"53" ,"0"});
		 put( "cv" , new String[] {"238"});
		 put( "cx" , new String[] {"61"});
		 put( "cy" , new String[] {"357"});
		 put( "cz" , new String[] {"420" ,"0"});
		 put( "de" , new String[] {"49" ,"0"});
		 put( "dj" , new String[] {"253"});
		 put( "dk" , new String[] {"45"});
		 put( "dm" , new String[] {"1-767" ,"1"});
		 put( "do" , new String[] {"1-809" ,"1"});
		 put( "dz" , new String[] {"213" ,"0"});
		 put( "ec" , null);
		 put( "ee" , new String[] {"372"});
		 put( "eg" , new String[] {"20" ,"0"});
		 put( "eh" , new String[] {"212"});
		 put( "er" , new String[] {"291" ,"0"});
		 put( "es" , new String[] {"34"});
		 put( "et" , new String[] {"251" ,"0"});
		 put( "fi" , new String[] {"358" ,"0"});
		 put( "fj" , new String[] {"679"});
		 put( "fk" , new String[] {"500"});
		 put( "fm" , new String[] {"691" ,"1"});
		 put( "fo" , new String[] {"298"});
		 put( "fr" , new String[] {"33" ,"0"});
		 put( "ga" , new String[] {"241"});
		 put( "gb" , new String[] {"44" ,"0"});
		 put( "gd" , new String[] {"1-473" ,"1"});
		 put( "ge" , new String[] {"955" ,"8"});
		 put( "gf" , new String[] {"594" ,"0"});
		 put( "gh" , new String[] {"233" ,"0"});
		 put( "gi" , new String[] {"350"});
		 put( "gl" , new String[] {"299"});
		 put( "gm" , new String[] {"220"});
		 put( "gn" , new String[] {"224"});
		 put( "gp" , new String[] {"590" ,"0"});
		 put( "gq" , new String[] {"240"});
		 put( "gr" , new String[] {"30"});
		 put( "gs" , null);
		 put( "gt" , new String[] {"502"});
		 put( "gu" , new String[] {"1-671" ,"1"});
		 put( "gw" , new String[] {"245"});
		 put( "gy" , new String[] {"592"});
		 put( "hk" , new String[] {"852"});
		 put( "hm" , null);
		 put( "hn" , new String[] {"504"});
		 put( "hr" , new String[] {"385" ,"0"});
		 put( "ht" , new String[] {"509"});
		 put( "hu" , new String[] {"36" ,"06"});
		 put( "id" , new String[] {"62" ,"0"});
		 put( "ie" , new String[] {"353" ,"0"});
		 put( "il" , new String[] {"972" ,"0"});
		 put( "in" , new String[] {"91" ,"0"});
		 put( "io" , new String[] {"246"});
		 put( "iq" , new String[] {"964"});
		 put( "ir" , new String[] {"98" ,"0"});
		 put( "is" , new String[] {"354"});
		 put( "it" , new String[] {"39"});
		 put( "jm" , new String[] {"1-876" ,"1"});
		 put( "jo" , new String[] {"962" ,"0"});
		 put( "jp" , new String[] {"81" ,"0"});
		 put( "ke" , new String[] {"254" ,"0"});
		 put( "kg" , new String[] {"996" ,"0"});
		 put( "kh" , new String[] {"855" ,"0"});
		 put( "ki" , new String[] {"686"});
		 put( "km" , new String[] {"269"});
		 put( "kn" , new String[] {"1-869" ,"1"});
		 put( "kp" , new String[] {"850"});
		 put( "kr" , new String[] {"82" ,"0"});
		 put( "kw" , new String[] {"965"});
		 put( "ky" , new String[] {"1-345" ,"1"});
		 put( "kz" , new String[] {"7-7" ,"8"});
		 put( "la" , new String[] {"856"});
		 put( "lb" , new String[] {"961" ,"0"});
		 put( "lc" , new String[] {"1-758" ,"1"});
		 put( "li" , new String[] {"423"});
		 put( "lk" , new String[] {"94" ,"0"});
		 put( "lr" , new String[] {"231"});
		 put( "ls" , new String[] {"266"});
		 put( "lt" , new String[] {"370" ,"8"});
		 put( "lu" , new String[] {"352"});
		 put( "lv" , new String[] {"371"});
		 put( "ly" , new String[] {"281" ,"0"});
		 put( "ma" , new String[] {"212" ,"0"});
		 put( "mc" , new String[] {"377"});
		 put( "md" , new String[] {"373" ,"0"});
		 put( "me" , new String[] {"382" ,"0"});
		 put( "mg" , new String[] {"261"});
		 put( "mh" , new String[] {"692" ,"1"});
		 put( "mk" , new String[] {"389" ,"0"});
		 put( "ml" , new String[] {"223"});
		 put( "mm" , new String[] {"95"});
		 put( "mn" , new String[] {"976" ,"0"});
		 put( "mo" , new String[] {"853"});
		 put( "mp" , new String[] {"1-670" ,"1"});
		 put( "mq" , new String[] {"596" ,"0"});
		 put( "mr" , new String[] {"222"});
		 put( "ms" , new String[] {"1-664" ,"1"});
		 put( "mt" , new String[] {"356"});
		 put( "mu" , new String[] {"230"});
		 put( "mv" , new String[] {"960"});
		 put( "mw" , new String[] {"265"});
		 put( "mx" , new String[] {"52" ,"01"});
		 put( "my" , new String[] {"60" ,"0"});
		 put( "mz" , new String[] {"258"});
		 put( "na" , new String[] {"264" ,"0"});
		 put( "nc" , new String[] {"687"});
		 put( "ne" , new String[] {"227"});
		 put( "nf" , new String[] {"6723"});
		 put( "ng" , new String[] {"234" ,"0"});
		 put( "ni" , new String[] {"505"});
		 put( "nl" , new String[] {"31" ,"0"});
		 put( "no" , new String[] {"47"});
		 put( "np" , new String[] {"977" ,"0"});
		 put( "nr" , new String[] {"674"});
		 put( "nu" , new String[] {"683"});
		 put( "nz" , new String[] {"64" ,"0"});
		 put( "om" , new String[] {"968"});
		 put( "pa" , new String[] {"507"});
		 put( "pe" , new String[] {"51" ,"0"});
		 put( "pf" , new String[] {"689"});
		 put( "pg" , new String[] {"675"});
		 put( "ph" , new String[] {"63" ,"0"});
		 put( "pk" , new String[] {"92" ,"0"});
		 put( "pl" , new String[] {"48"});
		 put( "pm" , new String[] {"508"});
		 put( "pn" , new String[] {"870"});
		 put( "pr" , new String[] {"1-787" ,"1"});
		 put( "ps" , null);
		 put( "pt" , new String[] {"351"});
		 put( "pw" , new String[] {"680"});
		 put( "py" , new String[] {"595" ,"0"});
		 put( "qa" , new String[] {"974"});
		 put( "re" , new String[] {"262" ,"0"});
		 put( "ro" , new String[] {"40" ,"0"});
		 put( "rs" , new String[] {"381" ,"0"});
		 put( "ru" , new String[] {"7" ,"8"});
		 put( "rw" , new String[] {"250"});
		 put( "sa" , new String[] {"966" ,"0"});
		 put( "sb" , new String[] {"677"});
		 put( "sc" , new String[] {"248"});
		 put( "sd" , new String[] {"249" ,"0"});
		 put( "se" , new String[] {"46" ,"0"});
		 put( "sg" , new String[] {"65"});
		 put( "sh" , new String[] {"290"});
		 put( "si" , new String[] {"386"});
		 put( "sj" , null);
		 put( "sk" , new String[] {"421" ,"0"});
		 put( "sl" , new String[] {"232" ,"0"});
		 put( "sm" , new String[] {"378"});
		 put( "sn" , new String[] {"221"});
		 put( "so" , new String[] {"252"});
		 put( "sr" , new String[] {"597" ,"0"});
		 put( "st" , new String[] {"239"});
		 put( "sv" , new String[] {"503"});
		 put( "sy" , new String[] {"963" ,"0"});
		 put( "sz" , new String[] {"268"});
		 put( "tc" , new String[] {"1-649" ,"1"});
		 put( "td" , new String[] {"235"});
		 put( "tf" , null);
		 put( "tg" , new String[] {"228"});
		 put( "th" , new String[] {"66" ,"0"});
		 put( "tj" , new String[] {"992" ,"8"});
		 put( "tk" , new String[] {"690"});
		 put( "tl" , new String[] {"670"});
		 put( "tm" , new String[] {"993" ,"8"});
		 put( "tn" , new String[] {"216"});
		 put( "to" , new String[] {"676"});
		 put( "tr" , new String[] {"90" ,"0"});
		 put( "tt" , new String[] {"1-868" ,"1"});
		 put( "tv" , new String[] {"688"});
		 put( "tw" , new String[] {"886" ,"0"});
		 put( "tz" , new String[] {"255" ,"0"});
		 put( "ua" , new String[] {"380" ,"0"});
		 put( "ug" , new String[] {"256" ,"0"});
		 put( "um" , new String[] {"1"});
		 put( "us" , new String[] {"1" ,"1"});
		 put( "uy" , new String[] {"598" ,"0"});
		 put( "uz" , new String[] {"998" ,"8"});
		 put( "va" , new String[] {"379"});
		 put( "vc" , new String[] {"1-784" ,"1"});
		 put( "ve" , new String[] {"58" ,"0"});
		 put( "vg" , new String[] {"1-284" ,"1"});
		 put( "vi" , new String[] {"1-340" ,"1"});
		 put( "vn" , new String[] {"84" ,"0"});
		 put( "vu" , new String[] {"678"});
		 put( "wf" , new String[] {"681"});
		 put( "ws" , new String[] {"685"});
		 put( "ye" , new String[] {"967" ,"0"});
		 put( "yt" , new String[] {"262"});
		 put( "za" , new String[] {"27" ,"0"});
		 put( "zm" , new String[] {"260" ,"0"});
		 put( "zw" , new String[] {"263" ,"0"});
	}};
	// @formatter:on

	/**
	 * Empty constructor : prevent caller from creating multiple instances
	 */
	private ContactUtils() {
		mCountryCode = null;
		mCountryAreaCode = null;
	}

	/**
	 * Constructor
	 * @param context
	 * @param countryCode
	 */
	private ContactUtils(Context context) {
		// Get ISO country code from telephony manager
		TelephonyManager mgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		String countryCodeIso = mgr.getSimCountryIso();
		if (countryCodeIso == null) {
			throw new IllegalStateException("Instantation failure: cannot read SIM ISO country code");
			
		}
		// Get the country code information associated to the ISO country code
		String[] countryCodeInfo = sCountryCodes.get(countryCodeIso);
		if (countryCodeInfo == null) {
			throw new IllegalStateException("Instantation failure: no cc for SIM ISO country code " + countryCodeIso);
			
		}
		// Get the country code from map
		String ccWithoutHeader = countryCodeInfo[COUNTRY_CODE_IDX];
		mCountryCode = COUNTRY_CODE_PREFIX.concat(ccWithoutHeader);
		if (countryCodeInfo.length == 2) {
			// Get the country area code from map
			mCountryAreaCode = countryCodeInfo[COUNTRY_AREA_CODE_IDX];
		} else {
			mCountryAreaCode = null;
		}
	}

	/**
	 * Get an instance of ContactUtils.
	 * 
	 * @param context
	 *            the context
	 * @return the singleton instance. May be null if country code cannot be read from provider.
	 */
	public static ContactUtils getInstance(Context context) {
		if (sInstance != null) {
			return sInstance;
			
		}
		synchronized (ContactUtils.class) {
			if (sInstance == null) {
				if (context == null) {
					throw new IllegalArgumentException("Context is null");
					
				}
				sInstance = new ContactUtils(context);
			}
		}
		return sInstance;
	}

	/**
	 * Remove blank and minus characters from contact
	 * 
	 * @param contact
	 *            the phone number
	 * @return phone string stripped of separators.
	 */
	private String stripSeparators(String contact) {
		if (TextUtils.isEmpty(contact)) {
			return null;
			
		}
		contact = contact.replaceAll("[ -]", "");
		Matcher matcher = PATTERN_CONTACT.matcher(contact);
		if (matcher.find()) {
			return matcher.group();
			
		}
		return null;
	}

	/**
	 * Verify the validity of a contact number.
	 * 
	 * @param contact
	 *            the contact number
	 * @return Returns true if the given ContactId have the syntax of valid RCS ContactId.
	 */
	public boolean isValidContact(String contact) {
		return (!TextUtils.isEmpty(stripSeparators(contact)));
	}

	/**
	 * Formats the given contact to uniquely represent a RCS contact phone number.
	 * <p>
	 * May throw a RcsContactFormatException exception if the string contact parameter is not enabled to produce a valid ContactId.
	 * 
	 * @param contact
	 *            the contact phone number
	 * @return the ContactId
	 */
	public ContactId formatContact(String contact) {
		contact = stripSeparators(contact);
		if (!TextUtils.isEmpty(contact)) {
			// Is Country Code provided ?
			if (!contact.startsWith(COUNTRY_CODE_PREFIX)) {
				// CC not provided, does it exists in provider ?
				if (mCountryCode == null) {
					throw new RcsContactFormatException("Country code is unknown");
				}
				// International numbering with prefix ?
				if (contact.startsWith(MSISDN_PREFIX_INTERNATIONAL)) {
					contact = new StringBuilder(COUNTRY_CODE_PREFIX).append(contact, MSISDN_PREFIX_INTERNATIONAL.length(), contact.length())
							.toString();
				} else {
					// Local numbering ?
					if (!TextUtils.isEmpty(mCountryAreaCode)) {
						// Local number must start with Country Area Code
						if (contact.startsWith(mCountryAreaCode)) {
							// Remove Country Area Code and add Country Code
							contact = new StringBuilder(mCountryCode).append(contact, mCountryAreaCode.length(), contact.length())
									.toString();
						} else {
							throw new RcsContactFormatException("Local phone number should be prefixed with Country Area Code");
						}
					} else {
						// No Country Area Code, add Country code to local number
						contact = new StringBuilder(mCountryCode).append(contact).toString();
					}
				}
			}
			return new ContactId(contact);
			
		}
		throw new RcsContactFormatException("Input parameter is null or empty");
	}
	
	/**
	 * Gets the user country code.
	 * 
	 * @return the country code
	 */
	public String getMyCountryCode() {
		return mCountryCode;
	}

	/**
	 * Gets the user country area code.
	 * 
	 * @return the country area code or null if it does not exist
	 */
	public String getMyCountryAreaCode() {
		return mCountryAreaCode;
	}

}

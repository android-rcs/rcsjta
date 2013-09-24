/*******************************************************************************
 * Software Name : RCS
 *
 * Copyright © 2010 France Telecom S.A.
 ******************************************************************************/

package com.orangelabs.rcs.eab;

import android.content.Intent;

/**
 * Contains helper classes used to create or manage {@link android.content.Intent Intents}
 * that involve contacts.
 */
public final class Intents {
    /**
     * This is the intent that is fired when a search suggestion is clicked on.
     */
    public static final String SEARCH_SUGGESTION_CLICKED =
            "android.provider.Contacts.SEARCH_SUGGESTION_CLICKED";

    /**
     * This is the intent that is fired when a search suggestion for dialing a number
     * is clicked on.
     */
    public static final String SEARCH_SUGGESTION_DIAL_NUMBER_CLICKED =
            "android.provider.Contacts.SEARCH_SUGGESTION_DIAL_NUMBER_CLICKED";

    /**
     * This is the intent that is fired when a search suggestion for creating a contact
     * is clicked on.
     */
    public static final String SEARCH_SUGGESTION_CREATE_CONTACT_CLICKED =
            "android.provider.Contacts.SEARCH_SUGGESTION_CREATE_CONTACT_CLICKED";

    /**
     * Starts an Activity that lets the user pick a contact to attach an image to.
     * After picking the contact it launches the image cropper in face detection mode.
     */
    public static final String ATTACH_IMAGE =
            "com.android.contacts.action.ATTACH_IMAGE";

    /**
     * Starts the contactListActivity to pick a RCS contact after a file has been selected
     * in the gallery for File Transfer.
     * fileUri = getExtras().getParcelable(Intent.EXTRA_STREAM);
     */
    public static final String PICK_RCSCONTACT_FOR_FILETRANSFER =
    		"com.orangelabs.rcs.action.PICK_RCSCONTACT_FOR_FILETRANSFER";
    
    /**
     * Immediately starts a file transfer with the String contact 
     * and the Stream file as String (MediaStore.Images.ImageColumns.DATA) parameters.
     */
    public static final String START_FILETRANSFER =
    		"com.orangelabs.rcs.action.START_FILETRANSFER";
    
    /**
     * Takes as input a data URI with a mailto: or tel: scheme. If a single
     * contact exists with the given data it will be shown. If no contact
     * exists, a dialog will ask the user if they want to create a new
     * contact with the provided details filled in. If multiple contacts
     * share the data the user will be prompted to pick which contact they
     * want to view.
     * <p>
     * For <code>mailto:</code> URIs, the scheme specific portion must be a
     * raw email address, such as one built using
     * {@link Uri#fromParts(String, String, String)}.
     * <p>
     * For <code>tel:</code> URIs, the scheme specific portion is compared
     * to existing numbers using the standard caller ID lookup algorithm.
     * The number must be properly encoded, for example using
     * {@link Uri#fromParts(String, String, String)}.
     * <p>
     * Any extras from the {@link Insert} class will be passed along to the
     * create activity if there are no contacts to show.
     * <p>
     * Passing true for the {@link #EXTRA_FORCE_CREATE} extra will skip
     * prompting the user when the contact doesn't exist.
     */
    public static final String SHOW_OR_CREATE_CONTACT =
            "com.android.contacts.action.SHOW_OR_CREATE_CONTACT";

    /**
     * Used with {@link #SHOW_OR_CREATE_CONTACT} to force creating a new
     * contact if no matching contact found. Otherwise, default behavior is
     * to prompt user with dialog before creating.
     * <p>
     * Type: BOOLEAN
     */
    public static final String EXTRA_FORCE_CREATE =
            "com.android.contacts.action.FORCE_CREATE";

    /**
     * Used with {@link #SHOW_OR_CREATE_CONTACT} to specify an exact
     * description to be shown when prompting user about creating a new
     * contact.
     * <p>
     * Type: STRING
     */
    public static final String EXTRA_CREATE_DESCRIPTION =
        "com.android.contacts.action.CREATE_DESCRIPTION";

    /**
     * Optional extra used with {@link #SHOW_OR_CREATE_CONTACT} to specify a
     * dialog location using screen coordinates. When not specified, the
     * dialog will be centered.
     *
     * @hide
     */
    @Deprecated
    public static final String EXTRA_TARGET_RECT = "target_rect";

    /**
     * Optional extra used with {@link #SHOW_OR_CREATE_CONTACT} to specify a
     * desired dialog style, usually a variation on size. One of
     * {@link #MODE_SMALL}, {@link #MODE_MEDIUM}, or {@link #MODE_LARGE}.
     *
     * @hide
     */
    @Deprecated
    public static final String EXTRA_MODE = "mode";

    /**
     * Value for {@link #EXTRA_MODE} to show a small-sized dialog.
     *
     * @hide
     */
    @Deprecated
    public static final int MODE_SMALL = 1;

    /**
     * Value for {@link #EXTRA_MODE} to show a medium-sized dialog.
     *
     * @hide
     */
    @Deprecated
    public static final int MODE_MEDIUM = 2;

    /**
     * Value for {@link #EXTRA_MODE} to show a large-sized dialog.
     *
     * @hide
     */
    @Deprecated
    public static final int MODE_LARGE = 3;

    /**
     * Optional extra used with {@link #SHOW_OR_CREATE_CONTACT} to indicate
     * a list of specific MIME-types to exclude and not display. Stored as a
     * {@link String} array.
     *
     * @hide
     */
    @Deprecated
    public static final String EXTRA_EXCLUDE_MIMES = "exclude_mimes";

    /**
     * Intents related to the Contacts app UI.
     *
     * @hide
     */
    public static final class UI {
        /**
         * The action for the default contacts list tab.
         */
        public static final String LIST_DEFAULT =
                "com.android.contacts.action.LIST_DEFAULT";

        /**
         * The action for the contacts list tab.
         */
        public static final String LIST_GROUP_ACTION =
                "com.android.contacts.action.LIST_GROUP";

        /**
         * When in LIST_GROUP_ACTION mode, this is the group to display.
         */
        public static final String GROUP_NAME_EXTRA_KEY = "com.android.contacts.extra.GROUP";

        /**
         * The action for the all contacts list tab.
         */
        public static final String LIST_ALL_CONTACTS_ACTION =
                "com.android.contacts.action.LIST_ALL_CONTACTS";

        /**
         * The action for the contacts with phone numbers list tab.
         */
        public static final String LIST_CONTACTS_WITH_PHONES_ACTION =
                "com.android.contacts.action.LIST_CONTACTS_WITH_PHONES";

        /**
         * The action for the starred contacts list tab.
         */
        public static final String LIST_STARRED_ACTION =
                "com.android.contacts.action.LIST_STARRED";

        /**
         * The action for the frequent contacts list tab.
         */
        public static final String LIST_FREQUENT_ACTION =
                "com.android.contacts.action.LIST_FREQUENT";

        /**
         * The action for the "strequent" contacts list tab. It first lists the starred
         * contacts in alphabetical order and then the frequent contacts in descending
         * order of the number of times they have been contacted.
         */
        public static final String LIST_STREQUENT_ACTION =
                "com.android.contacts.action.LIST_STREQUENT";

        /**
         * A key for to be used as an intent extra to set the activity
         * title to a custom String value.
         */
        public static final String TITLE_EXTRA_KEY =
                "com.android.contacts.extra.TITLE_EXTRA";

        /**
         * Activity Action: Display a filtered list of contacts
         * <p>
         * Input: Extra field {@link #FILTER_TEXT_EXTRA_KEY} is the text to use for
         * filtering
         * <p>
         * Output: Nothing.
         */
        public static final String FILTER_CONTACTS_ACTION =
                "com.android.contacts.action.FILTER_CONTACTS";

        /**
         * Used as an int extra field in {@link #FILTER_CONTACTS_ACTION}
         * intents to supply the text on which to filter.
         */
        public static final String FILTER_TEXT_EXTRA_KEY =
                "com.android.contacts.extra.FILTER_TEXT";
    }

    /**
     * Convenience class that contains string constants used
     * to create contact {@link android.content.Intent Intents}.
     */
    public static final class Insert {
        /** The action code to use when adding a contact */
        public static final String ACTION = Intent.ACTION_INSERT;

        /**
         * If present, forces a bypass of quick insert mode.
         */
        public static final String FULL_MODE = "full_mode";

        /**
         * The extra field for the contact name.
         * <P>Type: String</P>
         */
        public static final String NAME = "name";

        // TODO add structured name values here.

        /**
         * The extra field for the contact phonetic name.
         * <P>Type: String</P>
         */
        public static final String PHONETIC_NAME = "phonetic_name";

        /**
         * The extra field for the contact company.
         * <P>Type: String</P>
         */
        public static final String COMPANY = "company";

        /**
         * The extra field for the contact job title.
         * <P>Type: String</P>
         */
        public static final String JOB_TITLE = "job_title";

        /**
         * The extra field for the contact notes.
         * <P>Type: String</P>
         */
        public static final String NOTES = "notes";

        /**
         * The extra field for the contact phone number.
         * <P>Type: String</P>
         */
        public static final String PHONE = "phone";

        /**
         * The extra field for the contact phone number type.
         * <P>Type: Either an integer value from
         * {@link CommonDataKinds.Phone},
         *  or a string specifying a custom label.</P>
         */
        public static final String PHONE_TYPE = "phone_type";

        /**
         * The extra field for the phone isprimary flag.
         * <P>Type: boolean</P>
         */
        public static final String PHONE_ISPRIMARY = "phone_isprimary";

        /**
         * The extra field for an optional second contact phone number.
         * <P>Type: String</P>
         */
        public static final String SECONDARY_PHONE = "secondary_phone";

        /**
         * The extra field for an optional second contact phone number type.
         * <P>Type: Either an integer value from
         * {@link CommonDataKinds.Phone},
         *  or a string specifying a custom label.</P>
         */
        public static final String SECONDARY_PHONE_TYPE = "secondary_phone_type";

        /**
         * The extra field for an optional third contact phone number.
         * <P>Type: String</P>
         */
        public static final String TERTIARY_PHONE = "tertiary_phone";

        /**
         * The extra field for an optional third contact phone number type.
         * <P>Type: Either an integer value from
         * {@link CommonDataKinds.Phone},
         *  or a string specifying a custom label.</P>
         */
        public static final String TERTIARY_PHONE_TYPE = "tertiary_phone_type";

        /**
         * The extra field for the contact email address.
         * <P>Type: String</P>
         */
        public static final String EMAIL = "email";

        /**
         * The extra field for the contact email type.
         * <P>Type: Either an integer value from
         * {@link CommonDataKinds.Email}
         *  or a string specifying a custom label.</P>
         */
        public static final String EMAIL_TYPE = "email_type";

        /**
         * The extra field for the email isprimary flag.
         * <P>Type: boolean</P>
         */
        public static final String EMAIL_ISPRIMARY = "email_isprimary";

        /**
         * The extra field for an optional second contact email address.
         * <P>Type: String</P>
         */
        public static final String SECONDARY_EMAIL = "secondary_email";

        /**
         * The extra field for an optional second contact email type.
         * <P>Type: Either an integer value from
         * {@link CommonDataKinds.Email}
         *  or a string specifying a custom label.</P>
         */
        public static final String SECONDARY_EMAIL_TYPE = "secondary_email_type";

        /**
         * The extra field for an optional third contact email address.
         * <P>Type: String</P>
         */
        public static final String TERTIARY_EMAIL = "tertiary_email";

        /**
         * The extra field for an optional third contact email type.
         * <P>Type: Either an integer value from
         * {@link CommonDataKinds.Email}
         *  or a string specifying a custom label.</P>
         */
        public static final String TERTIARY_EMAIL_TYPE = "tertiary_email_type";

        /**
         * The extra field for the contact postal address.
         * <P>Type: String</P>
         */
        public static final String POSTAL = "postal";

        /**
         * The extra field for the contact postal address type.
         * <P>Type: Either an integer value from
         * {@link CommonDataKinds.StructuredPostal}
         *  or a string specifying a custom label.</P>
         */
        public static final String POSTAL_TYPE = "postal_type";

        /**
         * The extra field for the postal isprimary flag.
         * <P>Type: boolean</P>
         */
        public static final String POSTAL_ISPRIMARY = "postal_isprimary";

        /**
         * The extra field for an IM handle.
         * <P>Type: String</P>
         */
        public static final String IM_HANDLE = "im_handle";

        /**
         * The extra field for the IM protocol
         */
        public static final String IM_PROTOCOL = "im_protocol";

        /**
         * The extra field for the IM isprimary flag.
         * <P>Type: boolean</P>
         */
        public static final String IM_ISPRIMARY = "im_isprimary";
    }
}


# Provisioning templates for development

Here below the following tempates supported by the stack for manual provisioning:

1. Albatros template
    * [template-ota_config-Albatros.xml](https://github.com/android-rcs/rcsjta/blob/master/data/provisioning_templates/albatros/template-ota_config-Albatros.xml) : basic RCS-e services based on Albatros RCS 5.1 standard.
    * Parameters to be updated in the template:
	ConRef: your APN
	Private_User_Identity, Public_user_identity_List
	Home_network_domain_name
	LBO_P-CSCF_Address
	Public_user_identity
	APPAUTH: Realm, 	UserName, UserPwd
	conf-fcty-uri
	exploder-uri


2. Blackbird template
    * [template_config-Blackbird.xml](https://github.com/android-rcs/rcsjta/blob/master/data/provisioning_templates/blackbird/template_config-Blackbird.xml) : basic RCS-e services based on Blackbird RCS 5.2 standard + Multiledia Media session.
    * Parameters to be updated in the template:
	ConRef: your APN
	Private_User_Identity
	Public_user_identity_List, Public_user_identity
	Home_network_domain_name
	LBO_P-CSCF_Address
	APPAUTH: Realm, 	UserName, UserPwd
	ftHTTPCSURI, ftHTTPCSUser, ftHTTPCSPwd
	conf-fcty-uri


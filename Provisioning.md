# Introduction #

There are two diffrenet ways to realize the provisioning of the RCS service:

1) Network provisioning with HTTPS: this is an automatic procedure via the network.

2) Local provisioning: this is a manual procedure where the user has to enter the provisoning parameter for a given IMS platform.

The provisioning mode (HTTPS or manual) is fixed into a stack parameter.

Currently the local provisioning is set by default, but in a commercial release this parameter should be set to HTTPS by default.

The provisioning parameters contains the IMS user profile (eg. IMPU, IMPI, IMS domain, .etc), service settings (eg. auto accept mode, .etc) and stack settings (eg. SIP transport, .etc).


# Change the provisioning mode #

To change the default provisioning mode, start the local provisioning application and from the second folder selects the auto config mode:

<img src='https://github.com/android-rcs/rcsjta/blob/master/docs/website/provisioning.pn'>

Then save the configuration and restart the stack.

# Network provisioning #

The network provisioning uses an HTTPS procedure specified in the RCS standard in order to get the provisioning parameters associated to the current MSISDN of the device.

This is automatic but needs the corresponding DM server in your platform.


# Local provisioning #

The local provisioning is mainly used during debugging and permits to change of IMS platform easily during testing.

The local provisioning may be edited manually via the local provisioning application: there are 4 folders with "Profile" settings, "Stack" settings, "Service" settings and "Logger" settings.

For any update of a setting, don't forget to save the configuration and then to restart the stack.

For the IMS settings of the folder "Profile": to facilitate the edition  it's possible to load predefined profile from a local XML file stored in the directory `/sdcard` of the device. So you can create as many XML file per IMS platform and then load the right one for your test without manual and tedious edition:

<img src='https://github.com/android-rcs/rcsjta/blob/master/docs/website/provisioning2.png'>

See template for [Albatros configuration](https://github.com/android-rcs/rcsjta/blob/master/data/provisioning_templates/albatros/template-ota_config-Albatros.xml).

See template for [Blackbird configuration](https://github.com/android-rcs/rcsjta/blob/master/data/provisioning_templates/blackbird/template_config-Blackbird.xml).

If you already have a valid SIM card, it is possible to generate automatically a provisioning template using the tool "RCSJTA provisioning template". This tool gets the configuration via HTTPs from the DM server and saves the file "provisioning_template.xml" under the SD card of your device. The new created template can be used later to provision the stack manually with any other MSISDN (provided it is a valid one for the current platform).

<img src='https://github.com/android-rcs/rcsjta/blob/master/docs/website/provisioning_template.png'>


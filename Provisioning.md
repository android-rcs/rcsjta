# Introduction #

There are two different ways to perform the provisioning of the RCS service:

1) Automatic: the provisioning is sent over the network via HTTPS (default mode).

2) Manual: the user enters the provisoning parameters for a given IMS platform.

The provisioning mode (automatic or manual) is saved into a stack parameter.

The provisioning parameters contains the IMS user profile (eg. IMPU, IMPI, IMS domain, .etc), the service settings (eg. auto accept mode, .etc), the stack settings (eg. SIP transport, .etc), the capabilities settings and the logger settings.

# Change the provisioning mode #

To change the default provisioning mode, selects the auto config mode in the second tab :

<img src='https://github.com/android-rcs/rcsjta/blob/master/docs/website/provisioning.png'>

Then save the configuration and restart the stack.

# Automatic provisioning #

The automatic provisioning uses an HTTPS procedure specified in the RCS standard in order to get the provisioning parameters associated to the current MSISDN of the device.
This is automatic but requires the corresponding DM server in your platform.

# Manual provisioning #

The manual provisioning is mainly used during debugging and permits to change of IMS platform easily during testing.
The local provisioning application has 5 tabs : "Profile", "Stack", "Service", "Capabilities" and "Logger" settings.

For any update of a setting, don't forget to save the configuration and then to restart the stack.

To facilitate the edition  it's possible to load predefined profile from a local XML file stored in the directory `/sdcard` of the device. So you can create as many XML file per IMS platform and then load the right one for your test without manual and tedious edition:

<img src='https://github.com/android-rcs/rcsjta/blob/master/docs/website/provisioning2.png'>

See template for [Albatros configuration](https://github.com/android-rcs/rcsjta/blob/master/data/provisioning_templates/albatros/template-ota_config-Albatros.xml).

See template for [Blackbird configuration](https://github.com/android-rcs/rcsjta/blob/master/data/provisioning_templates/blackbird/template_config-Blackbird.xml).

If you already have a valid SIM card, it is possible to generate automatically a provisioning template using the tool "RCSJTA provisioning template". This tool gets the configuration via HTTPs from the DM server and saves the file "provisioning_template.xml" under the SD card of your device. The new created template can be used later to provision the stack manually with any other MSISDN (provided it is a valid one for the current platform).

<img src='https://github.com/android-rcs/rcsjta/blob/master/docs/website/provisioning_template.png'>


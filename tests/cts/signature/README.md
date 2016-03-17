# The Compatibility Test Suite for RCS API

This application checks the compatibility of a given RCS API with the different existing releases.

Build instruction:

- create a 'libs' directory and copy in this new directory the jar file corresponding to the RCS API library to be tested.
- edit the `ToTest` java class to set the `VERSION` with the reference of the RCS API XML file
- run on the device
<code>../../gradlew :cts_signature:installDebug</code>



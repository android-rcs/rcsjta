# RCS core stack settings

This application controls the activation of the RCS core stack.<br>
It also displays the RCS core stack settings and enables user to modify them.

##Build instruction:

<code>../../gradlew :settings:build</code>

**Additional steps for Eclipse compatibility:**

<code>ant libs</code>

This will create the following jar files under the "libs" folder:
- api.jar
- api_cnx.jar

Set up the api connection library by importing the Android project '../libs/api_cnx/build/intermediates/bundles/debug/'
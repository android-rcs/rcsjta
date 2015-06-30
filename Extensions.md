# Introduction #

Since the begining of this open source project, we have introduced the concept of extensions which permit to create and to deploy new RCS/IMS s
ervices without updating the RCS stack. This concept is now part of the RCS 5.2 standardized by GSMA.

The Multimedia Session API permits to implement extensions and the Capability API permits to declare new extensions.

# Details #

In order to control the use of extensions, 2 new parameters have been added to the RCS stack:
- "Allow extensions": this parameter indicates if the extensions are authorized or not in the stack.
- "Control extensions": this parameter indicates if the stack should control or not that an application, using the Multimedia Session API or the capability extension, has the permission to use the extension.

For more info about the solution to control the permission see the "RCS Extensibility" specification (by GSMA).

These 2 parameters may be configured via provisioning and via the local provisioning tool (see folder "Stack"). By default the stack allows extensions without any control.

## Test ##

To do
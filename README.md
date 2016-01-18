# rcsjta
RCS-e stack for Android with GSMA API **RCS-e stack for Android with GSMA API**

<img src='https://github.com/android-rcs/rcsjta/blob/master/docs/website/twitter-bird-16x16.png'> <a href='http://twitter.com/androidrcsstack'>Follow @androidrcsstack</a><br>

The RCS-e stack is an open source implementation of the Rich Communication Suite standards for Google Android platform. This implementation is compliant to GSMA RCS-e Blackbird standards. Thanks to its client/server API, the stack may be easily integrated with existing native Android applications (e.g. address book, dialer) and permits to create new RCS applications (e.g. chat, widgets).

##About RCS, Rich Communication Suite:

The Rich Communication Suite Initiative is a GSM Association programme dedicated to deliver convergent rich communication services. RCS should be the first set of services using IMS architecture in the mobile field. "joyn" is the commercial name of RCS.

See also the RCS website at GSM Association, [http://www.gsma.com/rcs/](http://www.gsma.com/rcs/).

The RCS specifications (product, technical, API, Guidelines) are available at [http://www.gsma.com/network2020/rcs/specs-and-product-docs/](http://www.gsma.com/network2020/rcs/specs-and-product-docs/).

Note: the [supported standards](https://rawgit.com/android-rcs/rcsjta/master/docs/SUPPORTED-STANDARDS.txt).

##Licensing:
The RCS core stack is under [Apache 2 license](https://rawgit.com/android-rcs/rcsjta/master/core/LICENSE-2.0.txt) and uses the following open source libraries:

- the SIP stack comes from [NIST-SIP project] (http://jsip.java.net/'>http://jsip.java.net/), see [License](https://rawgit.com/android-rcs/rcsjta/master/core/LICENSE-NIST.txt).

- the DNS stack comes from [DNSJava project](http://www.dnsjava.org/), see [License](https://rawgit.com/android-rcs/rcsjta/master/core/LICENSE-DNS.txt).

- the cryptography API comes from Legion of the [Bouncy Castle Inc] (https://www.bouncycastle.org/), see [License](https://rawgit.com/android-rcs/rcsjta/master/core/LICENSE-BOUNCYCASTLE.txt).

##Project:

- see [Project management](https://rawgit.com/android-rcs/rcsjta/master/docs/RCSJTA_open_source.ppt).

- see [GIT branches](https://github.com/android-rcs/rcsjta/blob/wiki/Branches.md).

##RCS API definition:

- see [TAPI 1.5.1 specification](https://rawgit.com/android-rcs/rcsjta/master/docs/tapi/RCC.53_v3.0_1.5.1-r1.docx).

- see [TAPI 1.5.1 Javadoc] (https://rawgit.com/android-rcs/rcsjta.javadoc/javadoc1.5/index.html).

##SDK nightly builds:
- see latest SDK of [master](https://github.com/android-rcs/rcsjta.build/tree/master) branch.
- see latest SDK of [tapi_1.5](https://github.com/android-rcs/rcsjta.build/tree/tapi_1.5) branch.

##Stack overview:

<img src='https://github.com/android-rcs/rcsjta/blob/master/docs/website/overview.png'><br>

##More docs:

- see [Wiki](https://github.com/android-rcs/rcsjta/wiki).

##Standardisation:

TAPI 1.6
- to review [CR Audio Message](https://github.com/android-rcs/rcsjta/raw/master/docs/tapi/CR_1.6/RCSJTA_T-API1.6_CR001_AudioMessage-R2.doc).
- to review [CR Enrich Calling](https://github.com/android-rcs/rcsjta/raw/master/docs/tapi/CR_1.6/RCSJTA_TT_BB_CR001_EnrichCalling_R6.doc).
- to review [CR Capability](https://github.com/android-rcs/rcsjta/raw/master/docs/tapi/CR_1.6/RCSJTA_T-API1.6_CR003_Capability_Discovery-R1.doc).
- to review [CR Crane priority release](https://github.com/android-rcs/rcsjta/raw/master/docs/tapi/CR_1.6/RCSJTA_T-API1.6_CR002_ReferenceToCranePriorityRelease-R2.doc).




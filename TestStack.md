# Introduction #

We receive a lot of questions about how to test the RCS stack? which server do I need to test the RCS stack? which open source or free server exists?


# Details #

By default the RCS service needs an IMS platform and also an AS (Application Server) for IM services (1-1 chat, Group chat and File transfer), the other services (Capabilities, Video/Image/Geoloc sharing and MM session) doesn't need any AS IM.

## Which IMS platform to use? ##
If you don't have any Telco platform available for your tests, you can install an open source IMS platform:

  * using [kamailio](http://www.kamailio.org) as P/I/S-CSCF
  * using bind as DNS server
  * using [FHoSS](http://www.openimscore.org/docs/FHoSS/index.html) for HSS

You can find informations on how to setup such an IMS platform on the following pages:

  * https://loadmultiplier.com/node/76
  * http://www.kamailio.org/wiki/tutorials/ims/installation-howto



## Which IM Application Server to use? ##
We don't know any existing open or free server for IM.
Nevertheless it's possible to play 1-1 chat and File transfer service without any AS: in this case the originating device should be in "active mode" and the terminating device in "passive mode".
IMCProxy
========

Introduction
------------

IMCProxy is a simple application for bridging IMC networks through the Internet. Bridging is done through a centralized gateway (proxy server) that receives all IMC messages and forwards them to other connected networks.

On each IMC network, a proxy client announces remote IMC nodes and listens for messages for those nodes. When a received message is addressed to a remote node, the proxy client converts it into a web-enabled packet and sends it to the proxy server. In a similar fashion, when a remote IMC node sends a message to a known local IMC node, the proxy client sends it to the final destination.


Compiling
---------

* To compile you need Java 7 and ANT installed in your system.
* In the project's folder run *ant dist*. Two files (_client.jar_, _server.jar_) will be generated.

Running
-------

* To run you need Java 7 installed in your system
* To run the proxy client run *java -jar client.jar*. Your IMC network is now connected.

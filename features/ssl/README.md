## SSL

You can secure traffic between the driver and Cassandra with SSL. This
section describes the driver-side configuration; it assumes that you've
already
[configured SSL in Cassandra](http://docs.datastax.com/en/cassandra/2.0/cassandra/security/secureSSLClientToNode_t.html).

### Preparing the certificates

#### Client truststore

This allows the client to verify the identity of all the Cassandra nodes
it connects to.

As part of configuring Cassandra, you should already have
[prepared a certificate](http://docs.datastax.com/en/cassandra/2.0/cassandra/security/secureSSLCertificates_t.html)
for each node. Export the public part of that certificate from the
node's keystore:

```
keytool -export -alias cassandra -file cassandranode0.cer -keystore .keystore
```

Add the public certificates to the client trustore:

```
keytool -import -v -trustcacerts -alias <cassandra_node0> -file cassandranode0.cer -keystore client.truststore
keytool -import -v -trustcacerts -alias <cassandra_node1> -file cassandranode1.cer -keystore client.truststore
...
```

Distribute `client.truststore` to the client(s).

#### Client keystore

If you also intend to use client certificate authentication, generate
the public and private key pair for the client:

```
keytool -genkey -keyalg RSA -alias client -keystore client.keystore
```

Then extract the public part of the client certificate, and import it in
the truststore of each Cassandra node:

```
keytool -export -alias client -file client.cer -keystore client.keystore
keytool -import -v -trustcacerts -alias client -file client.cer -keystore server.truststore
```

### Driver configuration

The base class to configure SSL is [SSLOptions]. It's very generic, but
you don't necessarily need to deal with it directly: the default
instance, or the provided subclasses, might be enough for your needs.

#### JSSE, Property-based

`withSSL()` gives you a basic JSSE configuration:

```java
Cluster cluster = Cluster.builder()
  .addContactPoint("127.0.0.1")
  .withSSL()
  .build();
```

You can then use
[JSSE system properties](http://docs.oracle.com/javase/6/docs/technotes/guides/security/jsse/JSSERefGuide.html#Customization)
for specific details, like keystore locations and passwords:

```
-Djavax.net.ssl.keyStore=/path/to/client.keystore
-Djavax.net.ssl.keyStorePassword=password123
-Djavax.net.ssl.trustStore=/path/to/client.truststore
-Djavax.net.ssl.trustStorePassword=password123
```

#### JSSE, programmatic

If you need more control than what system properties allow, you can
configure SSL programmatically with [JdkSSLOptions]:

```java
SSLContext sslContext = ... // create and configure SSL context

JdkSSLOptions sslOptions = JdkSSLOptions.builder()
  .withSSLContext(context)
  .build();

Cluster cluster = Cluster.builder()
  .addContactPoint("127.0.0.1")
  .withSSL(sslOptions)
  .build();
```

Note that you can also extend the class and override `newSSLEngine()` if
you need specific configuration on the `SSLEngine` (for example hostname
verification).

#### Netty

[NettySSLOptions] allows you to use Netty's `SslContext` instead of
the JDK directly. The advantage is that Netty can use OpenSSL if
available, which provides better performance.

##### Converting your client certificates for OpenSSL

OpenSSL doesn't use keystores, so if you use client authentication and
generated your certificates with keytool, you need to convert them.

* use this command to extract the public certificate chain:

    ```
    keytool -export -keystore client.keystore -alias client -rfc -file client.crt
    ```
* follow
  [this tutorial](http://www.herongyang.com/crypto/Migrating_Keys_keytool_to_OpenSSL_3.html)
  to extract your client's private key from `client.keystore` to a text
  file `client.key` in PEM format.

##### Updating your dependencies

Netty-tcnative provides the native integration with OpenSSL. Follow
[these instructions](http://netty.io/wiki/forked-tomcat-native.html) to
add it to your dependencies.

##### Configuring the context

Use the following Java code to configure OpenSSL with your certificates:

```java
KeyStore ks = KeyStore.getInstance("JKS");
// make sure you close this stream properly (not shown here for brevity)
InputStream trustStore = new FileInputStream("client.truststore");
ks.load(trustStore, "password123".toCharArray());
TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
tmf.init(ks);

SslContextBuilder builder = SslContextBuilder
  .forClient()
  .sslProvider(SslProvider.OPENSSL)
  .trustManager(tmf);
  // only if you use client authentication
  .keyManager(new File("client.crt"), new File("client.key"));

SSLOptions sslOptions = new NettySSLOptions(builder.build());

Cluster cluster = Cluster.builder()
  .addContactPoint("127.0.0.1")
  .withSSL(sslOptions)
  .build();
```

[SSLOptions]: http://docs.datastax.com/en/drivers/java/3.0/com/datastax/driver/core/SSLOptions.html
[JdkSSLOptions]: http://docs.datastax.com/en/drivers/java/3.0/com/datastax/driver/core/JdkSSLOptions.html
[NettySSLOptions]: http://docs.datastax.com/en/drivers/java/3.0/com/datastax/driver/core/NettySSLOptions.html
[NettyOptions]: http://docs.datastax.com/en/drivers/java/3.0/com/datastax/driver/core/NettyOptions.html

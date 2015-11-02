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

#### Property-based

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

#### Programmatic

If you need more control than what system properties allow, you can
configure SSL programmatically by creating an [SSLOptions] instance:

```java
SSLContext sslContext = ... // create and configure SSL context
String[] cipherSuites = ...

SSLOptions sslOptions = new SSLOptions(sslContext, cipherSuites);

Cluster cluster = Cluster.builder()
  .addContactPoint("127.0.0.1")
  .withSSL(sslOptions)
  .build();
```

A known limitation of the current API is that you can't customize the
`SSLEngine`, which prevents advanced features like hostname
verification. This will be fixed in 3.0 (see
[JAVA-841](https://datastax-oss.atlassian.net/browse/JAVA-841)), in the
meantime see below for a workaround.


### Bypassing `SSLOptions`

For advanced use cases, it's possible to bypass `SSLOptions` entirely:
since the driver provides a hook into the Netty pipeline (by way of
[NettyOptions]), you can add the SSL handler yourself:

```java
import io.netty.handler.ssl.SslHandler;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

// Base implementation: each time a connection is established, create an SSL handler
// and add it to the pipeline:
public abstract class SslNettyOptions extends NettyOptions {
  @Override
  public void afterChannelInitialized(SocketChannel channel) throws Exception {
    channel.pipeline().addFirst("ssl", createSslHandler(channel));
  }

  protected abstract SslHandler createSslHandler(SocketChannel channel);
}

// Concrete implementation based on JSSE
public class MyNettyOptions extends SslNettyOptions {

  private final SSLContext sslContext = createContext();

  protected SslHandler createSslHandler(SocketChannel channel) {
    return new SslHandler(createEngine(sslContext, channel));
  }

  private static SSLContext createContext() {
    ... // create and configure context
  }

  private static SSLEngine createEngine(SSLContext sslContext, SocketChannel channel) {
    SSLEngine engine = sslContext.createSSLEngine();
    engine.setUseClientMode(true);
    ... // any extra configuration
    return engine;
  }
}

cluster = Cluster.builder()
  .addContactPoint("127.0.0.1")
  .withNettyOptions(new MyNettyOptions())
  .build();
```

This could be used for the following use cases:

* fine control over JSSE configuration, for example tuning the SSL
  engine for hostname verification;
* bypassing JSSE altogether in favor of another SSL implementation:
  recent Netty versions support
  [OpenSSL](http://netty.io/wiki/forked-tomcat-native.html)
  for improved performance.

[SSLOptions]: http://docs.datastax.com/en/drivers/java/2.0/com/datastax/driver/core/SSLOptions.html
[NettyOptions]: http://docs.datastax.com/en/drivers/java/2.0/com/datastax/driver/core/NettyOptions.html

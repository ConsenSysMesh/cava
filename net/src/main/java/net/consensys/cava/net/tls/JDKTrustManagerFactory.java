package net.consensys.cava.net.tls;

import java.nio.file.Path;
import java.nio.file.Paths;
import javax.net.ssl.TrustManagerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.net.JksOptions;

final class JDKTrustManagerFactory {
  private JDKTrustManagerFactory() {}

  public static TrustManagerFactory create(Vertx vertx) {
    JksOptions delegateTrustOptions = new JksOptions();

    if (System.getProperty("javax.net.ssl.trustStore") != null) {
      delegateTrustOptions.setPath(System.getProperty("javax.net.ssl.trustStore"));
      if (System.getProperty("javax.net.ssl.trustStorePassword") != null) {
        delegateTrustOptions.setPassword(System.getProperty("javax.net.ssl.trustStorePassword"));
      }
    } else {
      Path jsseCaCerts = Paths.get(System.getProperty("java.home"), "lib", "security", "jssecacerts");
      if (jsseCaCerts.toFile().exists()) {
        delegateTrustOptions.setPath(jsseCaCerts.toString());
      } else {
        Path cacerts = Paths.get(System.getProperty("java.home"), "lib", "security", "cacerts");
        delegateTrustOptions.setPath(cacerts.toString());
      }
      delegateTrustOptions.setPassword("changeit");
    }

    try {
      return delegateTrustOptions.getTrustManagerFactory(vertx);
    } catch (Exception e) {
      throw new TLSEnvironmentException("Error initializing the CA trust manager factory", e);
    }
  }
}

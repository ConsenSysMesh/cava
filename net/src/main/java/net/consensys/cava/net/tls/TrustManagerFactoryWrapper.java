package net.consensys.cava.net.tls;

import javax.net.ssl.TrustManagerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.net.TrustOptions;

final class TrustManagerFactoryWrapper implements TrustOptions {

  private final TrustManagerFactory trustManagerFactory;

  TrustManagerFactoryWrapper(TrustManagerFactory trustManagerFactory) {
    this.trustManagerFactory = trustManagerFactory;
  }

  @Override
  public TrustOptions clone() {
    return new TrustManagerFactoryWrapper(trustManagerFactory);
  }

  @Override
  public TrustManagerFactory getTrustManagerFactory(Vertx vertx) {
    return trustManagerFactory;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof TrustManagerFactoryWrapper)) {
      return false;
    }
    TrustManagerFactoryWrapper other = (TrustManagerFactoryWrapper) obj;
    return trustManagerFactory.equals(other.trustManagerFactory);
  }

  @Override
  public int hashCode() {
    return trustManagerFactory.hashCode();
  }
}

package net.consensys.cava.net.tls;

import javax.net.ssl.TrustManagerFactory;

import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.vertx.core.Vertx;
import io.vertx.core.net.TrustOptions;

final class InsecureTrustOptions implements TrustOptions {

  static InsecureTrustOptions INSTANCE = new InsecureTrustOptions();

  private InsecureTrustOptions() {}

  @Override
  public TrustOptions clone() {
    return this;
  }

  @Override
  public TrustManagerFactory getTrustManagerFactory(Vertx vertx) {
    return InsecureTrustManagerFactory.INSTANCE;
  }
}

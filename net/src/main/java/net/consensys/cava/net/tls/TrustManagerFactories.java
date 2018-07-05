/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.cava.net.tls;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import javax.annotation.Nullable;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Trust manager factories for fingerprinting clients and servers.
 */
public final class TrustManagerFactories {
  private TrustManagerFactories() {}

  /**
   * Accept all server certificates, recording certificate fingerprints for those that are not CA-signed.
   *
   * <p>
   * Excepting when a server presents a CA-signed certificate, the server host+port and the certificate fingerprint will
   * be written to {@code knownServersFile}.
   *
   * <p>
   * Important: this provides no security as it is vulnerable to man-in-the-middle attacks.
   *
   * @param knownServersFile The path to a file in which to record fingerprints by host.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory recordServerFingerprints(Path knownServersFile) {
    requireNonNull(knownServersFile);
    return recordServerFingerprints(knownServersFile, true);
  }

  /**
   * Accept all server certificates, recording certificate fingerprints.
   *
   * <p>
   * For all connections, the server host+port and the fingerprint of the presented certificate will be written to
   * {@code knownServersFile}.
   *
   * <p>
   * Important: this provides no security as it is vulnerable to man-in-the-middle attacks.
   *
   * @param knownServersFile The path to a file in which to record fingerprints by host.
   * @param skipCASigned If <tt>true</tt>, CA-signed certificates are not recorded.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory recordServerFingerprints(Path knownServersFile, boolean skipCASigned) {
    requireNonNull(knownServersFile);
    return wrap(ServerFingerprintTrustManager.record(knownServersFile), skipCASigned);
  }

  /**
   * Accept all server certificates, recording certificate fingerprints for those that are not CA-signed.
   *
   * <p>
   * Excepting when a server presents a CA-signed certificate, the server host+port and the certificate fingerprint will
   * be written to {@code knownServersFile}.
   *
   * <p>
   * Important: this provides no security as it is vulnerable to man-in-the-middle attacks.
   *
   * @param knownServersFile The path to a file in which to record fingerprints by host.
   * @param tmf A {@link TrustManagerFactory} for checking server certificates against a CA.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory recordServerFingerprints(Path knownServersFile, TrustManagerFactory tmf) {
    requireNonNull(knownServersFile);
    requireNonNull(tmf);
    return wrap(ServerFingerprintTrustManager.record(knownServersFile), tmf);
  }

  /**
   * Accept CA-signed certificates, and otherwise trust server certificates on first use.
   *
   * <p>
   * Except when a server presents a CA-signed certificate, on first connection to a server (identified by host+port)
   * the fingerprint of the presented certificate will be recorded in {@code knownServersFile}. On subsequent
   * connections, the presented certificate will be matched to the stored fingerprint to ensure it has not changed.
   *
   * @param knownServersFile The path to the file containing fingerprints by host.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory trustServerOnFirstUse(Path knownServersFile) {
    requireNonNull(knownServersFile);
    return trustServerOnFirstUse(knownServersFile, true);
  }

  /**
   * Trust server certificates on first use.
   *
   * <p>
   * On first connection to a server (identified by host+port) the fingerprint of the presented certificate will be
   * recorded in {@code knownServersFile}. On subsequent connections, the presented certificate will be matched to the
   * stored fingerprint to ensure it has not changed.
   *
   * @param knownServersFile The path to the file containing fingerprints by host.
   * @param acceptCASigned If <tt>true</tt>, CA-signed certificates will always be accepted (and the fingerprint will
   *        not be recorded).
   * @return A trust manager factory.
   */
  public static TrustManagerFactory trustServerOnFirstUse(Path knownServersFile, boolean acceptCASigned) {
    requireNonNull(knownServersFile);
    return wrap(ServerFingerprintTrustManager.tofu(knownServersFile), acceptCASigned);
  }

  /**
   * Accept CA-signed certificates, and otherwise trust server certificates on first use.
   *
   * <p>
   * Except when a server presents a CA-signed certificate, on first connection to a server (identified by host+port)
   * the fingerprint of the presented certificate will be recorded in {@code knownServersFile}. On subsequent
   * connections, the presented certificate will be matched to the stored fingerprint to ensure it has not changed.
   *
   * @param knownServersFile The path to the file containing fingerprints by host.
   * @param tmf A {@link TrustManagerFactory} for checking server certificates against a CA.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory trustServerOnFirstUse(Path knownServersFile, TrustManagerFactory tmf) {
    requireNonNull(knownServersFile);
    requireNonNull(tmf);
    return wrap(ServerFingerprintTrustManager.tofu(knownServersFile), tmf);
  }

  /**
   * Require servers to present known certificates, or CA-signed certificates.
   *
   * <p>
   * If a certificate is not CA-signed, then its fingerprint must be present in the {@code knownServersFile}, associated
   * with the server (identified by host+port).
   *
   * @param knownServersFile The path to the file containing fingerprints by host.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory whitelistServers(Path knownServersFile) {
    requireNonNull(knownServersFile);
    return whitelistServers(knownServersFile, true);
  }

  /**
   * Require servers to present known certificates.
   *
   * <p>
   * The fingerprint for a server certificate must be present in the {@code knownServersFile}, associated with the
   * server (identified by host+port).
   *
   * @param knownServersFile The path to the file containing fingerprints by host.
   * @param acceptCASigned If <tt>true</tt>, CA-signed certificates will always be accepted.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory whitelistServers(Path knownServersFile, boolean acceptCASigned) {
    requireNonNull(knownServersFile);
    return wrap(ServerFingerprintTrustManager.whitelist(knownServersFile), acceptCASigned);
  }

  /**
   * Require servers to present known certificates, or CA-signed certificates.
   *
   * <p>
   * If a certificate is not CA-signed, then its fingerprint must be present in the {@code knownServersFile}, associated
   * with the server (identified by host+port).
   *
   * @param knownServersFile The path to the file containing fingerprints by host.
   * @param tmf A {@link TrustManagerFactory} for checking server certificates against a CA.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory whitelistServers(Path knownServersFile, TrustManagerFactory tmf) {
    requireNonNull(knownServersFile);
    requireNonNull(tmf);
    return wrap(ServerFingerprintTrustManager.whitelist(knownServersFile), tmf);
  }

  /**
   * Accept all client certificates, recording certificate fingerprints for those that are not CA-signed.
   *
   * <p>
   * Excepting when a client presents a CA-signed certificate, the certificate fingerprint will be written to
   * {@code knownClientsFile}.
   *
   * <p>
   * Important: this provides no security as it is vulnerable to man-in-the-middle attacks.
   *
   * @param knownClientsFile The path to a file in which to record fingerprints.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory recordClientFingerprints(Path knownClientsFile) {
    requireNonNull(knownClientsFile);
    return recordClientFingerprints(knownClientsFile, true);
  }

  /**
   * Accept all client certificates, recording certificate fingerprints.
   *
   * <p>
   * For all connections, the fingerprint of the presented certificate will be written to {@code knownClientsFile}.
   *
   * <p>
   * Important: this provides no security as it is vulnerable to man-in-the-middle attacks.
   *
   * @param knownClientsFile The path to a file in which to record fingerprints.
   * @param skipCASigned If <tt>true</tt>, CA-signed certificates are not recorded.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory recordClientFingerprints(Path knownClientsFile, boolean skipCASigned) {
    requireNonNull(knownClientsFile);
    return wrap(ClientFingerprintTrustManager.record(knownClientsFile), skipCASigned);
  }

  /**
   * Accept all client certificates, recording certificate fingerprints for those that are not CA-signed.
   *
   * <p>
   * Excepting when a client presents a CA-signed certificate, the certificate fingerprint will be written to
   * {@code knownClientsFile}.
   *
   * <p>
   * Important: this provides no security as it is vulnerable to man-in-the-middle attacks.
   *
   * @param knownClientsFile The path to a file in which to record fingerprints.
   * @param tmf A {@link TrustManagerFactory} for checking server certificates against a CA.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory recordClientFingerprints(Path knownClientsFile, TrustManagerFactory tmf) {
    requireNonNull(knownClientsFile);
    requireNonNull(tmf);
    return wrap(ClientFingerprintTrustManager.record(knownClientsFile), tmf);
  }

  /**
   * Accept CA-signed client certificates, and otherwise trust client certificates on first access.
   *
   * <p>
   * Except when a client presents a CA-signed certificate, on first connection to this server the common name and
   * fingerprint of the presented certificate will be recorded. On subsequent connections, the client will be rejected
   * if the fingerprint has changed.
   *
   * <p>
   * <i>Note: unlike the seemingly equivalent {@link #trustServerOnFirstUse(Path)} method for authenticating servers,
   * this method for authenticating clients is <b>insecure</b> and <b>provides zero confidence in client identity</b>.
   * Unlike the server version, which bases the identity on the hostname and port the connection is being established
   * to, the client version only uses the common name of the certificate that the connecting client presents. Therefore,
   * clients can circumvent access control by using a different common name from any previously recorded client.</i>
   *
   * @param knownClientsFile The path to the file containing fingerprints.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory trustClientOnFirstAccess(Path knownClientsFile) {
    requireNonNull(knownClientsFile);
    return trustClientOnFirstAccess(knownClientsFile, true);
  }

  /**
   * Trust client certificates on first access.
   *
   * <p>
   * on first connection to this server the common name and fingerprint of the presented certificate will be recorded.
   * On subsequent connections, the client will be rejected if the fingerprint has changed.
   *
   * <p>
   * <i>Note: unlike the seemingly equivalent {@link #trustServerOnFirstUse(Path)} method for authenticating servers,
   * this method for authenticating clients is <b>insecure</b> and <b>provides zero confidence in client identity</b>.
   * Unlike the server version, which bases the identity on the hostname and port the connection is being established
   * to, the client version only uses the common name of the certificate that the connecting client presents. Therefore,
   * clients can circumvent access control by using a different common name from any previously recorded client.</i>
   *
   * @param knownClientsFile The path to the file containing fingerprints.
   * @param acceptCASigned If <tt>true</tt>, CA-signed certificates will always be accepted.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory trustClientOnFirstAccess(Path knownClientsFile, boolean acceptCASigned) {
    requireNonNull(knownClientsFile);
    return wrap(ClientFingerprintTrustManager.tofa(knownClientsFile), acceptCASigned);
  }

  /**
   * Accept CA-signed certificates, and otherwise trust client certificates on first access.
   *
   * <p>
   * Except when a client presents a CA-signed certificate, on first connection to this server the common name and
   * fingerprint of the presented certificate will be recorded. On subsequent connections, the client will be rejected
   * if the fingerprint has changed.
   *
   * <p>
   * <i>Note: unlike the seemingly equivalent {@link #trustServerOnFirstUse(Path)} method for authenticating servers,
   * this method for authenticating clients is <b>insecure</b> and <b>provides zero confidence in client identity</b>.
   * Unlike the server version, which bases the identity on the hostname and port the connection is being established
   * to, the client version only uses the common name of the certificate that the connecting client presents. Therefore,
   * clients can circumvent access control by using a different common name from any previously recorded client.</i>
   *
   * @param knownClientsFile The path to the file containing fingerprints.
   * @param tmf A {@link TrustManagerFactory} for checking server certificates against a CA.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory trustClientOnFirstAccess(Path knownClientsFile, TrustManagerFactory tmf) {
    requireNonNull(knownClientsFile);
    requireNonNull(tmf);
    return wrap(ClientFingerprintTrustManager.tofa(knownClientsFile), tmf);
  }

  /**
   * Require servers to present known certificates, or CA-signed certificates.
   *
   * <p>
   * If a certificate is not CA-signed, then its fingerprint must be present in the {@code knownClientsFile}.
   *
   * @param knownClientsFile The path to the file containing fingerprints.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory whitelistClients(Path knownClientsFile) {
    requireNonNull(knownClientsFile);
    return whitelistClients(knownClientsFile, true);
  }

  /**
   * Require clients to present known certificates.
   *
   * <p>
   * The fingerprint for a client certificate must be present in {@code knownClientsFile}.
   *
   * @param knownClientsFile The path to the file containing fingerprints.
   * @param acceptCASigned If <tt>true</tt>, CA-signed certificates will always be accepted.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory whitelistClients(Path knownClientsFile, boolean acceptCASigned) {
    requireNonNull(knownClientsFile);
    return wrap(ClientFingerprintTrustManager.whitelist(knownClientsFile), acceptCASigned);
  }

  /**
   * Require servers to present known certificates, or CA-signed certificates.
   *
   * <p>
   * If a certificate is not CA-signed, then its fingerprint must be present in the {@code knownClientsFile}.
   *
   * @param knownClientsFile The path to the file containing fingerprints.
   * @param tmf A {@link TrustManagerFactory} for checking server certificates against a CA.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory whitelistClients(Path knownClientsFile, TrustManagerFactory tmf) {
    requireNonNull(knownClientsFile);
    requireNonNull(tmf);
    return wrap(ClientFingerprintTrustManager.whitelist(knownClientsFile), tmf);
  }

  private static TrustManagerFactory wrap(X509TrustManager trustManager, boolean acceptCASigned) {
    return wrap(trustManager, acceptCASigned ? defaultTrustManagerFactory() : null);
  }

  private static TrustManagerFactory wrap(X509TrustManager trustManager, @Nullable TrustManagerFactory delegate) {
    if (delegate != null) {
      return new DelegatingTrustManagerFactory(delegate, trustManager);
    } else {
      return new SingleTrustManagerFactory(trustManager);
    }
  }

  private static TrustManagerFactory defaultTrustManagerFactory() {
    TrustManagerFactory delegate;
    try {
      delegate = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    } catch (NoSuchAlgorithmException e) {
      // not reachable
      throw new RuntimeException(e);
    }
    try {
      delegate.init((KeyStore) null);
    } catch (KeyStoreException e) {
      // not reachable
      throw new RuntimeException(e);
    }
    return delegate;
  }
}

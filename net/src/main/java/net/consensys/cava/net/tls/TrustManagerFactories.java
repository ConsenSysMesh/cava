package net.consensys.cava.net.tls;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
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
    return wrap(HostFingerprintTrustManager.record(knownServersFile), skipCASigned);
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
    return wrap(HostFingerprintTrustManager.record(knownServersFile), tmf);
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
    return wrap(HostFingerprintTrustManager.tofu(knownServersFile), acceptCASigned);
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
    return wrap(HostFingerprintTrustManager.tofu(knownServersFile), tmf);
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
    return wrap(HostFingerprintTrustManager.whitelist(knownServersFile), acceptCASigned);
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
    return wrap(HostFingerprintTrustManager.whitelist(knownServersFile), tmf);
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
    return wrap(FingerprintTrustManager.record(knownClientsFile), skipCASigned);
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
    return wrap(FingerprintTrustManager.record(knownClientsFile), tmf);
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
    return wrap(FingerprintTrustManager.whitelist(knownClientsFile), acceptCASigned);
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
    return wrap(FingerprintTrustManager.whitelist(knownClientsFile), tmf);
  }

  private static TrustManagerFactory wrap(X509TrustManager trustManager, boolean acceptCASigned) {
    return wrap(trustManager, acceptCASigned ? defaultTrustManagerFactory() : null);
  }

  private static TrustManagerFactory wrap(X509TrustManager trustManager, TrustManagerFactory delegate) {
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

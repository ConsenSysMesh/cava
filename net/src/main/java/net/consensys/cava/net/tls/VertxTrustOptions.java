/*
 * Copyright 2018, ConsenSys Inc.
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

import java.nio.file.Path;
import javax.net.ssl.TrustManagerFactory;

import io.vertx.core.net.TrustOptions;

/**
 * Vert.x {@link TrustOptions} for fingerprinting clients and servers.
 *
 * <p>
 * This class depends upon the Vert.X library being available on the classpath, along with its dependencies. See
 * https://vertx.io/download/. Vert.X can be included using the gradle dependency 'io.vertx:vertx-core'.
 */
public final class VertxTrustOptions {
  private VertxTrustOptions() {}

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
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions recordServerFingerprints(Path knownServersFile) {
    return new TrustManagerFactoryWrapper(TrustManagerFactories.recordServerFingerprints(knownServersFile));
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
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions recordServerFingerprints(Path knownServersFile, boolean skipCASigned) {
    return new TrustManagerFactoryWrapper(
        TrustManagerFactories.recordServerFingerprints(knownServersFile, skipCASigned));
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
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions recordServerFingerprints(Path knownServersFile, TrustManagerFactory tmf) {
    return new TrustManagerFactoryWrapper(TrustManagerFactories.recordServerFingerprints(knownServersFile, tmf));
  }

  /**
   * Accept CA-signed certificates, and otherwise trust server certificates on first use.
   *
   * <p>
   * Except when a server presents a CA-signed certificate, on first connection to a server (identified by host+port)
   * the fingerprint of the presented certificate will be recorded. On subsequent connections, the presented certificate
   * will be matched to the stored fingerprint to ensure it has not changed.
   *
   * @param knownServersFile The path to the file containing fingerprints by host.
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions trustServerOnFirstUse(Path knownServersFile) {
    return new TrustManagerFactoryWrapper(TrustManagerFactories.trustServerOnFirstUse(knownServersFile));
  }

  /**
   * Trust server certificates on first use.
   *
   * <p>
   * On first connection to a server (identified by host+port) the fingerprint of the presented certificate will be
   * recorded. On subsequent connections, the presented certificate will be matched to the stored fingerprint to ensure
   * it has not changed.
   *
   * @param knownServersFile The path to the file containing fingerprints by host.
   * @param acceptCASigned If <tt>true</tt>, CA-signed certificates will always be accepted (and the fingerprint will
   *        not be recorded).
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions trustServerOnFirstUse(Path knownServersFile, boolean acceptCASigned) {
    return new TrustManagerFactoryWrapper(
        TrustManagerFactories.trustServerOnFirstUse(knownServersFile, acceptCASigned));
  }

  /**
   * Accept CA-signed certificates, and otherwise trust server certificates on first use.
   *
   * <p>
   * Except when a server presents a CA-signed certificate, on first connection to a server (identified by host+port)
   * the fingerprint of the presented certificate will be recorded. On subsequent connections, the presented certificate
   * will be matched to the stored fingerprint to ensure it has not changed.
   *
   * @param knownServersFile The path to the file containing fingerprints by host.
   * @param tmf A {@link TrustManagerFactory} for checking server certificates against a CA.
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions trustServerOnFirstUse(Path knownServersFile, TrustManagerFactory tmf) {
    return new TrustManagerFactoryWrapper(TrustManagerFactories.trustServerOnFirstUse(knownServersFile, tmf));
  }

  /**
   * Require servers to present known certificates, or CA-signed certificates.
   *
   * <p>
   * If a certificate is not CA-signed, then its fingerprint must be present in the known servers file, associated with
   * the server (identified by host+port).
   *
   * @param knownServersFile The path to the file containing fingerprints by host.
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions whitelistServers(Path knownServersFile) {
    return new TrustManagerFactoryWrapper(TrustManagerFactories.whitelistServers(knownServersFile));
  }

  /**
   * Require servers to present known certificates.
   *
   * <p>
   * The fingerprint for a server certificate must be present in the known servers file, associated with the server
   * (identified by host+port).
   *
   * @param knownServersFile The path to the file containing fingerprints by host.
   * @param acceptCASigned If <tt>true</tt>, CA-signed certificates will always be accepted.
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions whitelistServers(Path knownServersFile, boolean acceptCASigned) {
    return new TrustManagerFactoryWrapper(TrustManagerFactories.whitelistServers(knownServersFile, acceptCASigned));
  }

  /**
   * Require servers to present known certificates, or CA-signed certificates.
   *
   * <p>
   * If a certificate is not CA-signed, then its fingerprint must be present in the known servers file, associated with
   * the server (identified by host+port).
   *
   * @param knownServersFile The path to the file containing fingerprints by host.
   * @param tmf A {@link TrustManagerFactory} for checking server certificates against a CA.
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions whitelistServers(Path knownServersFile, TrustManagerFactory tmf) {
    return new TrustManagerFactoryWrapper(TrustManagerFactories.whitelistServers(knownServersFile, tmf));
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
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions recordClientFingerprints(Path knownClientsFile) {
    return new TrustManagerFactoryWrapper(TrustManagerFactories.recordClientFingerprints(knownClientsFile));
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
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions recordClientFingerprints(Path knownClientsFile, boolean skipCASigned) {
    return new TrustManagerFactoryWrapper(
        TrustManagerFactories.recordClientFingerprints(knownClientsFile, skipCASigned));
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
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions recordClientFingerprints(Path knownClientsFile, TrustManagerFactory tmf) {
    return new TrustManagerFactoryWrapper(TrustManagerFactories.recordClientFingerprints(knownClientsFile, tmf));
  }

  /**
   * Require servers to present known certificates, or CA-signed certificates.
   *
   * <p>
   * If a certificate is not CA-signed, then its fingerprint must be present in the {@code knownClientsFile}.
   *
   * @param knownClientsFile The path to the file containing fingerprints.
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions whitelistClients(Path knownClientsFile) {
    return new TrustManagerFactoryWrapper(TrustManagerFactories.whitelistClients(knownClientsFile));
  }

  /**
   * Require clients to present known certificates.
   *
   * <p>
   * The fingerprint for a client certificate must be present in {@code knownClientsFile}.
   *
   * @param knownClientsFile The path to the file containing fingerprints.
   * @param acceptCASigned If <tt>true</tt>, CA-signed certificates will always be accepted.
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions whitelistClients(Path knownClientsFile, boolean acceptCASigned) {
    return new TrustManagerFactoryWrapper(TrustManagerFactories.whitelistClients(knownClientsFile, acceptCASigned));
  }

  /**
   * Require servers to present known certificates, or CA-signed certificates.
   *
   * <p>
   * If a certificate is not CA-signed, then its fingerprint must be present in the {@code knownClientsFile}.
   *
   * @param knownClientsFile The path to the file containing fingerprints.
   * @param tmf A {@link TrustManagerFactory} for checking server certificates against a CA.
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions whitelistClients(Path knownClientsFile, TrustManagerFactory tmf) {
    return new TrustManagerFactoryWrapper(TrustManagerFactories.whitelistClients(knownClientsFile, tmf));
  }
}

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

import static java.lang.String.format;
import static net.consensys.cava.crypto.Hash.sha2_256;

import net.consensys.cava.bytes.Bytes;

import java.io.IOException;
import java.net.Socket;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;

import sun.security.x509.X500Name;

final class ClientFingerprintTrustManager extends X509ExtendedTrustManager {

  private static final X509Certificate[] EMPTY_X509_CERTIFICATES = new X509Certificate[0];

  static ClientFingerprintTrustManager record(Path repository) {
    return new ClientFingerprintTrustManager(repository, true, true);
  }

  static ClientFingerprintTrustManager tofa(Path repository) {
    return new ClientFingerprintTrustManager(repository, true, false);
  }

  static ClientFingerprintTrustManager whitelist(Path repository) {
    return new ClientFingerprintTrustManager(repository, false, false);
  }

  private final FingerprintRepository repository;
  private final boolean acceptNewFingerprints;
  private final boolean updateFingerprints;

  private ClientFingerprintTrustManager(Path repository, boolean acceptNewFingerprints, boolean updateFingerprints) {
    this.repository = new FingerprintRepository(repository);
    this.acceptNewFingerprints = acceptNewFingerprints;
    this.updateFingerprints = updateFingerprints;
  }

  @Override
  public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
    X509Certificate cert = chain[0];
    String hostname;
    try {
      hostname = ((X500Name) cert.getSubjectDN()).getCommonName();
    } catch (IOException e) {
      throw new CertificateException("Invalid certificate " + cert.getSubjectDN());
    }
    checkTrusted(chain, hostname);
  }

  @Override
  public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
      throws CertificateException {
    X509Certificate cert = chain[0];
    String hostname;
    try {
      hostname = ((X500Name) cert.getSubjectDN()).getCommonName();
    } catch (IOException e) {
      throw new CertificateException("Invalid certificate " + cert.getSubjectDN());
    }
    checkTrusted(chain, hostname);
  }

  @Override
  public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void checkClientTrusted(X509Certificate[] chain, String authType) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void checkServerTrusted(X509Certificate[] chain, String authType) {
    throw new UnsupportedOperationException();
  }

  private void checkTrusted(X509Certificate[] chain, String host) throws CertificateException {
    X509Certificate cert = chain[0];
    Bytes fingerprint = Bytes.wrap(sha2_256(cert.getEncoded()));
    if (repository.contains(host, fingerprint)) {
      return;
    }

    if (repository.contains(host)) {
      if (!updateFingerprints) {
        throw new CertificateException(
            format(
                "Client identification has changed!!" + " Certificate for %s (%s) has fingerprint %s",
                host,
                cert.getSubjectDN(),
                fingerprint.toHexString().substring(2).toLowerCase()));
      }
    } else if (!acceptNewFingerprints) {
      throw new CertificateException(
          format(
              "Certificate for %s (%s) has unknown fingerprint %s",
              host,
              cert.getSubjectDN(),
              fingerprint.toHexString().substring(2).toLowerCase()));
    }

    repository.addFingerprint(host, fingerprint);
  }

  @Override
  public X509Certificate[] getAcceptedIssuers() {
    return EMPTY_X509_CERTIFICATES;
  }
}

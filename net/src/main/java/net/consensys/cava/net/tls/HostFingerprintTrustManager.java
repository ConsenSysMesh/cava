package net.consensys.cava.net.tls;

import net.consensys.cava.bytes.Bytes;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;

import com.google.common.hash.Hashing;

final class HostFingerprintTrustManager extends X509ExtendedTrustManager {

  private static final X509Certificate[] EMPTY_X509_CERTIFICATES = new X509Certificate[0];

  static HostFingerprintTrustManager record(Path repository) {
    return new HostFingerprintTrustManager(repository, true, true);
  }

  static HostFingerprintTrustManager tofu(Path repository) {
    return new HostFingerprintTrustManager(repository, true, false);
  }

  static HostFingerprintTrustManager whitelist(Path repository) {
    return new HostFingerprintTrustManager(repository, false, false);
  }

  private final HostFingerprintRepository repository;
  private final boolean acceptNewFingerprints;
  private final boolean updateFingerprints;

  private HostFingerprintTrustManager(Path repository, boolean acceptNewFingerprints, boolean updateFingerprints) {
    this.repository = new HostFingerprintRepository(repository);
    this.acceptNewFingerprints = acceptNewFingerprints;
    this.updateFingerprints = updateFingerprints;
  }

  @Override
  public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
    InetSocketAddress socketAddress = (InetSocketAddress) socket.getRemoteSocketAddress();
    checkTrusted(chain, socketAddress.getHostName(), socketAddress.getPort());
  }

  @Override
  public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
      throws CertificateException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
      throws CertificateException {
    checkTrusted(chain, engine.getPeerHost(), engine.getPeerPort());
  }

  @Override
  public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
    throw new UnsupportedOperationException();
  }

  private void checkTrusted(X509Certificate[] chain, String host, int port) throws CertificateException {
    X509Certificate cert = chain[0];
    Bytes fingerprint = Bytes.wrap(Hashing.sha256().hashBytes(cert.getEncoded()).asBytes());
    if (repository.contains(host, port, fingerprint)) {
      return;
    }

    if (updateFingerprints || (acceptNewFingerprints && !repository.contains(host, port))) {
      repository.addHostFingerprint(host, port, fingerprint);
      return;
    }

    throw new CertificateException(
        "Certificate for "
            + cert.getSubjectDN()
            + " ("
            + host
            + ":"
            + port
            + ") with unknown fingerprint: "
            + fingerprint);
  }

  @Override
  public X509Certificate[] getAcceptedIssuers() {
    return EMPTY_X509_CERTIFICATES;
  }
}

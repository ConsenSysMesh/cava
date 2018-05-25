package net.consensys.cava.net.tls;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.createDirectories;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

/**
 * Common utilities for TLS.
 *
 * <p>
 * This class depends upon the BouncyCastle library being available and added as a {@link java.security.Provider}. See
 * https://www.bouncycastle.org/wiki/display/JA1/Provider+Installation.
 *
 * <p>
 * BouncyCastle can be included using the gradle dependencies <code>org.bouncycastle:bcprov-jdk15on</code> and
 * <code>org.bouncycastle:bcpkix-jdk15on</code>.
 */
public final class TLS {
  private TLS() {}

  /**
   * Create a self-signed certificate, if it is not already present.
   *
   * <p>
   * If either the key or the certificate file are missing, both will be re-created as a self-signed certificate.
   *
   * @param key The key path.
   * @param certificate The certificate path.
   * @throws IOException If an IO error occurs creating the certificate.
   */
  public static void createSelfSignedCertificateIfMissing(Path key, Path certificate) throws IOException {
    if (Files.exists(certificate) && Files.exists(key)) {
      return;
    }

    createDirectories(certificate.getParent());
    createDirectories(key.getParent());

    Path keyFile;
    Path certFile;
    try {
      keyFile = Files.createTempFile(key.getParent(), "client-key", ".tmp");
      certFile = Files.createTempFile(certificate.getParent(), "client-cert", ".tmp");
    } catch (IOException e) {
      throw new TLSEnvironmentException(
          "Could not write temporary files when generating certificate " + e.getMessage(),
          e);
    }

    try {
      createSelfSignedCertificate(new Date(), keyFile, certFile);
    } catch (CertificateException | NoSuchAlgorithmException | OperatorCreationException | IOException e) {
      throw new TLSEnvironmentException("Could not generate certificate " + e.getMessage(), e);
    }

    try {
      Files.move(keyFile, key, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new TLSEnvironmentException("Error writing private key " + key.toString(), e);
    }

    try {
      Files.move(certFile, certificate, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new TLSEnvironmentException("Error writing certificate " + certificate.toString(), e);
    }
  }

  private static void createSelfSignedCertificate(Date now, Path key, Path certificate) throws NoSuchAlgorithmException,
      IOException,
      OperatorCreationException,
      CertificateException {
    KeyPairGenerator rsa = KeyPairGenerator.getInstance("RSA");
    rsa.initialize(2048, new SecureRandom());

    KeyPair keyPair = rsa.generateKeyPair();

    Calendar cal = Calendar.getInstance();
    cal.setTime(now);
    cal.add(Calendar.YEAR, 1);
    Date yearFromNow = cal.getTime();

    X500Name dn = new X500Name("CN=example.com");

    X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
        dn,
        new BigInteger(64, new SecureRandom()),
        now,
        yearFromNow,
        dn,
        keyPair.getPublic());

    ContentSigner signer =
        new JcaContentSignerBuilder("SHA256WithRSAEncryption").setProvider("BC").build(keyPair.getPrivate());
    X509Certificate x509Certificate =
        new JcaX509CertificateConverter().setProvider("BC").getCertificate(builder.build(signer));

    try (BufferedWriter writer = Files.newBufferedWriter(key, UTF_8); PemWriter pemWriter = new PemWriter(writer)) {
      pemWriter.writeObject(new PemObject("PRIVATE KEY", keyPair.getPrivate().getEncoded()));
    }

    try (BufferedWriter writer = Files.newBufferedWriter(certificate, UTF_8);
        PemWriter pemWriter = new PemWriter(writer)) {
      pemWriter.writeObject(new PemObject("CERTIFICATE", x509Certificate.getEncoded()));
    }
  }
}

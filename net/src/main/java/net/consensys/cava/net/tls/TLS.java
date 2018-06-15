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

    Path keyFile = Files.createTempFile(key.getParent(), "client-key", ".tmp");
    Path certFile = Files.createTempFile(certificate.getParent(), "client-cert", ".tmp");

    try {
      createSelfSignedCertificate(new Date(), keyFile, certFile);
    } catch (CertificateException | NoSuchAlgorithmException | OperatorCreationException e) {
      throw new TLSEnvironmentException("Could not generate certificate: " + e.getMessage(), e);
    }

    Files.move(keyFile, key, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    Files.move(certFile, certificate, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
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

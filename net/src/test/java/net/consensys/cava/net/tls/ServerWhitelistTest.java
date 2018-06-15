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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.cava.junit.TempDirectory;
import net.consensys.cava.junit.TempDirectoryExtension;
import net.consensys.cava.junit.VertxExtension;
import net.consensys.cava.junit.VertxInstance;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import javax.net.ssl.SSLException;

import com.google.common.hash.Hashing;
import io.netty.util.internal.StringUtil;
import io.vertx.core.Vertx;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.SelfSignedCertificate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TempDirectoryExtension.class)
@ExtendWith(VertxExtension.class)
class ServerWhitelistTest {

  private static HttpClient caClient;
  private static String whitelistedFingerprint;
  private static HttpClient whitelistClient;
  private static HttpClient unknownClient;

  private Path knownClientsFile;
  private HttpServer httpServer;

  @BeforeAll
  static void setupClients(@TempDirectory Path tempDir, @VertxInstance Vertx vertx) throws Exception {
    SelfSignedCertificate caClientCert = SelfSignedCertificate.create();
    SecurityTestUtils.configureJDKTrustStore(tempDir, caClientCert);
    caClient = vertx.createHttpClient(
        new HttpClientOptions().setTrustOptions(InsecureTrustOptions.INSTANCE).setSsl(true).setKeyCertOptions(
            caClientCert.keyCertOptions()));

    SelfSignedCertificate nonCAClientCert = SelfSignedCertificate.create();
    whitelistedFingerprint = StringUtil.toHexStringPadded(
        Hashing
            .sha256()
            .hashBytes(SecurityTestUtils.loadPEM(Paths.get(nonCAClientCert.keyCertOptions().getCertPath())))
            .asBytes());

    HttpClientOptions whitelistClientOptions = new HttpClientOptions();
    whitelistClientOptions
        .setSsl(true)
        .setKeyCertOptions(nonCAClientCert.keyCertOptions())
        .setTrustOptions(InsecureTrustOptions.INSTANCE)
        .setConnectTimeout(1500)
        .setReuseAddress(true)
        .setReusePort(true);
    whitelistClient = vertx.createHttpClient(whitelistClientOptions);

    HttpClientOptions unknownClientOptions = new HttpClientOptions();
    unknownClientOptions
        .setSsl(true)
        .setKeyCertOptions(SelfSignedCertificate.create().keyCertOptions())
        .setTrustOptions(InsecureTrustOptions.INSTANCE)
        .setConnectTimeout(1500)
        .setReuseAddress(true)
        .setReusePort(true);
    unknownClient = vertx.createHttpClient(unknownClientOptions);
  }

  @BeforeEach
  void startServer(@TempDirectory Path tempDir, @VertxInstance Vertx vertx) throws Exception {
    knownClientsFile = tempDir.resolve("knownclients.txt");
    Files.write(knownClientsFile, whitelistedFingerprint.getBytes(UTF_8));

    SelfSignedCertificate serverCert = SelfSignedCertificate.create();
    HttpServerOptions options = new HttpServerOptions();
    options
        .setSsl(true)
        .setClientAuth(ClientAuth.REQUIRED)
        .setPemKeyCertOptions(serverCert.keyCertOptions())
        .setTrustOptions(VertxTrustOptions.whitelistClients(knownClientsFile, false))
        .setIdleTimeout(1500)
        .setReuseAddress(true)
        .setReusePort(true);
    httpServer = vertx.createHttpServer(options);
    SecurityTestUtils.configureAndStartTestServer(httpServer);
  }

  @AfterEach
  void stopServer() throws Exception {
    httpServer.close();

    List<String> knownClients = Files.readAllLines(knownClientsFile);
    assertEquals(1, knownClients.size());
    assertEquals(whitelistedFingerprint, knownClients.get(0));
  }

  @AfterAll
  static void cleanupClients() {
    caClient.close();
    whitelistClient.close();
    unknownClient.close();
    System.clearProperty("javax.net.ssl.trustStore");
    System.clearProperty("javax.net.ssl.trustStorePassword");
  }

  @Test
  void shouldNotValidateUsingCertificate() {
    HttpClientRequest req = caClient.get(httpServer.actualPort(), "localhost", "/upcheck");
    CompletableFuture<HttpClientResponse> respFuture = new CompletableFuture<>();
    req.handler(respFuture::complete).exceptionHandler(respFuture::completeExceptionally).end();
    try {
      respFuture.join();
    } catch (CompletionException e) {
      assertTrue(e.getCause() instanceof SSLException);
    }
  }

  @Test
  void shouldValidateWhitelisted() {
    HttpClientRequest req = whitelistClient.get(httpServer.actualPort(), "localhost", "/upcheck");
    CompletableFuture<HttpClientResponse> respFuture = new CompletableFuture<>();
    req.handler(respFuture::complete).exceptionHandler(respFuture::completeExceptionally).end();
    HttpClientResponse resp = respFuture.join();
    assertEquals(200, resp.statusCode());
  }

  @Test
  void shouldRejectNonWhitelisted() {
    HttpClientRequest req = unknownClient.get(httpServer.actualPort(), "localhost", "/upcheck");
    CompletableFuture<HttpClientResponse> respFuture = new CompletableFuture<>();
    req.handler(respFuture::complete).exceptionHandler(respFuture::completeExceptionally).end();
    try {
      respFuture.join();
    } catch (CompletionException e) {
      assertTrue(e.getCause() instanceof SSLException);
    }
  }
}

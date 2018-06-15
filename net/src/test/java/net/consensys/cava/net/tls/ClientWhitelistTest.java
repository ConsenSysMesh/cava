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

import static net.consensys.cava.net.tls.SecurityTestUtils.startServer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.cava.junit.TempDirectory;
import net.consensys.cava.junit.TempDirectoryExtension;
import net.consensys.cava.junit.VertxExtension;
import net.consensys.cava.junit.VertxInstance;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import javax.net.ssl.SSLException;

import com.google.common.hash.Hashing;
import io.netty.util.internal.StringUtil;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
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
class ClientWhitelistTest {

  private static String whitelistedFingerprint;
  private static HttpServer caValidServer;
  private static HttpServer whitelistedServer;
  private static HttpServer unknownServer;

  private Path knownServersFile;
  private HttpClient client;

  @BeforeAll
  static void startServers(@TempDirectory Path tempDir, @VertxInstance Vertx vertx) throws Exception {
    SelfSignedCertificate caSignedCert = SelfSignedCertificate.create("localhost");
    SecurityTestUtils.configureJDKTrustStore(tempDir, caSignedCert);
    caValidServer = vertx
        .createHttpServer(new HttpServerOptions().setSsl(true).setPemKeyCertOptions(caSignedCert.keyCertOptions()))
        .requestHandler(context -> context.response().end("OK"));
    startServer(caValidServer);

    SelfSignedCertificate fooCert = SelfSignedCertificate.create("foo.com");
    whitelistedFingerprint = StringUtil.toHexStringPadded(
        Hashing
            .sha256()
            .hashBytes(SecurityTestUtils.loadPEM(Paths.get(fooCert.keyCertOptions().getCertPath())))
            .asBytes());

    whitelistedServer = vertx
        .createHttpServer(new HttpServerOptions().setSsl(true).setPemKeyCertOptions(fooCert.keyCertOptions()))
        .requestHandler(context -> context.response().end("OK"));
    startServer(whitelistedServer);

    SelfSignedCertificate unknownCert = SelfSignedCertificate.create();
    unknownServer = vertx
        .createHttpServer(new HttpServerOptions().setSsl(true).setPemKeyCertOptions(unknownCert.keyCertOptions()))
        .requestHandler(context -> context.response().end("OK"));
    startServer(unknownServer);
  }

  @BeforeEach
  void setupClient(@TempDirectory Path tempDir, @VertxInstance Vertx vertx) throws Exception {
    knownServersFile = tempDir.resolve("knownclients.txt");
    Files.write(
        knownServersFile,
        Arrays.asList("#First line", "localhost:" + whitelistedServer.actualPort() + " " + whitelistedFingerprint));

    HttpClientOptions options = new HttpClientOptions();
    options
        .setSsl(true)
        .setTrustOptions(VertxTrustOptions.whitelistServers(knownServersFile, false))
        .setConnectTimeout(1500)
        .setReuseAddress(true)
        .setReusePort(true);
    client = vertx.createHttpClient(options);
  }

  @AfterEach
  void cleanupClient() throws Exception {
    client.close();

    List<String> knownServers = Files.readAllLines(knownServersFile);
    assertEquals(2, knownServers.size(), "Host was verified via TOFU and not CA");
    assertEquals("#First line", knownServers.get(0));
    assertEquals("localhost:" + whitelistedServer.actualPort() + " " + whitelistedFingerprint, knownServers.get(1));
  }

  @AfterAll
  static void stopServers() {
    caValidServer.close();
    whitelistedServer.close();
    unknownServer.close();
    System.clearProperty("javax.net.ssl.trustStore");
    System.clearProperty("javax.net.ssl.trustStorePassword");
  }

  @Test
  void shouldNotValidateUsingCertificate() {
    CompletableFuture<Integer> statusCode = new CompletableFuture<>();
    client
        .post(
            caValidServer.actualPort(),
            "localhost",
            "/sample",
            response -> statusCode.complete(response.statusCode()))
        .exceptionHandler(statusCode::completeExceptionally)
        .end();
    try {
      statusCode.join();
    } catch (CompletionException e) {
      assertTrue(e.getCause() instanceof SSLException);
    }
  }

  @Test
  void shouldValidateWhitelisted() {
    CompletableFuture<Integer> statusCode = new CompletableFuture<>();
    client
        .post(
            whitelistedServer.actualPort(),
            "localhost",
            "/sample",
            response -> statusCode.complete(response.statusCode()))
        .exceptionHandler(statusCode::completeExceptionally)
        .end();
    assertEquals((Integer) 200, statusCode.join());
  }

  @Test
  void shouldRejectNonWhitelisted() {
    CompletableFuture<Integer> statusCode = new CompletableFuture<>();
    client
        .post(
            unknownServer.actualPort(),
            "localhost",
            "/sample",
            response -> statusCode.complete(response.statusCode()))
        .exceptionHandler(statusCode::completeExceptionally)
        .end();
    try {
      statusCode.join();
    } catch (CompletionException e) {
      assertTrue(e.getCause() instanceof SSLException);
    }
  }
}

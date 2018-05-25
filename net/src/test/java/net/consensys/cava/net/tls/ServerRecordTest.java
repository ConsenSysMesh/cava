package net.consensys.cava.net.tls;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.consensys.cava.junit.TempDirectory;
import net.consensys.cava.junit.TempDirectoryExtension;
import net.consensys.cava.junit.VertxExtension;
import net.consensys.cava.junit.VertxInstance;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
class ServerRecordTest {

  private static String caClientFingerprint;
  private static HttpClient caClient;
  private static String unknownClientFingerprint;
  private static HttpClient unknownClient1;
  private static HttpClient unknownClient2;

  private Path knownClientsFile;
  private HttpServer httpServer;

  @BeforeAll
  static void setupClients(@TempDirectory Path tempDir, @VertxInstance Vertx vertx) throws Exception {
    SelfSignedCertificate caClientCert = SelfSignedCertificate.create();
    SecurityTestUtils.configureJDKTrustStore(tempDir, caClientCert);
    caClientFingerprint = StringUtil.toHexStringPadded(
        Hashing
            .sha256()
            .hashBytes(SecurityTestUtils.loadPEM(Paths.get(caClientCert.keyCertOptions().getCertPath())))
            .asBytes());

    caClient = vertx.createHttpClient(
        new HttpClientOptions().setTrustOptions(InsecureTrustOptions.INSTANCE).setSsl(true).setKeyCertOptions(
            caClientCert.keyCertOptions()));

    SelfSignedCertificate nonCAClientCert = SelfSignedCertificate.create();
    unknownClientFingerprint = StringUtil.toHexStringPadded(
        Hashing
            .sha256()
            .hashBytes(SecurityTestUtils.loadPEM(Paths.get(nonCAClientCert.keyCertOptions().getCertPath())))
            .asBytes());

    HttpClientOptions unknownClient1Options = new HttpClientOptions();
    unknownClient1Options
        .setSsl(true)
        .setKeyCertOptions(nonCAClientCert.keyCertOptions())
        .setTrustOptions(InsecureTrustOptions.INSTANCE)
        .setConnectTimeout(1500)
        .setReuseAddress(true)
        .setReusePort(true);
    unknownClient1 = vertx.createHttpClient(unknownClient1Options);

    HttpClientOptions unknownClient2Options = new HttpClientOptions();
    unknownClient2Options
        .setSsl(true)
        .setKeyCertOptions(SelfSignedCertificate.create().keyCertOptions())
        .setTrustOptions(InsecureTrustOptions.INSTANCE)
        .setConnectTimeout(1500)
        .setReuseAddress(true)
        .setReusePort(true);
    unknownClient2 = vertx.createHttpClient(unknownClient2Options);
  }

  @BeforeEach
  void startServer(@TempDirectory Path tempDir, @VertxInstance Vertx vertx) throws Exception {
    knownClientsFile = tempDir.resolve("knownclients.txt");
    Files.write(knownClientsFile, Collections.singletonList("#First line"));

    SelfSignedCertificate serverCert = SelfSignedCertificate.create();
    HttpServerOptions options = new HttpServerOptions();
    options
        .setSsl(true)
        .setClientAuth(ClientAuth.REQUIRED)
        .setPemKeyCertOptions(serverCert.keyCertOptions())
        .setTrustOptions(VertxTrustOptions.recordClientFingerprints(knownClientsFile, false))
        .setIdleTimeout(1500)
        .setReuseAddress(true)
        .setReusePort(true);
    httpServer = vertx.createHttpServer(options);
    SecurityTestUtils.configureAndStartTestServer(httpServer);
  }

  @AfterEach
  void stopServer() {
    httpServer.close();
  }

  @AfterAll
  static void cleanupClients() {
    caClient.close();
    unknownClient1.close();
    unknownClient2.close();
  }

  @Test
  void shouldNotValidateUsingCertificate() throws Exception {
    HttpClientRequest req = caClient.get(httpServer.actualPort(), "localhost", "/upcheck");
    CompletableFuture<HttpClientResponse> respFuture = new CompletableFuture<>();
    req.handler(respFuture::complete).exceptionHandler(respFuture::completeExceptionally).end();
    HttpClientResponse resp = respFuture.join();
    assertEquals(200, resp.statusCode());

    List<String> knownClients = Files.readAllLines(knownClientsFile);
    assertEquals(2, knownClients.size(), String.join("\n", knownClients));
    assertEquals("#First line", knownClients.get(0));
    assertEquals(caClientFingerprint, knownClients.get(1));
  }

  @Test
  void shouldRecordMultipleFingerprints() throws Exception {
    HttpClientRequest req = unknownClient1.get(httpServer.actualPort(), "localhost", "/upcheck");
    CompletableFuture<HttpClientResponse> respFuture = new CompletableFuture<>();
    req.handler(respFuture::complete).exceptionHandler(respFuture::completeExceptionally).end();
    HttpClientResponse resp = respFuture.join();
    assertEquals(200, resp.statusCode());

    List<String> knownClients = Files.readAllLines(knownClientsFile);
    assertEquals(2, knownClients.size(), String.join("\n", knownClients));
    assertEquals("#First line", knownClients.get(0));
    assertEquals(unknownClientFingerprint, knownClients.get(1));

    req = unknownClient2.get(httpServer.actualPort(), "localhost", "/upcheck");
    respFuture = new CompletableFuture<>();
    req.handler(respFuture::complete).exceptionHandler(respFuture::completeExceptionally).end();
    resp = respFuture.join();
    assertEquals(200, resp.statusCode());

    knownClients = Files.readAllLines(knownClientsFile);
    assertEquals(3, knownClients.size(), String.join("\n", knownClients));
    assertEquals("#First line", knownClients.get(0));
  }
}

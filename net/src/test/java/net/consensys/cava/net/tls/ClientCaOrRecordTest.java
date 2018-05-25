package net.consensys.cava.net.tls;

import static net.consensys.cava.net.tls.SecurityTestUtils.startServer;
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
class ClientCaOrRecordTest {

  private static String caValidFingerprint;
  private static String fooFingerprint;
  private static HttpServer caValidServer;
  private static HttpServer fooServer;
  private static HttpServer foobarServer;

  private Path knownServersFile;
  private HttpClient client;

  @BeforeAll
  static void startServers(@TempDirectory Path tempDir, @VertxInstance Vertx vertx) throws Exception {
    SelfSignedCertificate caSignedCert = SelfSignedCertificate.create("localhost");
    SecurityTestUtils.configureJDKTrustStore(tempDir, caSignedCert);
    caValidFingerprint = StringUtil.toHexStringPadded(
        Hashing
            .sha256()
            .hashBytes(SecurityTestUtils.loadPEM(Paths.get(caSignedCert.keyCertOptions().getCertPath())))
            .asBytes());

    caValidServer = vertx
        .createHttpServer(new HttpServerOptions().setSsl(true).setPemKeyCertOptions(caSignedCert.keyCertOptions()))
        .requestHandler(context -> context.response().end("OK"));
    startServer(caValidServer);

    SelfSignedCertificate fooCert = SelfSignedCertificate.create("foo.com");
    fooFingerprint = StringUtil.toHexStringPadded(
        Hashing
            .sha256()
            .hashBytes(SecurityTestUtils.loadPEM(Paths.get(fooCert.keyCertOptions().getCertPath())))
            .asBytes());

    fooServer = vertx
        .createHttpServer(new HttpServerOptions().setSsl(true).setPemKeyCertOptions(fooCert.keyCertOptions()))
        .requestHandler(context -> context.response().end("OK"));
    startServer(fooServer);

    foobarServer = vertx
        .createHttpServer(
            new HttpServerOptions().setSsl(true).setPemKeyCertOptions(
                SelfSignedCertificate.create("foobar.com").keyCertOptions()))
        .requestHandler(context -> context.response().end("OK"));
    startServer(foobarServer);
  }

  @BeforeEach
  void setupClient(@TempDirectory Path tempDir, @VertxInstance Vertx vertx) throws Exception {
    knownServersFile = tempDir.resolve("knownclients.txt");
    Files.write(knownServersFile, Collections.singletonList("#First line"));

    HttpClientOptions options = new HttpClientOptions();
    options
        .setSsl(true)
        .setTrustOptions(VertxTrustOptions.recordServerFingerprints(knownServersFile))
        .setConnectTimeout(1500)
        .setReuseAddress(true)
        .setReusePort(true);
    client = vertx.createHttpClient(options);
  }

  @AfterEach
  void cleanupClient() {
    client.close();
  }

  @AfterAll
  static void stopServers() {
    caValidServer.close();
    fooServer.close();
    foobarServer.close();
    System.clearProperty("javax.net.ssl.trustStore");
    System.clearProperty("javax.net.ssl.trustStorePassword");
  }

  @Test
  void shouldValidateUsingCertificate() throws Exception {
    CompletableFuture<Integer> statusCode = new CompletableFuture<>();
    client
        .post(
            caValidServer.actualPort(),
            "localhost",
            "/sample",
            response -> statusCode.complete(response.statusCode()))
        .exceptionHandler(statusCode::completeExceptionally)
        .end();
    assertEquals((Integer) 200, statusCode.join());

    List<String> knownServers = Files.readAllLines(knownServersFile);
    assertEquals(1, knownServers.size(), "CA verified host should not have been recorded");
    assertEquals("#First line", knownServers.get(0));
  }

  @Test
  void shouldFallbackToRecordingForInvalidName() throws Exception {
    CompletableFuture<Integer> statusCode = new CompletableFuture<>();
    client
        .post(
            caValidServer.actualPort(),
            "127.0.0.1",
            "/sample",
            response -> statusCode.complete(response.statusCode()))
        .exceptionHandler(statusCode::completeExceptionally)
        .end();
    assertEquals((Integer) 200, statusCode.join());

    List<String> knownServers = Files.readAllLines(knownServersFile);
    assertEquals(2, knownServers.size(), String.join("\n", knownServers));
    assertEquals("#First line", knownServers.get(0));
    assertEquals("127.0.0.1:" + caValidServer.actualPort() + " " + caValidFingerprint, knownServers.get(1));
  }

  @Test
  void shouldRecordMultipleHosts() throws Exception {
    CompletableFuture<Integer> statusCode = new CompletableFuture<>();
    client
        .post(fooServer.actualPort(), "localhost", "/sample", response -> statusCode.complete(response.statusCode()))
        .exceptionHandler(statusCode::completeExceptionally)
        .end();
    assertEquals((Integer) 200, statusCode.join());

    List<String> knownServers = Files.readAllLines(knownServersFile);
    assertEquals(2, knownServers.size(), String.join("\n", knownServers));
    assertEquals("#First line", knownServers.get(0));
    assertEquals("localhost:" + fooServer.actualPort() + " " + fooFingerprint, knownServers.get(1));

    CompletableFuture<Integer> secondStatusCode = new CompletableFuture<>();
    client
        .post(
            foobarServer.actualPort(),
            "localhost",
            "/sample",
            response -> secondStatusCode.complete(response.statusCode()))
        .end();
    assertEquals((Integer) 200, secondStatusCode.join());

    knownServers = Files.readAllLines(knownServersFile);
    assertEquals(3, knownServers.size(), String.join("\n", knownServers));
  }
}

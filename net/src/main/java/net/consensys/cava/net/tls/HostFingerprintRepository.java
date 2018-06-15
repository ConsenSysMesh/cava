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

import static java.nio.file.Files.createDirectories;
import static net.consensys.cava.io.file.Files.atomicReplace;
import static net.consensys.cava.io.file.Files.copy;
import static net.consensys.cava.io.file.Files.createFileIfMissing;

import net.consensys.cava.bytes.Bytes;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class HostFingerprintRepository {

  private final Path fingerprintFile;
  private final Map<String, Bytes> fingerprints;

  HostFingerprintRepository(Path fingerprintFile) {
    try {
      createDirectories(fingerprintFile.getParent());
      createFileIfMissing(fingerprintFile);
    } catch (IOException e) {
      throw new TLSEnvironmentException("Cannot create fingerprint file " + fingerprintFile, e);
    }
    try {
      this.fingerprintFile = fingerprintFile;
      try (Stream<String> lines = Files.lines(fingerprintFile)) {
        this.fingerprints = lines
            .map(String::trim)
            .filter(line -> !line.isEmpty() && !line.startsWith("#"))
            .map(line -> line.split("\\s+", 2))
            .collect(Collectors.toMap(segments -> segments[0], segments -> Bytes.fromHexString(segments[1])));
      }
    } catch (IOException e) {
      throw new TLSEnvironmentException("Cannot read fingerprint file " + fingerprintFile, e);
    }
  }

  boolean contains(String host, int port) {
    return fingerprints.containsKey(hostIdentifier(host, port));
  }

  boolean contains(String host, int port, Bytes fingerprint) {
    return contains(hostIdentifier(host, port), fingerprint);
  }

  private boolean contains(String hostIdentifier, Bytes fingerprint) {
    return fingerprint.equals(fingerprints.get(hostIdentifier));
  }

  void addHostFingerprint(String host, int port, Bytes fingerprint) {
    String hostIdentifier = hostIdentifier(host, port);
    try {
      if (!contains(hostIdentifier, fingerprint)) {
        synchronized (fingerprints) {
          if (!contains(hostIdentifier, fingerprint)) {
            atomicReplace(fingerprintFile, writer -> {
              copy(fingerprintFile, writer);
              writer.write(hostIdentifier);
              writer.write(' ');
              writer.write(fingerprint.toHexString().substring(2).toLowerCase());
              writer.write(System.lineSeparator());
            });
            fingerprints.put(hostIdentifier, fingerprint);
          }
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private String hostIdentifier(String host, int port) {
    return host.trim().toLowerCase() + ":" + port;
  }
}

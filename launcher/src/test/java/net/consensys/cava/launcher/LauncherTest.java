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
package net.consensys.cava.launcher;

import static net.consensys.cava.launcher.Launcher.args;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.logl.LoggerProvider;

class LauncherTest {

  private boolean called = false;

  @BeforeEach
  void resetCalled() {
    called = false;
  }

  @Test
  void testLaunchWithNoHandler() {
    assertThrows(
        LaunchException.class,
        () -> new Launcher().loggerProvider(LoggerProvider.nullProvider()).launchExceptionHandler((l, e, o, t) -> {
          throw (LaunchException) t;
        }).run());
  }

  @Test
  void testWithDefaultHandler() {
    new Launcher()
        .loggerProvider(LoggerProvider.nullProvider())
        .handle((logger, out, err, args) -> called = true)
        .run();
    assertTrue(called);
  }

  @Test
  void testLaunchShowVersionShortForm() {
    new Launcher()
        .loggerProvider(LoggerProvider.nullProvider())
        .handle("-v", (logger, out, err, args) -> called = true)
        .handle((logger, out, err, args) -> out.println("started"))
        .run("-v");
    assertTrue(called);
  }

  @Test
  void testLaunchShowVersionSeveralFlags() {
    new Launcher()
        .loggerProvider(LoggerProvider.nullProvider())
        .handle(args("-v", "--version", "-h"), (logger, out, err, args) -> called = true)
        .handle((logger, out, err, args) -> out.println("started"))
        .run("-h");
    assertTrue(called);
  }

  @Test
  void throwExceptionWhenPassedNull() {
    assertThrows(
        NullPointerException.class,
        () -> new Launcher().handle((String) null, (logger, out, err, args) -> err.println("there")));
  }

  @Test
  void throwExceptionWhenPassedArgsContainingNull() {
    assertThrows(
        NullPointerException.class,
        () -> new Launcher()
            .loggerProvider(LoggerProvider.nullProvider())
            .handle(new String[] {null}, (logger, out, err, args) -> err.println("there")));
  }

  @Test
  void overrideExceptionHandling() {
    Exception e = assertThrows(
        IllegalArgumentException.class,
        () -> new Launcher().loggerProvider(LoggerProvider.nullProvider()).handle((logger, out, err, args) -> {
          throw new IllegalArgumentException("no");
        }).launchExceptionHandler((logger, err, out, t) -> {
          throw (IllegalArgumentException) t;
        }).run());
    assertEquals("no", e.getMessage());
  }

  @Test
  void overrideSysErr() {
    PrintStream mySysErr = new PrintStream(new ByteArrayOutputStream(), true);
    new Launcher()
        .loggerProvider(LoggerProvider.nullProvider())
        .syserr(mySysErr)
        .handle((logger, out, err, args) -> called = err == mySysErr)
        .run();
    assertTrue(called);
  }

  @Test
  void overrideSysOut() {
    PrintStream mySysOut = new PrintStream(new ByteArrayOutputStream(), true);
    new Launcher()
        .loggerProvider(LoggerProvider.nullProvider())
        .sysout(mySysOut)
        .handle((logger, out, err, args) -> called = out == mySysOut)
        .run();
    assertTrue(called);
  }

  @Test
  void noLoggerProviderConfigured() {
    Exception e = assertThrows(IllegalArgumentException.class, () -> new Launcher().run("foo"));
    assertEquals("No logger provider configured", e.getMessage());
  }
}

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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.logl.Logger;
import org.logl.LoggerProvider;

/**
 * Provides common application bootstrapping.
 *
 * <p>
 * Used to configure the behavior of the program at startup and provide handlers for command line arguments.
 */
public final class Launcher {

  private final Map<String, LauncherHandler> handlerMap = new HashMap<>();

  private LoggerProvider loggerProvider = null;
  private LauncherHandler defaultHandler = null;
  private PrintStream sysout = System.out;
  private PrintStream syserr = System.err;

  private LaunchExceptionHandler launchExceptionHandler = (logger, syserr, sysout, t) -> {
    syserr.println(t.getMessage());
    System.exit(1);
  };

  /**
   * Runs the application with the arguments provided.
   *
   * @param args The command line arguments.
   */
  public void launch(String... args) {
    if (loggerProvider == null) {
      throw new IllegalStateException("Logger provider has not been set");
    }
    if (defaultHandler == null) {
      throw new IllegalStateException("Default handler has not been set");
    }
    Logger logger = loggerProvider.getLogger("launch");
    try {
      for (String arg : args) {
        LauncherHandler handler = handlerMap.get(arg);
        if (handler != null) {
          handler.handle(logger, sysout, syserr, args);
        }
      }
      defaultHandler.handle(logger, sysout, syserr, args);
    } catch (Exception t) {
      launchExceptionHandler.handle(logger, syserr, sysout, t);
    }
  }

  /**
   * Adds a new command line argument handler.
   *
   * @param arg The arguments to handle.
   * @param handler The handler to execute if the argument is present.
   * @return This launcher.
   */
  public Launcher handle(String arg, LauncherHandler handler) {
    return handle(new String[] { arg }, handler);
  }

  /**
   * Adds a new command line argument handler.
   *
   * @param args The arguments to handle.
   * @param handler The handler to execute if any of the arguments are present.
   * @return This launcher.
   */
  public Launcher handle(String[] args, LauncherHandler handler) {
    checkNotNull(args);
    checkNotNull(handler);
    checkArgument(args.length > 0);

    for (String arg : args) {
      checkNotNull(arg);
      handlerMap.put(arg, handler);
    }
    return this;
  }

  /**
   * Adds a default handler to execute after all other handlers.
   *
   * @param handler The default handler.
   * @return This launcher.
   */
  public Launcher handle(LauncherHandler handler) {
    this.defaultHandler = handler;
    return this;
  }

  /**
   * Configures the logger provider to use for the launcher logger.
   *
   * @param provider The logger provider.
   * @return This launcher.
   */
  public Launcher loggerProvider(LoggerProvider provider) {
    this.loggerProvider = provider;
    return this;
  }

  /**
   * Configures the syserr output provided to the handler during execution.
   *
   * @param printStream The print stream to use for standard error output.
   * @return This launcher.
   */
  public Launcher syserr(PrintStream printStream) {
    this.syserr = printStream;
    return this;
  }

  /**
   * Configures the sysout output provided to the handler during execution.
   *
   * @param printStream The print stream to use for standard output.
   * @return This launcher.
   */
  public Launcher sysout(PrintStream printStream) {
    this.sysout = printStream;
    return this;
  }

  /**
   * Configures the handler to manage exceptions arising during the initial handling. The default handler will print the
   * exception message to syserr and exit with an exit code of 1.
   *
   * @param launchExceptionHandler The handler.
   * @return This launcher.
   */
  public Launcher exceptionHandler(LaunchExceptionHandler launchExceptionHandler) {
    this.launchExceptionHandler = launchExceptionHandler;
    return this;
  }

  /**
   * The functional interface for launch handlers.
   */
  @FunctionalInterface
  public interface LauncherHandler {

    /**
     * Executes with a given set of arguments.
     * 
     * @param logger The logger to use for normal logging.
     * @param out The standard output stream.
     * @param err The standard error stream.
     * @param args the set of arguments provided to the program.
     */
    void handle(Logger logger, PrintStream out, PrintStream err, String[] args);

  }

  /**
   * The functional interface for launch exception handlers.
   */
  @FunctionalInterface
  public interface LaunchExceptionHandler {

    /**
     * Handles an exception thrown during launch.
     *
     * @param logger The logger to use for normal logging.
     * @param out The standard output stream.
     * @param err The standard error stream.
     * @param e The exception thrown during launch.
     */
    void handle(Logger logger, PrintStream out, PrintStream err, Exception e);
  }
}

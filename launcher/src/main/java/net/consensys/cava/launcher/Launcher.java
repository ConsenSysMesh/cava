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
 * Launches a program, allowing to create handlers for specific set of arguments.
 * <p>
 * It is used to configure the behavior of the program at startup.
 *
 */
public final class Launcher {

  /**
   * Syntactic sugar to cast a varags String array to a native String array.
   *
   * @param arguments the varargs arguments
   * @return the native array of varargs arguments
   */
  public static String[] args(String... arguments) {
    return arguments;
  }

  private final Map<String, LauncherHandler> handlerMap = new HashMap<>();

  private LoggerProvider loggerProvider = null;

  private LauncherHandler defaultHandler;

  private PrintStream sysout = System.out;

  private PrintStream syserr = System.err;

  private LaunchExceptionHandler launchExceptionHandler = (logger, syserr, sysout, t) -> {
    syserr.println(t.getMessage());
    System.exit(1);
  };

  /**
   * Runs the application with the arguments provided.
   *
   * @param args the arguments passed to the main method.
   */
  public void run(String... args) {
    if (loggerProvider == null) {
      throw new IllegalArgumentException("No logger provider configured");
    }
    Logger logger = loggerProvider.getLogger("launch");
    try {
      for (String arg : args) {
        LauncherHandler handler = handlerMap.get(arg);
        if (handler != null) {
          handler.handle(logger, sysout, syserr, args);
        }
      }
      if (defaultHandler != null) {
        defaultHandler.handle(logger, sysout, syserr, args);
      } else {
        throw new LaunchException("No default handler is defined");
      }
    } catch (Throwable t) {
      launchExceptionHandler.handle(logger, syserr, sysout, t);
    }
  }

  /**
   * Adds a new handler for the launcher for one argument.
   *
   * @param arg the argument.
   * @param handler the handler to execute if this argument is present.
   * @return the launcher itself.
   */
  public Launcher handle(String arg, LauncherHandler handler) {
    return handle(args(arg), handler);
  }

  /**
   * Adds a new handler for the launcher for a set of arguments.
   *
   * @param args the arguments.
   * @param handler the handler to execute if those arguments are present.
   * @return the launcher itself.
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
   * @param handler the default handler
   * @return the launcher itself.
   */
  public Launcher handle(LauncherHandler handler) {
    this.defaultHandler = handler;
    return this;
  }

  /**
   * Configures the logger provider to use for the launcher logger.
   *
   * @param provider the logger provider.
   * @return the launcher itself.
   */
  public Launcher loggerProvider(LoggerProvider provider) {
    this.loggerProvider = provider;
    return this;
  }

  /**
   * Configures the syserr output provided to the handler during execution.
   *
   * @param printStream the print stream to use as syserr.
   * @return the launcher itself.
   */
  public Launcher syserr(PrintStream printStream) {
    this.syserr = printStream;
    return this;
  }

  /**
   * Configures the sysout output provided to the handler during execution.
   *
   * @param printStream the print stream to use as sysout.
   * @return the launcher itself.
   */
  public Launcher sysout(PrintStream printStream) {
    this.sysout = printStream;
    return this;
  }

  /**
   * Configures the handler to manage exceptions arising during the initial handling. The default handler will print the
   * exception message to syserr and exit with an exit code of 1.
   *
   * @param launchExceptionHandler the new launch exception handler.
   * @return the launcher itself.
   */
  public Launcher launchExceptionHandler(LaunchExceptionHandler launchExceptionHandler) {
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
     * @param logger the logger to use for normal logging.
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
     * @param logger the logger to use for normal logging.
     * @param out The standard output stream.
     * @param err The standard error stream.
     * @param throwable the exception thrown.
     */
    void handle(Logger logger, PrintStream out, PrintStream err, Throwable throwable);
  }


}

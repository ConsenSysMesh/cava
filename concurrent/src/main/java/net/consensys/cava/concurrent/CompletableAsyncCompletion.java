package net.consensys.cava.concurrent;

/**
 * An {@link AsyncCompletion} that can later be completed successfully or with a provided exception.
 */
public interface CompletableAsyncCompletion extends AsyncCompletion {

  /**
   * Complete this completion.
   *
   * @return <tt>true</tt> if this invocation caused this completion to transition to a completed state, else
   *         <tt>false</tt>.
   */
  boolean complete();

  /**
   * Complete this completion with the given exception.
   *
   * @param ex The exception to complete this result with.
   * @return <tt>true</tt> if this invocation caused this completion to transition to a completed state, else
   *         <tt>false</tt>.
   */
  boolean completeExceptionally(Throwable ex);
}

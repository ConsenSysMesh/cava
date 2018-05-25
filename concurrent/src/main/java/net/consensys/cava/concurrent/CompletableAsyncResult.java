package net.consensys.cava.concurrent;

/**
 * A {@link AsyncResult} that can be later completed successfully with a provided value, or completed with an exception.
 *
 * @param <T> The type of the value returned by this result.
 */
public interface CompletableAsyncResult<T> extends AsyncResult<T> {

  /**
   * Complete this result with the given value.
   *
   * @param value The value to complete this result with.
   * @return <tt>true</tt> if this invocation caused this result to transition to a completed state, else
   *         <tt>false</tt>.
   */
  boolean complete(T value);

  /**
   * Complete this result with the given exception.
   *
   * @param ex The exception to complete this result with.
   * @return <tt>true</tt> if this invocation caused this result to transition to a completed state, else
   *         <tt>false</tt>.
   */
  boolean completeExceptionally(Throwable ex);
}

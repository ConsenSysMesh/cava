package net.consensys.cava.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

class AtomicSlotMapTest {

  @Test
  void shouldUseSlotsIncrementally() throws Exception {
    AtomicSlotMap<Integer, String> slotMap = AtomicSlotMap.positiveIntegerSlots();

    assertEquals(1, (int) slotMap.add("value"));
    assertEquals(2, (int) slotMap.add("value"));
    assertEquals(3, (int) slotMap.add("value"));
    assertEquals(4, (int) slotMap.add("value"));
  }

  @Test
  void shouldReuseSlotsIncrementally() throws Exception {
    AtomicSlotMap<Integer, String> slotMap = AtomicSlotMap.positiveIntegerSlots();

    assertEquals(1, (int) slotMap.add("value"));
    assertEquals(2, (int) slotMap.add("value"));
    assertEquals(3, (int) slotMap.add("value"));
    assertEquals(4, (int) slotMap.add("value"));
    slotMap.remove(2);
    slotMap.remove(4);
    assertEquals(2, (int) slotMap.add("value"));
    assertEquals(4, (int) slotMap.add("value"));
  }

  @Test
  void shouldNotDuplicateSlotsWhileAddingAndRemoving() throws Exception {
    AtomicSlotMap<Integer, String> slotMap = AtomicSlotMap.positiveIntegerSlots();
    Set<Integer> fastSlots = ConcurrentHashMap.newKeySet();
    Set<Integer> slowSlots = ConcurrentHashMap.newKeySet();

    Callable<Void> fastAdders = () -> {
      int slot = slotMap.add("a fast value");
      fastSlots.add(slot);
      return null;
    };

    Callable<Void> slowAdders = () -> {
      CompletableAsyncResult<String> result = AsyncResult.incomplete();
      slotMap.computeAsync(s -> result).thenAccept(slowSlots::add);

      Thread.sleep(10);
      result.complete("a slow value");
      return null;
    };

    Callable<Void> addAndRemovers = () -> {
      int slot = slotMap.add("a value");
      Thread.sleep(5);
      slotMap.remove(slot);
      return null;
    };

    ExecutorService fastPool = Executors.newFixedThreadPool(20);
    ExecutorService slowPool = Executors.newFixedThreadPool(20);
    ExecutorService addAndRemovePool = Executors.newFixedThreadPool(40);
    List<Future<Void>> fastFutures = fastPool.invokeAll(Collections.nCopies(1000, fastAdders));
    List<Future<Void>> slowFutures = slowPool.invokeAll(Collections.nCopies(1000, slowAdders));
    List<Future<Void>> addAndRemoveFutures = addAndRemovePool.invokeAll(Collections.nCopies(2000, addAndRemovers));

    for (Future<Void> future : addAndRemoveFutures) {
      future.get();
    }
    for (Future<Void> future : slowFutures) {
      future.get();
    }
    for (Future<Void> future : fastFutures) {
      future.get();
    }

    assertEquals(1000, fastSlots.size());
    assertEquals(1000, slowSlots.size());
    slowSlots.addAll(fastSlots);
    assertEquals(2000, slowSlots.size());

    assertEquals(2000, slotMap.size());
  }
}

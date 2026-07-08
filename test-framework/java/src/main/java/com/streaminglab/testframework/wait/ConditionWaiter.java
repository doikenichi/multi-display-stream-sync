package com.streaminglab.testframework.wait;

import java.time.Duration;
import java.time.Instant;
import java.util.function.BooleanSupplier;

/** Small reusable polling utility for API/browser conditions. */
public class ConditionWaiter {

  public boolean until(BooleanSupplier condition, Duration timeout, Duration pollInterval)
      throws InterruptedException {
    Instant deadline = Instant.now().plus(timeout);

    while (Instant.now().isBefore(deadline)) {
      if (condition.getAsBoolean()) {
        return true;
      }

      Thread.sleep(pollInterval.toMillis());
    }

    return false;
  }
}

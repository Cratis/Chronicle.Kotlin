```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reactor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestingReactorsPureFunction {

    @EventType
    record VibeCancelled(String host) {
    }

    record CreateNotification(String host) {
    }

    /** Returns the side effect as its result, so its logic is a pure function of the event. */
    @Reactor
    static class CancellationReactor {
        CreateNotification vibeCancelled(VibeCancelled event) {
            return new CreateNotification(event.host());
        }
    }

    @Test
    void aCancelledVibeRequestsANotificationForItsHost() {
        var reactor = new CancellationReactor();

        var command = reactor.vibeCancelled(new VibeCancelled("Ada"));

        assertEquals("Ada", command.host());
    }
}
```

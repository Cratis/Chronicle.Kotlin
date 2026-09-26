```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reactor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestingReactorsPureFunction {

    @EventType
    record VibeCancelled(String host) {
    }

    @EventType
    record NotificationRequested(String host) {
    }

    /** Returns an event that Chronicle appends; its logic is a pure function of the input event. */
    @Reactor
    static class CancellationReactor {
        NotificationRequested vibeCancelled(VibeCancelled event) {
            return new NotificationRequested(event.host());
        }
    }

    @Test
    void aCancelledVibeRequestsANotificationForItsHost() {
        var reactor = new CancellationReactor();

        var notification = reactor.vibeCancelled(new VibeCancelled("Ada"));

        assertEquals("Ada", notification.host());
    }
}
```

```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.eventSequences.AppendResult;
import io.cratis.chronicle.java.BlockingEventSequence;
import io.cratis.chronicle.testing.EventScenario;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TestingEventsScenarioAppendMany {

    @EventType
    record ItemAddedToCart(String itemId) {
    }

    @EventType
    record ItemQuantityAdjusted(String itemId, int quantity) {
    }

    @Test
    void appendManyAppendsABatchOfEventsInOneCall() {
        var scenario = new EventScenario("testing", "default");
        var eventLog = new BlockingEventSequence(scenario.getEventSequence());
        var cartId = "cart-1";

        List<AppendResult> result = eventLog.appendMany(cartId, List.of(
            new ItemAddedToCart("item-1"),
            new ItemAddedToCart("item-2"),
            new ItemQuantityAdjusted("item-1", 3)));

        assertTrue(result.stream().allMatch(AppendResult::isSuccess));
    }
}
```

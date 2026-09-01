```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.readModels.ReadModel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestingReadModelsScenarioInjectingDependencies {

    @EventType
    record OrderCreated(String orderId) {
    }

    @ReadModel
    record OrderSummary(String orderId, double total) {
    }

    interface PricingService {
        double basePrice();
    }

    @Reducer
    static class OrderSummaryReducer {
        private final PricingService pricingService;

        OrderSummaryReducer(PricingService pricingService) {
            this.pricingService = pricingService;
        }

        OrderSummary orderCreated(OrderCreated event) {
            return new OrderSummary(event.orderId(), pricingService.basePrice());
        }
    }

    @Test
    void aFakePassedIntoTheReducersConstructorIsUsedWhileFolding() {
        PricingService pricingService = () -> 42.0;
        var reducer = new OrderSummaryReducer(pricingService);

        var instance = reducer.orderCreated(new OrderCreated("order-1"));

        assertEquals(42.0, instance.total());
    }
}
```

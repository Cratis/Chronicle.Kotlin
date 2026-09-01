```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.events.EventType;

import io.cratis.chronicle.java.WebhookDefinitionBuilderJavaBridge;
import io.cratis.chronicle.java.WebhooksServiceJavaBridge;

@EventType
record WebhooksIndexOrderPlaced(String orderId) {}

class WebhooksIndexRegistration {
    void registerOrderWebhook(EventStore store) {
        WebhooksServiceJavaBridge.register(store.getWebhooks(), "order-placed-webhook", "https://hooks.example.com/orders", builder -> {
            WebhookDefinitionBuilderJavaBridge.withEventType(builder, WebhooksIndexOrderPlaced.class)
                .withBearerToken("webhook-token");
            return null; // Java lambda returning Unit
        });
    }
}
```

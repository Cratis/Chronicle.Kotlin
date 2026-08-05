```java
import Cratis.Chronicle.Contracts.Observation.Webhooks.ObservationWebhooks;

import io.cratis.chronicle.EventStore;

import io.cratis.chronicle.java.WebhooksServiceJavaBridge;

import java.util.List;

class WebhooksIndexListing {
    void listRegisteredWebhooks(EventStore store) {
        List<ObservationWebhooks.WebhookDefinition> webhooks = WebhooksServiceJavaBridge.getAll(store.getWebhooks());
        for (ObservationWebhooks.WebhookDefinition webhook : webhooks) {
            System.out.println(webhook.getIdentifier() + " -> " + webhook.getTarget().getUrl());
        }
    }
}
```

```kotlin
import io.cratis.chronicle.eventSequences.EventSequenceId

object SubscriptionsOutboxInboxId {
    fun resolve(): EventSequenceId {
        val inboxId = EventSequenceId("inbox-source-event-store")
        // Resolves to: "inbox-source-event-store"
        return inboxId
    }
}
```

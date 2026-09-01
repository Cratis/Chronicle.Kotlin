```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.eventSequences.EventSequenceId

suspend fun getInboxMessages(store: IEventStore) =
    store.projections.query(
        """
        projection InboxMessages
          from MessageReceived
        """,
        EventSequenceId("inbox")
    )
```

```kotlin
import io.cratis.chronicle.IEventStore

suspend fun previewNextSequenceNumber(store: IEventStore): Long {
    val next = store.eventLog.getNextSequenceNumber()
    println("The next appended event will be assigned sequence number ${next.value}")
    return next.value
}
```

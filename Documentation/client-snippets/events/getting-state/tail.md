```kotlin
import io.cratis.chronicle.IEventStore

suspend fun getAccountTailSequenceNumber(store: IEventStore, accountId: String): Long {
    val tail = store.eventLog.getTailSequenceNumber(accountId)
    println("Tail sequence number for $accountId: ${tail.value}")
    return tail.value
}
```

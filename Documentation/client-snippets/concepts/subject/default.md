```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.events.EventType

@EventType
data class SubjectAuthorRegistered(val name: String)

class SubjectAuthorService(private val eventStore: IEventStore) {
    suspend fun register(authorId: String, name: String) {
        // Subject defaults to authorId, so encryption keys for any PII on
        // SubjectAuthorRegistered are keyed by authorId.
        eventStore.eventLog.append(authorId, SubjectAuthorRegistered(name))
    }
}
```

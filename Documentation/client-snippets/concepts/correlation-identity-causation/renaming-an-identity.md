```kotlin
import io.cratis.chronicle.IEventStore

class CorrelationIdentityCausationRenamingAnIdentity {
    suspend fun rename(eventStore: IEventStore) {
        eventStore.identities.rename("subject-42", "Jane Austen")
    }
}
```

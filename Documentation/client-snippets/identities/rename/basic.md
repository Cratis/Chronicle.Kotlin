```kotlin
import io.cratis.chronicle.IEventStore

/** Renames the human-readable name the kernel has stored for an identity's subject. */
suspend fun renameIdentity(store: IEventStore, subject: String, newName: String) {
    store.identities.rename(subject, newName)
}
```

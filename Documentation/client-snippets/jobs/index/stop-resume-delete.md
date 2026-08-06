```kotlin
import io.cratis.chronicle.EventStore

suspend fun controlJob(store: EventStore, jobId: String) {
    store.jobs.stop(jobId)
    store.jobs.resume(jobId)
    store.jobs.delete(jobId)
}
```

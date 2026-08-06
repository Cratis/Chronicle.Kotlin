```kotlin
import io.cratis.chronicle.EventStore

suspend fun listJobs(store: EventStore) {
    store.jobs.getJobs().forEach { job ->
        println("${job.type}: ${job.status}")
    }
}
```

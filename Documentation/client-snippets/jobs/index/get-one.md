```kotlin
import io.cratis.chronicle.EventStore

suspend fun getReindexJob(store: EventStore, jobId: String) {
    val job = store.jobs.getJob(jobId)
    if (job != null) {
        println("${job.type}: ${job.status}")
    }
}
```

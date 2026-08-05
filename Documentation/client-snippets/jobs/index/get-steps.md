```kotlin
import io.cratis.chronicle.EventStore

suspend fun listJobSteps(store: EventStore, jobId: String) {
    store.jobs.getJobSteps(jobId).forEach { step ->
        println("${step.name}: ${step.status}")
    }
}
```

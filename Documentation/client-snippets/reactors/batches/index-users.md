```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.BatchContext
import io.cratis.chronicle.observation.EventBatch
import io.cratis.chronicle.observation.Observes
import io.cratis.chronicle.observation.Reactor
import java.time.Duration

@EventType
data class BatchUserRegistered(val name: String)

@EventType
data class BatchUserRenamed(val newName: String)

@EventType
data class BatchUserEmailChanged(val email: String)

data class BatchUserDocument(val id: String, val name: String)

data class BatchUpsertFailure(val eventSourceId: String, val error: Exception)

data class BatchUpsertThrottled(val eventSourceId: String, val retryAfter: Duration)

data class BatchUpsertResponse(val failures: List<BatchUpsertFailure>, val throttled: List<BatchUpsertThrottled>)

interface BatchSearchIndex {
    suspend fun bulkUpsert(documents: List<BatchUserDocument>): BatchUpsertResponse
}

interface BatchEmailConfirmations {
    suspend fun send(accountId: String, email: String)
}

@Reactor
class BatchUsers(private val index: BatchSearchIndex, private val confirmations: BatchEmailConfirmations) {
    @Observes(BatchUserRegistered::class)
    @Observes(BatchUserRenamed::class)
    suspend fun index(batch: EventBatch, context: BatchContext) {
        val documents = batch.map { e ->
            when (val event = e.event) {
                is BatchUserRegistered -> BatchUserDocument(e.context.eventSourceId, event.name)
                is BatchUserRenamed -> BatchUserDocument(e.context.eventSourceId, event.newName)
                else -> error("Unknown user event: $event")
            }
        }

        val response = index.bulkUpsert(documents)
        response.failures.forEach { batch.fail(it.eventSourceId, it.error) }
        response.throttled.forEach { batch.defer(it.eventSourceId, it.retryAfter) }
    }

    suspend fun emailChanged(event: BatchUserEmailChanged, context: EventContext) =
        confirmations.send(context.eventSourceId, event.email)
}
```

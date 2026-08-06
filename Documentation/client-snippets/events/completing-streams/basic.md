```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.eventSequences.CompleteStreamResult

/**
 * Completes a stream so no further events can be appended to it. The default stream can never
 * be completed, and completing an already-completed stream returns [CompleteStreamResult.AlreadyCompleted].
 */
suspend fun completeOrderStream(store: IEventStore, orderId: String) {
    when (val result = store.eventLog.completeStream("Order", orderId)) {
        is CompleteStreamResult.Success -> println("Completed at sequence ${result.sequenceNumber.value}")
        CompleteStreamResult.AlreadyCompleted -> println("Already completed")
        CompleteStreamResult.DefaultStreamCannotBeCompleted -> println("The default stream cannot be completed")
    }
}
```

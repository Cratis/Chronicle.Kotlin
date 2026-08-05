```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.events.EventType

@EventType
data class InspectingResultOrderPlaced(val orderId: String = "", val totalAmount: Double = 0.0)

/**
 * Commits a unit of work and inspects the outcome — violations, errors, and the last committed
 * sequence number are all available after [io.cratis.chronicle.transactions.IUnitOfWork.commit] returns.
 */
suspend fun commitAndInspect(store: IEventStore, orderId: String) {
    val unitOfWork = store.unitOfWorkManager.begin()
    store.eventLog.transactional.append(orderId, InspectingResultOrderPlaced(orderId, 42.0))
    unitOfWork.commit()

    if (unitOfWork.isSuccess) {
        println("Committed up to sequence ${unitOfWork.tryGetLastCommittedEventSequenceNumber()?.value}")
    } else {
        val violations = unitOfWork.getConstraintViolations().joinToString { it.message }
        val concurrencyViolations = unitOfWork.getConcurrencyViolations().joinToString { it.eventSourceId }
        val errors = unitOfWork.getAppendErrors().joinToString { it.message }
        println("Failed: violations=[$violations] concurrency=[$concurrencyViolations] errors=[$errors]")
    }
}
```

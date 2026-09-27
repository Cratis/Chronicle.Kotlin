```kotlin
import io.cratis.chronicle.IEventStore
import kotlin.reflect.KClass

suspend fun retryOneReactorPartition(store: IEventStore, reactor: KClass<*>) {
    val failedPartition = store.failedPartitions.getFor(reactor).firstOrNull() ?: return
    store.failedPartitions.retry(reactor, failedPartition.partition)
    // retry returns Unit, so a refused retry of a quarantined partition is not signalled here.
    // Inspect partition state before reporting recovery.
}
```

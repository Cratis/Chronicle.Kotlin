```kotlin
import io.cratis.chronicle.eventSequences.IEventLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Collects append operations for as long as [scope] is active, logging each appended event's
 * outcome.
 */
class AppendMonitor(eventLog: IEventLog, scope: CoroutineScope) {
    init {
        scope.launch {
            eventLog.appendOperations.collect { operations ->
                operations.forEach { println("Event ${it.event::class.simpleName} appended: success=${it.result.isSuccess}") }
            }
        }
    }
}
```

```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reactor

@EventType(id = "reactor-account-closed")
data class ReactorAccountClosed(val accountId: String)

@Reactor
class AuditReactor {
    fun accountClosed(event: ReactorAccountClosed, context: EventContext) {
        writeAudit(event.accountId, context.occurred, context.eventSourceId)
    }

    private fun writeAudit(accountId: String, occurred: java.time.Instant, eventSourceId: String) {}
}
```

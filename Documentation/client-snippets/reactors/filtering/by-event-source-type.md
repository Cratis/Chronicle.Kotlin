```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.IEventLog
import io.cratis.chronicle.observation.EventSourceType
import io.cratis.chronicle.observation.Reactor

@EventType
data class ReactorsFilteringCustomerRegistered(val emailAddress: String)

class ReactorsFilteringCustomerService(private val eventLog: IEventLog) {
    suspend fun register(eventSourceId: String, emailAddress: String) =
        eventLog.append(
            eventSourceId,
            ReactorsFilteringCustomerRegistered(emailAddress),
            AppendOptions(eventSourceType = "customer")
        )
}

@Reactor
@EventSourceType("customer")
class ReactorsFilteringCustomerWelcomeReactor {
    fun registered(event: ReactorsFilteringCustomerRegistered, context: EventContext) {
    }
}
```

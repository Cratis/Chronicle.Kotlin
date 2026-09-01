```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.readModels.ReadModel
import java.util.UUID

@EventType(id = "event-processing-account-opened")
data class EventProcessingAccountOpened(val accountId: UUID)

@EventType(id = "event-processing-deposit-made")
data class EventProcessingDepositMade(val amount: Double)

@EventType(id = "event-processing-account-closed")
class EventProcessingAccountClosed

@ReadModel
data class EventProcessingAccount(val accountId: UUID = UUID(0, 0), val balance: Double = 0.0, val isActive: Boolean = false)

@Reducer
class EventProcessingAccountReducer {
    fun opened(event: EventProcessingAccountOpened, current: EventProcessingAccount?, context: EventContext) =
        EventProcessingAccount(event.accountId, 0.0, true)

    fun depositMade(event: EventProcessingDepositMade, current: EventProcessingAccount?, context: EventContext): EventProcessingAccount? {
        // Skip if account doesn't exist or is not active
        if (current == null || !current.isActive) return current

        return current.copy(balance = current.balance + event.amount)
    }

    fun closed(event: EventProcessingAccountClosed, current: EventProcessingAccount?, context: EventContext): EventProcessingAccount? {
        if (current == null) return null

        return current.copy(isActive = false)
    }
}
```

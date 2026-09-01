```kotlin
import io.cratis.chronicle.events.EventType
import java.math.BigDecimal
import java.time.OffsetDateTime

data class DecSetPropsCustomer(val name: String, val email: String)

@EventType
data class DecSetPropsAccountOpened(
    val number: String,
    val owner: DecSetPropsCustomer,
    val timestamp: OffsetDateTime
)

@EventType
data class DecSetPropsMoneyDeposited(
    val amount: BigDecimal,
    val timestamp: OffsetDateTime
)
```

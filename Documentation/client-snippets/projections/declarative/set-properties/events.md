```kotlin
import io.cratis.chronicle.events.EventType
import java.math.BigDecimal
import java.time.OffsetDateTime

data class DecSetPropsCustomer(val name: String, val email: String)

@EventType(id = "dec-set-props-account-opened")
data class DecSetPropsAccountOpened(
    val number: String,
    val owner: DecSetPropsCustomer,
    val timestamp: OffsetDateTime
)

@EventType(id = "dec-set-props-money-deposited")
data class DecSetPropsMoneyDeposited(
    val amount: BigDecimal,
    val timestamp: OffsetDateTime
)
```

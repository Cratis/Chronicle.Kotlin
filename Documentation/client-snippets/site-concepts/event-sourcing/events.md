```kotlin
import io.cratis.chronicle.events.EventType
import java.math.BigDecimal

@EventType
data class AccountOpened(val owner: String, val initialBalance: BigDecimal)

@EventType
data class DepositMade(val amount: BigDecimal)

@EventType
data class WithdrawalMade(val amount: BigDecimal)
```

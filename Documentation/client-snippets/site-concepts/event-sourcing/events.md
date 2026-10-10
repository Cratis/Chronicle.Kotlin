```kotlin
import io.cratis.chronicle.events.EventType

@EventType
data class AccountOpened(val owner: String, val initialBalance: Double)

@EventType
data class DepositMade(val amount: Double)

@EventType
data class WithdrawalMade(val amount: Double)
```

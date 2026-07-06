```kotlin
import java.math.BigDecimal
import java.time.OffsetDateTime

data class DecSetPropsAccount(
    val accountNumber: String = "",
    val customerName: String = "",
    val balance: BigDecimal = BigDecimal.ZERO,
    val isActive: Boolean = false,
    val openedAt: OffsetDateTime? = null,
    val lastTransaction: OffsetDateTime? = null
)
```

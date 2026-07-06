```kotlin
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor
import java.math.BigDecimal

class DecSetPropsAccountProjection : IProjectionFor<DecSetPropsAccount> {
    override fun define(builder: IProjectionBuilderFor<DecSetPropsAccount>) {
        builder
            .from(DecSetPropsAccountOpened::class) {
                it.set(DecSetPropsAccount::accountNumber).to { e -> e.number }
                it.set(DecSetPropsAccount::customerName).to { e -> e.owner.name }
                it.set(DecSetPropsAccount::balance).to { BigDecimal("42.0") }
                it.set(DecSetPropsAccount::isActive).to { true }
                it.set(DecSetPropsAccount::openedAt).to { e -> e.timestamp }
            }
            .from(DecSetPropsMoneyDeposited::class) {
                it.set(DecSetPropsAccount::balance).to { e -> e.amount }
                it.set(DecSetPropsAccount::lastTransaction).to { e -> e.timestamp }
            }
    }
}
```

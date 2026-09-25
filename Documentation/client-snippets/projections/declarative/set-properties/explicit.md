```kotlin
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

class DecSetPropsAccountProjection : IProjectionFor<DecSetPropsAccount> {
    override fun define(builder: IProjectionBuilderFor<DecSetPropsAccount>) {
        builder
            .from(DecSetPropsAccountOpened::class) {
                it.set(DecSetPropsAccount::accountNumber).toProperty("number")
                it.set(DecSetPropsAccount::customerName).toProperty("owner.name")
                it.set(DecSetPropsAccount::isActive).toValue(true)
                it.set(DecSetPropsAccount::openedAt).toProperty("timestamp")
            }
            .from(DecSetPropsMoneyDeposited::class) {
                it.set(DecSetPropsAccount::balance).toProperty("amount")
                it.set(DecSetPropsAccount::lastTransaction).toProperty("timestamp")
            }
    }
}
```

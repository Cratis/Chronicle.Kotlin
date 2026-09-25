```kotlin
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

class DecSetPropsAccountProjection : IProjectionFor<DecSetPropsAccount> {
    override fun define(builder: IProjectionBuilderFor<DecSetPropsAccount>) {
        builder
            .from(DecSetPropsAccountOpened::class) {
                it.set(DecSetPropsAccount::accountNumber).toProperty("number")
                it.set(DecSetPropsAccount::customerName).toProperty("owner.name")
                // isActive is not set: the JVM fluent builder in 6.4.0 cannot set a constant value.
                it.set(DecSetPropsAccount::openedAt).toProperty("timestamp")
            }
            .from(DecSetPropsMoneyDeposited::class) {
                it.set(DecSetPropsAccount::balance).toProperty("amount")
                it.set(DecSetPropsAccount::lastTransaction).toProperty("timestamp")
            }
    }
}
```

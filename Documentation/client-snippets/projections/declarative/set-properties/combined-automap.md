```kotlin
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

class DecSetPropsCombinedAccountProjection : IProjectionFor<DecSetPropsAccount> {
    override fun define(builder: IProjectionBuilderFor<DecSetPropsAccount>) {
        builder
            // AutoMap is on by default; these two properties don't have matching event
            // properties, so they're set explicitly and everything else is left to AutoMap.
            .from(DecSetPropsAccountOpened::class) {
                it.set(DecSetPropsAccount::customerName).to { e -> e.owner.name }
                it.set(DecSetPropsAccount::isActive).to { true }
            }
            .from(DecSetPropsMoneyDeposited::class) // Uses AutoMap for all properties
    }
}
```

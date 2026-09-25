```kotlin
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

class DecSetPropsCombinedAccountProjection : IProjectionFor<DecSetPropsAccount> {
    override fun define(builder: IProjectionBuilderFor<DecSetPropsAccount>) {
        builder
            // AutoMap is on by default for matching event properties.
            .from(DecSetPropsAccountOpened::class) {
                // Map the property AutoMap cannot find: it is nested on the event.
                it.set(DecSetPropsAccount::customerName).toProperty("owner.name")
                it.set(DecSetPropsAccount::isActive).toValue(true)
            }
            .from(DecSetPropsMoneyDeposited::class) // Uses AutoMap for all properties
    }
}
```

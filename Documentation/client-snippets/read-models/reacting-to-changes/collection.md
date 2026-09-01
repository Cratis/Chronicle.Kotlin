```kotlin
import io.cratis.chronicle.readModels.IReadModelReactor
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
data class ReactingCollectionAccount(val name: String = "", val balance: Double = 0.0)

class AccountBatchProjector : IReadModelReactor {
    fun modified(accounts: List<ReactingCollectionAccount>) {
        accounts.forEach { sync(it) }
    }

    private fun sync(account: ReactingCollectionAccount) { /* ... */ }
}
```

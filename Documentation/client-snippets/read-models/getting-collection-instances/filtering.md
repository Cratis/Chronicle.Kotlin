```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
data class GettingCollectionFilteringAccount(val name: String = "", val balance: Double = 0.0)

/**
 * The read returns every instance; apply language-native filtering afterwards.
 */
suspend fun highValueAccounts(store: IEventStore, threshold: Double): List<GettingCollectionFilteringAccount> =
    store.readModels.getInstances(GettingCollectionFilteringAccount::class)
        .filter { it.balance > threshold }
        .sortedByDescending { it.balance }
```

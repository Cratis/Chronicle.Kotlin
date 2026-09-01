```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
data class GettingCollectionAccount(val name: String = "", val balance: Double = 0.0)

suspend fun printAllAccounts(store: IEventStore) {
    val accounts = store.readModels.getInstances(GettingCollectionAccount::class)
    accounts.forEach { println("${it.name}: ${it.balance}") }
}
```

```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.readModels.ReadModelReactors
import kotlinx.coroutines.Job
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

// JVM read model reactors are registered explicitly; cancel the job on shutdown.
fun startAccountBatchProjector(store: IEventStore): Job =
    ReadModelReactors(store.readModels, store.eventLog).register(AccountBatchProjector())
```

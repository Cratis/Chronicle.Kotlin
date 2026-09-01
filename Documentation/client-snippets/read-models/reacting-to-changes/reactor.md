```kotlin
import io.cratis.chronicle.readModels.IReadModelReactor
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
data class ReactingReactorAccount(val name: String = "", val balance: Double = 0.0)

class AccountNotifier : IReadModelReactor {
    fun added(account: ReactingReactorAccount) = sendWelcome(account)
    fun modified(account: ReactingReactorAccount) = sendUpdated(account)
    fun removed(account: ReactingReactorAccount?) = sendClosed(account)

    private fun sendWelcome(account: ReactingReactorAccount) { /* ... */ }
    private fun sendUpdated(account: ReactingReactorAccount) { /* ... */ }
    private fun sendClosed(account: ReactingReactorAccount?) { /* ... */ }
}
```

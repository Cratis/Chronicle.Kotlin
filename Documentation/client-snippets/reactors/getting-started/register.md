```kotlin
import io.cratis.chronicle.IEventStore
import kotlinx.coroutines.Job

class ReactorRegistration(private val emailGateway: ReactorEmailGateway) {
    suspend fun register(store: IEventStore): Job =
        store.reactors.register(OrderNotificationsReactor(emailGateway))
}
```

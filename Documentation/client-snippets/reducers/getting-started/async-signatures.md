```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.readModels.ReadModel
import kotlinx.coroutines.delay

@EventType(id = "reducers-async-signatures-opened")
data class ReducersAsyncSignaturesOpened(val orderId: String)

@EventType(id = "reducers-async-signatures-item-added")
data class ReducersAsyncSignaturesItemAdded(val sku: String, val amount: Double)

@ReadModel
data class ReducersAsyncSignaturesOrder(
    val orderId: String = "",
    val total: Double = 0.0,
    val currency: String = ""
)

class ReducersAsyncSignaturesRates {
    // Stands in for whatever you actually await - an HTTP client, a cache, a database.
    suspend fun currencyFor(sku: String): String {
        delay(1)
        return if (sku.startsWith("EU")) "EUR" else "USD"
    }
}

@Reducer
class ReducersAsyncSignaturesOrderReducer(
    private val rates: ReducersAsyncSignaturesRates = ReducersAsyncSignaturesRates()
) {
    // A handler that needs nothing awaited stays an ordinary function.
    fun opened(event: ReducersAsyncSignaturesOpened) =
        ReducersAsyncSignaturesOrder(orderId = event.orderId)

    // Mark a handler `suspend` and it is awaited rather than blocking the
    // thread the observation runs on. Every shape - (event), (event, current),
    // (event, current, context) - may suspend.
    suspend fun itemAdded(
        event: ReducersAsyncSignaturesItemAdded,
        current: ReducersAsyncSignaturesOrder?
    ) = current?.copy(
        total = current.total + event.amount,
        currency = rates.currencyFor(event.sku)
    )
}
```

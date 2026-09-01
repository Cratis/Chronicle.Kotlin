```kotlin
import io.cratis.chronicle.IEventStore

/**
 * Kotlin has no ASP.NET Core minimal-API route builder to show here - the handler is a plain
 * suspend function taking the values a web framework would already have extracted from the
 * request (a path variable and a query parameter), and appends the event directly. Spring MVC
 * handlers are synchronous, so bridge with runBlocking there; on WebFlux, or anywhere that
 * already supports suspend functions, call this directly instead.
 */
class AspNetCoreBookEndpoint(private val eventStore: IEventStore) {
    suspend fun borrow(bookId: String, memberName: String) =
        eventStore.eventLog.append(bookId, GetStartedBookBorrowed(memberName))
}
```

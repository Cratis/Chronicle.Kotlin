```kotlin
import io.cratis.chronicle.IEventStore
import kotlinx.coroutines.runBlocking
import java.util.UUID

/**
 * A Spring `CommandLineRunner` bean (`@Component class X(...) : CommandLineRunner`) is the plain
 * Spring Boot way to do work at startup in a host with no web layer. Reactors and projections are
 * already running in the background once the starter has registered them - the function below is
 * what that runner's `run` method calls to append the first event.
 */
fun seedFirstBook(eventStore: IEventStore) = runBlocking {
    val bookId = UUID.randomUUID().toString()
    eventStore.eventLog.append(bookId, GetStartedBookAdded("The Pragmatic Programmer", "978-0135957059"))
}
```

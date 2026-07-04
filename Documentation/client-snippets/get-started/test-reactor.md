```kotlin title="The reactor - does something when it happens"
import io.cratis.chronicle.observation.Reactor

@Reactor
class TestReactor {
    fun react(event: TestEvent) {
        println("Received event with message: ${event.message}")
    }
}
```

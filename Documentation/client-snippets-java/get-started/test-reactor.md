```java title="The reactor - does something when it happens"
import io.cratis.chronicle.observation.Reactor;

@Reactor
class TestReactor {
    void react(TestEvent event) {
        System.out.println("Received event with message: " + event.message());
    }
}
```

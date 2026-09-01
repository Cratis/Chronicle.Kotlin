```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.readModels.ReadModel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestingReadModelsScenarioBasic {

    @EventType
    record SomeEvent(String value) {
    }

    @ReadModel
    record MyReadModel(String value) {
    }

    @Reducer
    static class MyReducer {
        MyReadModel someEvent(SomeEvent event) {
            return new MyReadModel(event.value());
        }
    }

    @Test
    void foldingAnEventProducesTheReadModel() {
        var reducer = new MyReducer();

        var instance = reducer.someEvent(new SomeEvent("expected value"));

        assertEquals("expected value", instance.value());
    }
}
```

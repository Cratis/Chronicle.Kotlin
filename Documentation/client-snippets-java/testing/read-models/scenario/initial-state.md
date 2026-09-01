```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.readModels.ReadModel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * ReadModelScenario has no constructor parameter for a starting baseline, so a non-default initial
 * state is passed directly to the reducer's current-state parameter.
 */
class TestingReadModelsScenarioInitialState {

    @EventType
    record ItemAdded() {
    }

    @ReadModel
    record CountingReadModel(int count) {
    }

    @Reducer
    static class CountingReducer {
        CountingReadModel itemAdded(ItemAdded event, CountingReadModel current) {
            return new CountingReadModel(current.count() + 1);
        }
    }

    @Test
    void foldingFromANonDefaultBaselineContinuesFromIt() {
        var reducer = new CountingReducer();
        var initial = new CountingReadModel(10);

        var result = reducer.itemAdded(new ItemAdded(), initial);

        assertEquals(11, result.count());
    }
}
```

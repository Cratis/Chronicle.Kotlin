```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.readModels.ReadModel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestingExampleSpec {

    @EventType
    record AuthorRegistered(String name) {
    }

    @ReadModel
    record Author(String name) {
    }

    @Reducer
    static class AuthorReducer {
        Author registered(AuthorRegistered event) {
            return new Author(event.name());
        }
    }

    @Test
    void theAuthorReadModelCarriesTheRegisteredName() {
        var reducer = new AuthorReducer();

        var author = reducer.registered(new AuthorRegistered("Jane Austen"));

        assertEquals("Jane Austen", author.name());
    }
}
```

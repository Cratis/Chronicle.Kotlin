```java
import io.cratis.chronicle.compliance.Pii;
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.observation.Reducer;

record ChoosingStyleBookStatusReducerModel(
    String id,
    String title,
    String isbn,
    boolean isBorrowed,
    @Pii String borrowedBy) {}

@Reducer
class ChoosingStyleBookStatusReducer {
    public ChoosingStyleBookStatusReducerModel choosingStyleBookRegistered(
        ChoosingStyleBookRegistered event,
        ChoosingStyleBookStatusReducerModel current,
        EventContext context) {
        return new ChoosingStyleBookStatusReducerModel(
            context.getEventSourceId(), event.title(), event.isbn(), false, null);
    }

    public ChoosingStyleBookStatusReducerModel choosingStyleBookBorrowed(
        ChoosingStyleBookBorrowed event,
        ChoosingStyleBookStatusReducerModel current,
        EventContext context) {
        if (current == null) {
            current = new ChoosingStyleBookStatusReducerModel(
                context.getEventSourceId(), null, null, false, null);
        }
        return new ChoosingStyleBookStatusReducerModel(
            current.id(), current.title(), current.isbn(), true, event.memberName());
    }

    public ChoosingStyleBookStatusReducerModel choosingStyleBookReturned(
        ChoosingStyleBookReturned event,
        ChoosingStyleBookStatusReducerModel current,
        EventContext context) {
        if (current == null) {
            current = new ChoosingStyleBookStatusReducerModel(
                context.getEventSourceId(), null, null, false, null);
        }
        return new ChoosingStyleBookStatusReducerModel(
            current.id(), current.title(), current.isbn(), false, null);
    }
}
```

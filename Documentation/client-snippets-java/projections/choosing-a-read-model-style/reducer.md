```java
import io.cratis.chronicle.observation.Reducer;

record ChoosingStyleBookStatusReducerModel(
    String title,
    String isbn,
    boolean isBorrowed,
    String borrowedBy) {}

@Reducer
class ChoosingStyleBookStatusReducer {
    ChoosingStyleBookStatusReducerModel choosingStyleBookRegistered(ChoosingStyleBookRegistered event) {
        return new ChoosingStyleBookStatusReducerModel(event.title(), event.isbn(), false, null);
    }

    ChoosingStyleBookStatusReducerModel choosingStyleBookBorrowed(
        ChoosingStyleBookBorrowed event,
        ChoosingStyleBookStatusReducerModel current) {
        return new ChoosingStyleBookStatusReducerModel(
            current.title(), current.isbn(), true, event.memberName());
    }

    ChoosingStyleBookStatusReducerModel choosingStyleBookReturned(
        ChoosingStyleBookReturned event,
        ChoosingStyleBookStatusReducerModel current) {
        return new ChoosingStyleBookStatusReducerModel(
            current.title(), current.isbn(), false, null);
    }
}
```

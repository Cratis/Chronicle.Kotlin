```java
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

public class BookStatusProjection implements IProjectionFor<BookStatus> {
    @Override
    public void define(IProjectionBuilderFor<BookStatus> builder) {
        builder
            .from(BookRegistered.class, fb -> {
                fb.<String>set("id").toEventSourceId();
                fb.<Boolean>set("isBorrowed").toValue(false);
                fb.<String>set("borrowedBy").toValue(null);
            })
            .from(BookBorrowed.class, fb -> {
                fb.<Boolean>set("isBorrowed").toValue(true);
                fb.<String>set("borrowedBy").toProperty("memberName");
            })
            .from(BookReturned.class, fb -> {
                fb.<Boolean>set("isBorrowed").toValue(false);
                fb.<String>set("borrowedBy").toValue(null);
            });
    }
}
```

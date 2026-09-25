```java
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

class ChoosingStyleBookStatusProjection implements IProjectionFor<ChoosingStyleBookStatus> {
    @Override
    public void define(IProjectionBuilderFor<ChoosingStyleBookStatus> builder) {
        builder
            .from(ChoosingStyleBookRegistered.class, fb -> {
                fb.<String>set("id").toEventSourceId();
                fb.<String>set("title").toProperty("title");
                fb.<String>set("isbn").toProperty("isbn");
                fb.<Boolean>set("isBorrowed").toValue(false);
                fb.<String>set("borrowedBy").toValue(null);
            })
            .from(ChoosingStyleBookBorrowed.class, fb -> {
                fb.<Boolean>set("isBorrowed").toValue(true);
                fb.<String>set("borrowedBy").toProperty("memberName");
            })
            .from(ChoosingStyleBookReturned.class, fb -> {
                fb.<Boolean>set("isBorrowed").toValue(false);
                fb.<String>set("borrowedBy").toValue(null);
            });
    }
}
```

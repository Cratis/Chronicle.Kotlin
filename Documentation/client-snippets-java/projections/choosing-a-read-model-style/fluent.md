```java
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

class ChoosingStyleBookStatusFluent {
    public String id = "";
    public String title = "";
    public String isbn = "";
    public boolean isBorrowed = false;
    public String borrowedBy = null;
}

class ChoosingStyleBookStatusProjection implements IProjectionFor<ChoosingStyleBookStatusFluent> {
    @Override
    public void define(IProjectionBuilderFor<ChoosingStyleBookStatusFluent> builder) {
        builder
            .from(ChoosingStyleBookRegistered.class, fb -> {
                fb.set("id").toEventSourceId();
                fb.<String>set("title").to(e -> e.title());
                fb.<String>set("isbn").to(e -> e.isbn());
                fb.<Boolean>set("isBorrowed").to(e -> false);
                fb.<String>set("borrowedBy").to(e -> null);
                return null; // Java lambda returning Unit
            })
            .from(ChoosingStyleBookBorrowed.class, fb -> {
                fb.<Boolean>set("isBorrowed").to(e -> true);
                fb.<String>set("borrowedBy").to(e -> e.memberName());
                return null; // Java lambda returning Unit
            })
            .from(ChoosingStyleBookReturned.class, fb -> {
                fb.<Boolean>set("isBorrowed").to(e -> false);
                fb.<String>set("borrowedBy").to(e -> null);
                return null; // Java lambda returning Unit
            });
    }
}
```

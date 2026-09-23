```java
import io.cratis.chronicle.constraints.Constraint;
import io.cratis.chronicle.constraints.IConstraint;
import io.cratis.chronicle.constraints.IConstraintBuilder;
import io.cratis.chronicle.events.EventType;

@EventType
record ConstraintsUniqueEventTypeScopedLoanCheckedOut(String title) {
}

@Constraint
class ConstraintsUniqueEventTypeOneLoanPerBranch implements IConstraint {
    // One loan per borrower per branch. The stream the event is appended to decides which
    // branch it belongs to, so the same borrower can hold one loan at every branch.
    // The Java fluent builder has no removedWith, so this scoped constraint is never
    // released by a later event - each borrower/branch pair gets exactly one checkout.
    @Override
    public void define(IConstraintBuilder builder) {
        builder
            .perEventStreamId()
            .uniqueFor(ConstraintsUniqueEventTypeScopedLoanCheckedOut.class);
    }
}
```

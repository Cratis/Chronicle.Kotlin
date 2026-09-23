```kotlin
import io.cratis.chronicle.constraints.Constraint
import io.cratis.chronicle.constraints.IConstraint
import io.cratis.chronicle.constraints.IConstraintBuilder
import io.cratis.chronicle.events.EventType

@EventType
data class ConstraintsUniqueEventTypeScopedLoanCheckedOut(val title: String)

@Constraint
class ConstraintsUniqueEventTypeOneLoanPerBranch : IConstraint {
    // One loan per borrower per branch. The stream the event is appended to decides which
    // branch it belongs to, so the same borrower can hold one loan at every branch.
    // The Kotlin fluent builder has no removedWith, so this scoped constraint is never
    // released by a later event - each borrower/branch pair gets exactly one checkout.
    override fun define(builder: IConstraintBuilder) {
        builder
            .perEventStreamId()
            .uniqueFor(ConstraintsUniqueEventTypeScopedLoanCheckedOut::class)
    }
}
```

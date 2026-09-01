```kotlin title="Model-bound and declarative AutoMap"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class ConventionEquivalentUserRegistered(
    val name: String,
    val email: String
)

@ReadModel
@FromEvent(ConventionEquivalentUserRegistered::class)
data class ConventionEquivalentUser(
    val name: String = "",
    val email: String = ""
)

class ConventionEquivalentProjection : IProjectionFor<ConventionEquivalentUser> {
    override fun define(builder: IProjectionBuilderFor<ConventionEquivalentUser>) {
        builder.from(ConventionEquivalentUserRegistered::class)
    }
}
```

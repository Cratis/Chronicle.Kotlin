```kotlin title="AutoMap by convention"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

@EventType
data class AutoMapUserCreated(val name: String, val email: String)

@EventType
data class AutoMapUserRenamed(val name: String)

data class AutoMapUser(val name: String = "", val email: String = "")

class AutoMapUserProjection : IProjectionFor<AutoMapUser> {
    override fun define(builder: IProjectionBuilderFor<AutoMapUser>) {
        builder
            .from(AutoMapUserCreated::class)
            .from(AutoMapUserRenamed::class)
    }
}
```

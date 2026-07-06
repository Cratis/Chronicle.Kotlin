```kotlin title="The projection - builds queryable state"
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
@FromEvent(TestEvent::class)
data class TestProjection(
    val message: String = ""
)
```

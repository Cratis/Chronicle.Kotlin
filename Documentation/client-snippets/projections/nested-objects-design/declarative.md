```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

@EventType(id = "nod-declarative-slice-created")
data class NodDeclarativeSliceCreated(val name: String)

@EventType(id = "nod-declarative-command-set")
data class NodDeclarativeCommandSet(val name: String, val schema: String)

@EventType(id = "nod-declarative-command-cleared")
class NodDeclarativeCommandCleared

data class NodDeclarativeSlice(
    val name: String = "",
    val command: NodDeclarativeCommandItem? = null
)

data class NodDeclarativeCommandItem(
    val name: String = "",
    val schema: String = ""
)

class NodDeclarativeSliceProjection : IProjectionFor<NodDeclarativeSlice> {
    override fun define(builder: IProjectionBuilderFor<NodDeclarativeSlice>) {
        builder
            .from(NodDeclarativeSliceCreated::class)
            .nested(NodDeclarativeSlice::command, NodDeclarativeCommandItem::class) { nested ->
                nested
                    .from(NodDeclarativeCommandSet::class)
                    .clearWith(NodDeclarativeCommandCleared::class)
            }
    }
}
```

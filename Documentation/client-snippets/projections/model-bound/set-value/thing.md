```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.SetValue
import io.cratis.chronicle.readModels.ReadModel

@EventType(id = "mb-set-value-thing-happened")
data class MbSetValueThingHappened(val placeholder: Boolean = true)

@ReadModel
@FromEvent(MbSetValueThingHappened::class)
data class MbSetValueThing(
    @SetValue(MbSetValueThingHappened::class, value = "pending")
    val statusLabel: String = "",

    // Kotlin annotation parameters can only be compile-time constants of a fixed set of types, so
    // SetValue always carries its constant as a string - a numeric or boolean value is written out
    // as its literal text and interpreted against the property's declared type.
    @SetValue(MbSetValueThingHappened::class, value = "42")
    val priority: Int = 0,

    @SetValue(MbSetValueThingHappened::class, value = "true")
    val isActive: Boolean = false,

    @SetValue(MbSetValueThingHappened::class, value = "3.14")
    val score: Double = 0.0
)
```

```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.ChildrenFrom
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.RemovedWithJoin
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.readModels.ReadModel

@EventType(id = "mb-children-removed-feature-activated")
data class MbChildrenRemovedFeatureActivated(val featureId: String, val name: String)

@EventType(id = "mb-children-removed-feature-deactivated")
data class MbChildrenRemovedFeatureDeactivated(val featureId: String)

@ReadModel
@FromEvent(MbChildrenRemovedFeatureActivated::class)
data class MbChildrenRemovedSubscription(
    @ChildrenFrom(MbChildrenRemovedFeatureActivated::class, key = "featureId", identifiedBy = "featureId")
    @RemovedWithJoin(MbChildrenRemovedFeatureDeactivated::class, key = "featureId")
    val features: List<MbChildrenRemovedFeature> = emptyList()
)

data class MbChildrenRemovedFeature(
    val featureId: String = "",

    @SetFrom("name", MbChildrenRemovedFeatureActivated::class)
    val name: String = ""
)
```

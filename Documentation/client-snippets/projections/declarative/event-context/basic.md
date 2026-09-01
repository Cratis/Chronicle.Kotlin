```kotlin
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

class DecEventContextUserActivityProjection : IProjectionFor<DecEventContextUserActivity> {
    override fun define(builder: IProjectionBuilderFor<DecEventContextUserActivity>) {
        builder
            .from(DecEventContextUserLoggedIn::class) {
                it.set(DecEventContextUserActivity::userId).toEventSourceId()
                it.set(DecEventContextUserActivity::lastLogin).toEventContextProperty("occurred")
            }
            .from(DecEventContextUserPerformedAction::class) {
                it.set(DecEventContextUserActivity::userId).toEventSourceId()
                it.set(DecEventContextUserActivity::lastActivity).toEventContextProperty("occurred")
            }
    }
}
```

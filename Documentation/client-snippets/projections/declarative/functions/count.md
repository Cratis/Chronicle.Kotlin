```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

@EventType(id = "dec-functions-user-logged-in")
data class DecFunctionsUserLoggedIn(val username: String)

@EventType(id = "dec-functions-user-performed-action")
data class DecFunctionsUserPerformedAction(val username: String, val actionType: String)

data class DecFunctionsUserActivity(
    val username: String = "",
    val loginCount: Int = 0,
    val actionCount: Int = 0
)

class DecFunctionsUserActivityProjection : IProjectionFor<DecFunctionsUserActivity> {
    override fun define(builder: IProjectionBuilderFor<DecFunctionsUserActivity>) {
        builder
            .autoMap()
            .from(DecFunctionsUserLoggedIn::class) {
                it.count(DecFunctionsUserActivity::loginCount)
            }
            .from(DecFunctionsUserPerformedAction::class) {
                it.count(DecFunctionsUserActivity::actionCount)
            }
    }
}
```

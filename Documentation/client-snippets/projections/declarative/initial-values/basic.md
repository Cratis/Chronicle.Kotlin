```kotlin title="Initial values"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

enum class InitialValuesUserStatus {
    Inactive,
    Active
}

@EventType
data class InitialValuesUserCreated(val name: String, val email: String)

// Kotlin default parameter values are the read model's initial values — the kernel builds
// the starting instance by calling the primary constructor with none of its arguments supplied.
data class InitialValuesUserProfile(
    val name: String = "Unknown user",
    val email: String = "",
    val status: InitialValuesUserStatus = InitialValuesUserStatus.Inactive,
    val lastLogin: String? = null,
    val loginCount: Int = 0,
    val isVerified: Boolean = false
)

class InitialValuesUserProfileProjection : IProjectionFor<InitialValuesUserProfile> {
    override fun define(builder: IProjectionBuilderFor<InitialValuesUserProfile>) {
        builder.from(InitialValuesUserCreated::class)
    }
}
```

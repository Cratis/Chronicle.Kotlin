```kotlin
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

class DecSimpleUserProjection : IProjectionFor<DecSimpleUser> {
    override fun define(builder: IProjectionBuilderFor<DecSimpleUser>) {
        builder.from(DecSimpleUserCreated::class)
    }
}
```

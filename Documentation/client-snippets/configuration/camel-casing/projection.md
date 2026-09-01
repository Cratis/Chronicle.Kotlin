```kotlin
import io.cratis.chronicle.projections.IProjectionFor
import io.cratis.chronicle.projections.IProjectionBuilderFor

// Kotlin property names are already camelCase, so the projected read model properties below —
// firstName, lastName, emailAddress, registrationDate — need no naming policy configuration to
// come out as camelCase; that is simply how Kotlin properties are named.
class CamelCasingUserProjection : IProjectionFor<CamelCasingUserReadModel> {
    override fun define(builder: IProjectionBuilderFor<CamelCasingUserReadModel>) {
        builder.from(CamelCasingUserRegistered::class) {
            it.set(CamelCasingUserReadModel::firstName).to { e -> e.firstName }
            it.set(CamelCasingUserReadModel::lastName).to { e -> e.lastName }
            it.set(CamelCasingUserReadModel::emailAddress).to { e -> e.emailAddress }
            it.set(CamelCasingUserReadModel::registrationDate).to { e -> e.registrationDate }
        }
    }
}
```

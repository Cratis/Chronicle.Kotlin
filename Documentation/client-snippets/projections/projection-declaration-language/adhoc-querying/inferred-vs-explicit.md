```kotlin
import io.cratis.chronicle.IEventStore

suspend fun compareInferredAndExplicit(store: IEventStore) {
    // Inferred — schema derived from OrderPlaced and OrderShipped event properties
    val inferred = store.projections.query(
        """
        projection Orders
          from OrderPlaced
          from OrderShipped
        """
    )

    // Explicit — schema comes from the registered 'PdlOrderReadModel' type
    val explicitResult = store.projections.query(
        """
        projection Orders => PdlOrderReadModel
          from OrderPlaced
          from OrderShipped
        """
    )
}
```

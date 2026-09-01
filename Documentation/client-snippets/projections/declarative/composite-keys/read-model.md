```kotlin title="Read model addressed by a composite key"
data class CompositeOrder(
    val customerName: String = "",
    val orderDate: String = "",
    val shippedDate: String? = null
)
```

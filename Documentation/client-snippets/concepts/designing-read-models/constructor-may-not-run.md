```kotlin
data class DesigningReadModelsLineItem(val productName: String, val quantity: Int, val price: Double)

// A non-nullable property with a default in the constructor - not a body that "fixes up" a
// value after the fact, which a document deserializer may skip entirely.
data class DesigningReadModelsOrderSummary(
    val id: String,
    val lines: List<DesigningReadModelsLineItem> = emptyList()
)
```

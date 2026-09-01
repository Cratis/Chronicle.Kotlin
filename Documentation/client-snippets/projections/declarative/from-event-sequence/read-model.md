```kotlin
enum class DecFromEventSequenceOrderStatus {
    Created,
    Processing,
    Shipped,
    Delivered,
    Cancelled
}

data class DecFromEventSequenceOrder(
    val orderNumber: String = "",
    val customerId: String = "",
    val totalAmount: Double = 0.0,
    val status: DecFromEventSequenceOrderStatus = DecFromEventSequenceOrderStatus.Created,
    val shippedAt: String? = null
)
```

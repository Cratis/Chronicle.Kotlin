```kotlin
data class DecNotRewindableAuditLogEntry(
    val userId: String = "",
    val action: String = "",
    val details: String = "",
    val processedAt: String = "",
    val sequenceNumber: Long = 0
)
```

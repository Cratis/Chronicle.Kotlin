```kotlin
data class DecEventContextUserActivity(
    val userId: String = "",
    val lastLogin: String = "",
    val lastActivity: String = ""
)

data class DecEventContextAuditEntry(
    val eventId: Long = 0,
    val occurredAt: String = "",
    val correlationId: String = "",
    val actionType: String = "",
    val userId: String = ""
)
```

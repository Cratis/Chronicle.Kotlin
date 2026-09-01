```kotlin title="Read model with children"
data class GroupWithMembers(
    val name: String = "",
    val description: String = "",
    val members: List<GroupMember> = emptyList()
)

data class GroupMember(
    val userId: String = "",
    val role: String = ""
)
```

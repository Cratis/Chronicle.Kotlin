```java title="Read model with children"
import java.util.List;

record GroupWithMembers(String name, String description, List<GroupMember> members) {}

record GroupMember(String userId, String role) {}
```

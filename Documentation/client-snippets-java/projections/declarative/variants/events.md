```java
import io.cratis.chronicle.events.EventType;

@EventType
record DecVariantIssueCreated(String title) {}

@EventType
record DecVariantPullRequestCreated(String pullRequestUrl) {}
```

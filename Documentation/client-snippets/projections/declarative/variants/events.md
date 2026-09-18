```kotlin
import io.cratis.chronicle.events.EventType

@EventType
data class DecVariantIssueCreated(val title: String)

@EventType
data class DecVariantPullRequestCreated(val pullRequestUrl: String)
```

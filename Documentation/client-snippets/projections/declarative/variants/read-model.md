```kotlin
/**
 * Anchors the logical identity shared by DecVariantBacklogItem and DecVariantPullRequestItem.
 * Deliberately not a read model itself, and does not need a common supertype with either variant.
 */
class DecVariantWorkItem

data class DecVariantBacklogItem(val id: String = "", val title: String = "")

data class DecVariantPullRequestItem(val id: String = "", val pullRequestUrl: String = "")
```

```java
/**
 * Anchors the logical identity shared by DecVariantBacklogItem and DecVariantPullRequestItem.
 * Deliberately not a read model itself, and does not need a common supertype with either variant.
 */
class DecVariantWorkItem {}

class DecVariantBacklogItem {
    public String id = "";
    public String title = "";
}

class DecVariantPullRequestItem {
    public String id = "";
    public String pullRequestUrl = "";
}
```
